import { readFileSync } from "node:fs"
import { resolve } from "node:path"
import { parseArgs } from "./lib/util.mts"
import { discoverComposeFiles } from "./lib/scan.mts"

// Usage: node suggest.mts [dir] [--json]
// READ-ONLY. Scans compose files for static published ports and container_name
// lines, proposes ${SERVICE_ROLE_PORT:-default} variable names, and prints the
// edits to apply. It never writes — the agent (or you) applies the changes so
// nothing is silently rewritten. See SKILL.md "prepare".

interface PortFinding {
  file: string
  line: number
  service: string
  raw: string
  host: number
  container: number
  comment: string
  suggested: string
}

interface NameFinding {
  file: string
  line: number
  raw: string
}

const ROLE_HINTS: [RegExp, string][] = [
  [/otlp.*grpc/i, "OTLP_GRPC"],
  [/otlp.*http/i, "OTLP_HTTP"],
  [/grpc/i, "GRPC"],
  [/\bui\b/i, "UI"],
  [/http/i, "HTTP"],
]

function sanitize(name: string): string {
  return name.toUpperCase().replace(/[^A-Z0-9]+/g, "_").replace(/^_+|_+$/g, "")
}

function roleFor(comment: string, container: number, multi: boolean): string {
  for (const [re, role] of ROLE_HINTS) if (re.test(comment)) return role
  return multi ? String(container) : ""
}

function suggestName(service: string, container: number, comment: string, multi: boolean): string {
  const svc = sanitize(service)
  const role = roleFor(comment, container, multi)
  return role ? `${svc}_${role}_PORT` : `${svc}_PORT`
}

const PORT_ITEM =
  /^(\s*-\s*)(["']?)(?:(\d{1,3}(?:\.\d{1,3}){3}):)?(\d+):(\d+)((?:\/\w+)?)\2(\s*#.*)?$/

function analyze(file: string): { ports: PortFinding[]; names: NameFinding[] } {
  const ports: PortFinding[] = []
  const names: NameFinding[] = []
  let text: string
  try {
    text = readFileSync(file, "utf8")
  } catch {
    return { ports, names }
  }
  const lines = text.split("\n")

  // Track the current service by indentation: the first key under "services:"
  // establishes the service-key indent; keys at exactly that indent are services.
  let inServices = false
  let serviceIndent = -1
  let currentService = "service"
  const raw: Omit<PortFinding, "suggested">[] = []

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    if (/^\s*services:\s*$/.test(line)) {
      inServices = true
      serviceIndent = -1
      continue
    }
    if (inServices) {
      const key = line.match(/^(\s*)([A-Za-z0-9._-]+):\s*(#.*)?$/)
      if (key && key[2] !== "services") {
        const indent = key[1].length
        if (serviceIndent === -1) serviceIndent = indent
        if (indent === serviceIndent) currentService = key[2]
        // a top-level (indent 0) key ends the services block
        if (indent === 0) inServices = false
      }
    }
    if (/^\s*container_name:\s*\S+/.test(line)) {
      names.push({ file, line: i + 1, raw: line })
      continue
    }
    const m = line.match(PORT_ITEM)
    if (m && !line.includes("${")) {
      raw.push({
        file,
        line: i + 1,
        service: currentService,
        raw: line,
        host: Number(m[4]),
        container: Number(m[5]),
        comment: (m[7] ?? "").replace(/^\s*#\s*/, "").trim(),
      })
    }
  }

  // A service exposing more than one port needs role suffixes to disambiguate.
  const perService = new Map<string, number>()
  for (const r of raw) perService.set(r.service, (perService.get(r.service) ?? 0) + 1)
  for (const r of raw) {
    const multi = (perService.get(r.service) ?? 1) > 1
    ports.push({ ...r, suggested: suggestName(r.service, r.container, r.comment, multi) })
  }
  return { ports, names }
}

const args = parseArgs(process.argv.slice(2), ["json"])
const dir = resolve(String(args._[0] ?? process.cwd()))
const files = discoverComposeFiles(dir)
const all = files.map((f) => ({ file: f, ...analyze(f) }))

if (args.flags.json) {
  console.log(JSON.stringify(all, null, 2))
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
  for (const n of names) {
    console.log(`  L${n.line}  remove fixed name :  ${n.raw.trim()}`)
  }
  for (const p of ports) {
    total++
    const repl = `- "\${${p.suggested}:-${p.host}}:${p.container}"`
    console.log(`  L${p.line}  ${p.service}: ${p.raw.trim()}`)
    console.log(`         ->  ${repl}`)
  }
}
if (total === 0) console.log("\n(no static published ports found — already parameterized?)")
console.log(
  `\nApply these edits, then mirror any shared *_PORT used by the host app into its config\n` +
    `with Spring syntax ( \${VAR:default} — single colon ), e.g. server.port: \${APP_PORT:3773}.`,
)
