import { readFileSync } from "node:fs"

// Shared compose analysis used by suggest.mts (report) and prepare.mts (rewrite).
// Best-effort line parsing for the common compose shapes — enough to propose and
// apply `${SERVICE_ROLE_PORT:-default}` parameterization and drop container_name.

export interface PortFinding {
  file: string
  line: number // 1-based
  service: string
  raw: string
  host: number
  container: number
  comment: string
  suggested: string
}

export interface NameFinding {
  file: string
  line: number // 1-based
  raw: string
}

export interface ComposeAnalysis {
  ports: PortFinding[]
  names: NameFinding[]
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

// - "127.0.0.1:8080:80/tcp"  # comment   → host/container/comment captured; ipv4 + proto + quote optional.
const PORT_ITEM = /^(\s*-\s*)(["']?)(?:(\d{1,3}(?:\.\d{1,3}){3}):)?(\d+):(\d+)((?:\/\w+)?)\2(\s*#.*)?$/

/** Parse a compose file for static published ports and container_name lines. */
export function analyzeCompose(file: string): ComposeAnalysis {
  const ports: PortFinding[] = []
  const names: NameFinding[] = []
  let text: string
  try {
    text = readFileSync(file, "utf8")
  } catch {
    return { ports, names }
  }
  const lines = text.split("\n")

  // Track the current service by the indent of the first key under "services:".
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

  // A service exposing >1 port needs role suffixes to disambiguate.
  const perService = new Map<string, number>()
  for (const r of raw) perService.set(r.service, (perService.get(r.service) ?? 0) + 1)
  for (const r of raw) {
    const multi = (perService.get(r.service) ?? 1) > 1
    ports.push({ ...r, suggested: suggestName(r.service, r.container, r.comment, multi) })
  }
  return { ports, names }
}

/** The parameterized replacement line for a port finding (indent + comment preserved). */
export function replacementFor(p: PortFinding): string {
  const indent = p.raw.match(/^(\s*)/)?.[1] ?? "      "
  const comment = p.comment ? `   # ${p.comment}` : ""
  return `${indent}- "\${${p.suggested}:-${p.host}}:${p.container}"${comment}`
}

/**
 * Apply the parameterization to a compose file's text: rewrite static port lines
 * to `${VAR:-default}` and delete container_name lines. Line-accurate against the
 * same text the analysis was produced from. Returns new text + change counts.
 */
export function rewriteComposeText(
  text: string,
  analysis: ComposeAnalysis,
): { text: string; portsChanged: number; namesRemoved: number } {
  const replaceAt = new Map<number, string>()
  const deleteLines = new Set<number>()
  for (const p of analysis.ports) replaceAt.set(p.line - 1, replacementFor(p))
  for (const n of analysis.names) deleteLines.add(n.line - 1)

  const out: string[] = []
  text.split("\n").forEach((line, i) => {
    if (deleteLines.has(i)) return
    out.push(replaceAt.get(i) ?? line)
  })
  return { text: out.join("\n"), portsChanged: replaceAt.size, namesRemoved: deleteLines.size }
}
