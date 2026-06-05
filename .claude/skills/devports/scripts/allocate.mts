import { realpathSync } from "node:fs"
import { resolve, join } from "node:path"
import { parseArgs } from "./lib/util.mts"
import { deterministicOffset, findFree, isPortFree } from "./lib/ports.mts"
import { discoverConfigFiles, discoverHttpFiles, scanPortVars } from "./lib/scan.mts"
import { loadRegistry, saveRegistry, reservedByOthers, withLock } from "./lib/registry.mts"
import { projectName, renderBlock, upsertEnvBlock } from "./lib/env.mts"
import { writeHttpEnv, HTTP_PRIVATE_ENV } from "./lib/httpenv.mts"

// Usage: node allocate.mts [dir] [--span N] [--env-file .env] [--http-env dev] [--dry-run] [--json]
const args = parseArgs(process.argv.slice(2), ["dry-run", "json"])
const dir = resolve(String(args._[0] ?? process.cwd()))
const absDir = (() => {
  try {
    return realpathSync(dir)
  } catch {
    return dir
  }
})()
const span = Number(args.flags.span ?? 10000)
const envFile = join(absDir, String(args.flags["env-file"] ?? ".env"))
const httpEnvName = String(args.flags["http-env"] ?? "dev")
const dryRun = Boolean(args.flags["dry-run"])
const httpFiles = discoverHttpFiles(absDir)

const files = discoverConfigFiles(absDir)
const vars = scanPortVars(files)
if (vars.size === 0) {
  console.error(`devports: no \${NAME_PORT:-default} placeholders found under ${absDir}`)
  console.error(`Parameterize ports first — run suggest.mts and apply, or see SKILL.md "prepare".`)
  process.exit(2)
}

const result = await withLock(async () => {
  const reg = loadRegistry()
  const prev = reg.envs[absDir]?.ports ?? {}
  const othersReserved = reservedByOthers(reg, absDir)
  const offset = deterministicOffset(absDir, span)
  const ports: Record<string, number> = {}

  for (const [name, base] of [...vars].sort((a, b) => (a[0] < b[0] ? -1 : 1))) {
    const usedThisRun = new Set<number>(Object.values(ports))
    const hint = prev[name] && prev[name] <= 65535 ? prev[name] : base + offset
    const conflictsWithOthers = othersReserved.has(hint) || usedThisRun.has(hint)
    // Reuse our previous port even if currently bound (likely our own running
    // stack). For a fresh hint, require it to be actually free.
    const acceptable =
      !conflictsWithOthers && (hint === prev[name] || (await isPortFree(hint)))
    ports[name] = acceptable
      ? hint
      : await findFree(base + offset, new Set([...othersReserved, ...usedThisRun]))
  }

  const pname = reg.envs[absDir]?.projectName ?? projectName(absDir)
  const http = httpFiles.length ? { env: httpEnvName, keys: Object.keys(ports).sort() } : undefined
  reg.envs[absDir] = { updatedAt: new Date().toISOString(), projectName: pname, ports, ...(http ? { http } : {}) }
  if (!dryRun) saveRegistry(reg)
  return { ports, projectName: pname, http }
})

if (!dryRun) {
  upsertEnvBlock(envFile, renderBlock(result.projectName, result.ports))
  if (result.http) writeHttpEnv(absDir, result.http.env, result.ports)
}

if (args.flags.json) {
  console.log(JSON.stringify({ dir: absDir, envFile, ...result }, null, 2))
} else {
  console.log(`devports: ${dryRun ? "[dry-run] " : ""}allocated for ${absDir}`)
  console.log(`  COMPOSE_PROJECT_NAME=${result.projectName}`)
  for (const k of Object.keys(result.ports).sort()) console.log(`  ${k}=${result.ports[k]}`)
  if (!dryRun) console.log(`  -> written to ${envFile}`)
  if (result.http) {
    console.log(`  -> ${HTTP_PRIVATE_ENV} updated (env "${result.http.env}") for ${httpFiles.length} .http file(s)`)
  }
  console.log(`\nCompose auto-loads .env. A host process (not in compose) needs them exported, e.g.:`)
  console.log(`  set -a && . ${envFile} && set +a && ./mvnw spring-boot:run`)
}
