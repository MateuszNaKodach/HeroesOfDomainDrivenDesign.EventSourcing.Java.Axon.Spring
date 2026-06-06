import { readFileSync, writeFileSync } from "node:fs"
import { resolve } from "node:path"
import { parseArgs } from "./lib/util.mts"
import { discoverComposeFiles } from "./lib/scan.mts"
import { analyzeCompose, replacementFor, rewriteComposeText } from "./lib/compose.mts"

// Usage: node prepare.mts [dir] [--write] [--json]
//   (default)  dry-run: print the planned compose edits, change nothing.
//   --write    apply the edits to COMPOSE files (parameterize static ports,
//              remove container_name). Idempotent — re-running is a no-op.
//
// Scope note: prepare only rewrites compose files (mechanical + safe). Host app
// config (e.g. Spring application.yaml) and .http files need judgement about
// WHICH ports the app uses and the correct placeholder syntax, so they are
// reported for review, not auto-edited. See reference/decisions.md (#6, #8).

const args = parseArgs(process.argv.slice(2), ["write", "json"])
const dir = resolve(String(args._[0] ?? process.cwd()))
const write = Boolean(args.flags.write)

const plans = discoverComposeFiles(dir)
  .map((f) => ({ file: f, ...analyzeCompose(f) }))
  .filter((p) => p.ports.length > 0 || p.names.length > 0)

if (args.flags.json) {
  console.log(JSON.stringify({ write, plans }, null, 2))
  process.exit(0)
}

if (plans.length === 0) {
  console.log(`devports prepare: nothing to do — compose files already parameterized in ${dir}`)
  process.exit(0)
}

let ports = 0
let names = 0
for (const plan of plans) {
  console.log(`\n# ${plan.file}`)
  for (const n of plan.names) console.log(`  - remove   ${n.raw.trim()}`)
  for (const p of plan.ports) {
    console.log(`  - L${p.line}   ${p.raw.trim()}`)
    console.log(`         ->   ${replacementFor(p).trim()}`)
  }
  if (write) {
    const res = rewriteComposeText(readFileSync(plan.file, "utf8"), plan)
    writeFileSync(plan.file, res.text)
    ports += res.portsChanged
    names += res.namesRemoved
  } else {
    ports += plan.ports.length
    names += plan.names.length
  }
}

if (write) {
  console.log(`\ndevports: wrote ${ports} port mapping(s), removed ${names} container_name line(s) across ${plans.length} file(s).`)
} else {
  console.log(`\n[dry-run] would change ${ports} port mapping(s) + ${names} container_name line(s). Re-run with --write to apply.`)
}

console.log(
  `\nNot auto-applied — review and edit by hand:\n` +
    `  • Host app config (e.g. Spring application.yaml/.properties): mirror shared\n` +
    `    *_PORT with Spring syntax \${VAR:default} (single colon).\n` +
    `  • .http files: use {{APP_PORT}} (drop any @serverPort); commit an\n` +
    `    http-client.env.json default.\n` +
    `Verify:  docker compose config  (shows original ports)  &&  node suggest.mts --check  (exits 0)`,
)
