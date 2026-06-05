import { realpathSync, existsSync } from "node:fs"
import { resolve, join } from "node:path"
import { parseArgs } from "./lib/util.mts"
import { loadRegistry, saveRegistry, withLock, type DirEntry } from "./lib/registry.mts"
import { removeEnvBlock } from "./lib/env.mts"
import { clearHttpEnv } from "./lib/httpenv.mts"

// Usage: node release.mts [dir] [--prune] [--keep-env] [--env-file .env] [--json]
//   --prune     also drop registry entries whose directory no longer exists
//   --keep-env  do not touch the .env file (only free the reservation)
const args = parseArgs(process.argv.slice(2), ["prune", "keep-env", "json"])
const dir = resolve(String(args._[0] ?? process.cwd()))
const absDir = (() => {
  try {
    return realpathSync(dir)
  } catch {
    return dir
  }
})()
const envFile = join(absDir, String(args.flags["env-file"] ?? ".env"))

const freed = await withLock(async () => {
  const reg = loadRegistry()
  const removed: Record<string, DirEntry> = {}
  if (args.flags.prune) {
    for (const d of Object.keys(reg.envs)) {
      if (!existsSync(d)) {
        removed[d] = reg.envs[d]
        delete reg.envs[d]
      }
    }
  }
  if (reg.envs[absDir]) {
    removed[absDir] = reg.envs[absDir]
    delete reg.envs[absDir]
  }
  saveRegistry(reg)
  return removed
})

let envRemoved = false
let httpCleared = false
if (!args.flags["keep-env"] && freed[absDir]) {
  envRemoved = removeEnvBlock(envFile)
  const http = freed[absDir].http
  if (http) httpCleared = clearHttpEnv(absDir, http.env, http.keys)
}

if (args.flags.json) {
  console.log(JSON.stringify({ freed, envRemoved, httpCleared }, null, 2))
} else {
  const dirs = Object.keys(freed)
  if (dirs.length === 0) {
    console.log(`devports: nothing reserved for ${absDir} — nothing to release`)
  } else {
    for (const d of dirs) {
      const tag = d === absDir ? "" : "  (pruned — dir gone)"
      console.log(`devports: released ${d}${tag}`)
      console.log(`  freed: ${Object.entries(freed[d].ports).map(([k, v]) => `${k}=${v}`).join(", ")}`)
    }
    if (envRemoved) console.log(`  removed managed block from ${envFile}`)
    if (httpCleared) console.log(`  cleared devports keys from http-client.private.env.json`)
  }
}
