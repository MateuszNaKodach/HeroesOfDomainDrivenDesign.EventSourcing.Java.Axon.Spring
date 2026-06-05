import { readFileSync, readdirSync, existsSync } from "node:fs"
import { join } from "node:path"

// Matches both Compose (${VAR:-4318}) and Spring (${VAR:4318}) default syntax;
// the dash is optional and the default must be numeric (a port).
const VAR_RE = /\$\{([A-Z][A-Z0-9_]*):-?(\d+)\}/g

/**
 * Files a project draws port placeholders from: compose files in the project
 * root, plus Spring `application*.yaml` under src/main/resources when present.
 * Generic projects get the compose files; the Spring path is a no-op elsewhere.
 */
export function discoverConfigFiles(dir: string): string[] {
  const files: string[] = []
  try {
    for (const f of readdirSync(dir)) {
      if (/^(docker-)?compose.*\.ya?ml$/i.test(f)) files.push(join(dir, f))
    }
  } catch {
    /* unreadable dir */
  }
  const res = join(dir, "src", "main", "resources")
  if (existsSync(res)) {
    try {
      for (const f of readdirSync(res)) {
        if (/^application.*\.ya?ml$/i.test(f)) files.push(join(res, f))
      }
    } catch {
      /* ignore */
    }
  }
  return files
}

/**
 * Collect `${NAME:-default}` / `${NAME:default}` placeholders whose name passes
 * `filter` (default: ends in _PORT), mapping name -> base (default) value. The
 * first base seen for a name wins; later differing defaults are ignored.
 */
export function scanPortVars(files: string[], filter = /_PORT$/): Map<string, number> {
  const out = new Map<string, number>()
  for (const f of files) {
    let text: string
    try {
      text = readFileSync(f, "utf8")
    } catch {
      continue
    }
    for (const m of text.matchAll(VAR_RE)) {
      const name = m[1]
      if (!filter.test(name)) continue
      if (!out.has(name)) out.set(name, Number(m[2]))
    }
  }
  return out
}
