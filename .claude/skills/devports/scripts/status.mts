import { realpathSync } from "node:fs"
import { resolve } from "node:path"
import { parseArgs } from "./lib/util.mts"
import { loadRegistry, registryPath } from "./lib/registry.mts"

// Usage: node status.mts [--json]
const args = parseArgs(process.argv.slice(2), ["json"])
const here = (() => {
  try {
    return realpathSync(resolve(process.cwd()))
  } catch {
    return process.cwd()
  }
})()

const reg = loadRegistry()

if (args.flags.json) {
  console.log(JSON.stringify(reg, null, 2))
  process.exit(0)
}

console.log(`devports registry: ${registryPath()}`)
const dirs = Object.keys(reg.envs).sort()
if (dirs.length === 0) {
  console.log("  (empty)")
  process.exit(0)
}
for (const d of dirs) {
  const e = reg.envs[d]
  const mark = d === here ? "->" : "  "
  console.log(`${mark} ${d}`)
  console.log(`     project: ${e.projectName}   (updated ${e.updatedAt})`)
  console.log(`     ports:   ${Object.entries(e.ports).map(([k, v]) => `${k}=${v}`).join(", ")}`)
}
