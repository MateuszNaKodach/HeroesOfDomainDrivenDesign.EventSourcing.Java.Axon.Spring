import { resolve } from "node:path"
import { parseArgs } from "./lib/util.mts"
import { discoverComposeFiles } from "./lib/scan.mts"
import { analyzeCompose, replacementFor } from "./lib/compose.mts"

// Usage: node suggest.mts [dir] [--check] [--json]
//   --check  exit 1 if the project NEEDS PREPARE (static ports / container_name
//            lines present), exit 0 if already parameterized. The gate for the
//            skill's prepare-vs-isolate decision.
// READ-ONLY. Proposes ${SERVICE_ROLE_PORT:-default} names and the container_name
// lines to drop. Apply them with `prepare.mts --write` (compose only) or by hand.

const args = parseArgs(process.argv.slice(2), ["json", "check"])
const dir = resolve(String(args._[0] ?? process.cwd()))
const files = discoverComposeFiles(dir)
const all = files.map((f) => ({ file: f, ...analyzeCompose(f) }))
const staticPorts = all.reduce((n, a) => n + a.ports.length, 0)
const fixedNames = all.reduce((n, a) => n + a.names.length, 0)
const needsPrepare = staticPorts > 0 || fixedNames > 0

if (args.flags.json) {
  console.log(JSON.stringify({ needsPrepare, staticPorts, fixedNames, files: all }, null, 2))
  process.exit(0)
}

if (args.flags.check) {
  // Gate for the skill's prepare/isolate decision:
  //   exit 1 = project NEEDS PREPARE, exit 0 = already prepared.
  if (needsPrepare) {
    console.log(`devports: NEEDS PREPARE — ${staticPorts} static port(s), ${fixedNames} fixed container_name(s)`)
    process.exit(1)
  }
  console.log("devports: already prepared — no static ports or container_name lines found")
  process.exit(0)
}

if (files.length === 0) {
  console.log(`devports suggest: no compose files found in ${dir}`)
  process.exit(0)
}

let total = 0
for (const { file, ports, names } of all) {
  if (ports.length === 0 && names.length === 0) continue
  console.log(`\n# ${file}`)
  for (const n of names) console.log(`  L${n.line}  remove fixed name :  ${n.raw.trim()}`)
  for (const p of ports) {
    total++
    console.log(`  L${p.line}  ${p.service}: ${p.raw.trim()}`)
    console.log(`         ->  ${replacementFor(p).trim()}`)
  }
}
if (total === 0) console.log("\n(no static published ports found — already parameterized?)")
console.log(
  `\nApply (compose only):  node prepare.mts --write\n` +
    `Then by hand: mirror shared *_PORT into host app config with Spring syntax\n` +
    `\${VAR:default} (single colon), and point .http files at {{APP_PORT}}.`,
)
