import { readFileSync, writeFileSync, existsSync, rmSync } from "node:fs"
import { basename } from "node:path"
import { shortHash } from "./ports.mts"
import { escapeRegExp } from "./util.mts"

const BEGIN = "# >>> devports (managed — do not edit by hand) >>>"
const END = "# <<< devports <<<"

/**
 * Compose-safe project name for a directory: sanitized basename + stable hash
 * suffix. Compose project names must match [a-z0-9][a-z0-9_-]*, so dots and
 * uppercase (e.g. "Heroes.Axon4to5.DCB") are normalized away.
 */
export function projectName(absDir: string): string {
  const base =
    basename(absDir)
      .toLowerCase()
      .replace(/[^a-z0-9_-]+/g, "-")
      .replace(/^[-_]+/, "") || "project"
  return `${base}-${shortHash(absDir)}`
}

export function renderBlock(projectName: string, ports: Record<string, number>): string {
  const lines = [BEGIN, `COMPOSE_PROJECT_NAME=${projectName}`]
  for (const k of Object.keys(ports).sort()) lines.push(`${k}=${ports[k]}`)
  lines.push(END)
  return lines.join("\n")
}

function blockRe(): RegExp {
  return new RegExp(`\\n?${escapeRegExp(BEGIN)}[\\s\\S]*?${escapeRegExp(END)}\\n?`)
}

/** Insert or replace the managed block in `path`, preserving other content. */
export function upsertEnvBlock(path: string, block: string): void {
  let text = existsSync(path) ? readFileSync(path, "utf8") : ""
  const re = blockRe()
  if (re.test(text)) {
    text = text.replace(re, "\n" + block + "\n")
  } else {
    if (text.length && !text.endsWith("\n")) text += "\n"
    text += block + "\n"
  }
  writeFileSync(path, text.replace(/^\n+/, ""))
}

/**
 * Parse the managed block of `path` back into the values it records. Returns
 * null when the file or the block is absent.
 *
 * Lets `allocate` adopt ports that are already published for a directory instead
 * of silently handing out different ones. Two cases produce a block with no
 * matching registry entry: the registry was lost or reset, and a tool copying a
 * sibling worktree's gitignored files (Conductor's "files to copy", a manual
 * `cp`) dropped another checkout's `.env` in.
 */
export function readEnvBlock(path: string): { projectName?: string; ports: Record<string, number> } | null {
  if (!existsSync(path)) return null
  const match = blockRe().exec(readFileSync(path, "utf8"))
  if (!match) return null
  const ports: Record<string, number> = {}
  let projectName: string | undefined
  for (const line of match[0].split("\n")) {
    const kv = /^([A-Za-z_][A-Za-z0-9_]*)=(.*)$/.exec(line.trim())
    if (!kv) continue
    const [, key, rawValue] = kv
    const value = rawValue.trim()
    if (key === "COMPOSE_PROJECT_NAME") {
      if (value) projectName = value
      continue
    }
    const port = Number(value)
    if (Number.isInteger(port) && port > 0 && port <= 65535) ports[key] = port
  }
  return { projectName, ports }
}

/**
 * Strip the managed block. Returns true if something was removed. Deletes the
 * file entirely if nothing but the block remained.
 */
export function removeEnvBlock(path: string): boolean {
  if (!existsSync(path)) return false
  const text = readFileSync(path, "utf8")
  const re = blockRe()
  if (!re.test(text)) return false
  const out = text.replace(re, "\n").replace(/\n{3,}/g, "\n\n").replace(/^\n+/, "")
  if (out.trim() === "") rmSync(path)
  else writeFileSync(path, out)
  return true
}
