import { readFileSync, readdirSync, statSync } from "node:fs"
import { join, basename } from "node:path"

// Matches both Compose (${VAR:-4318}) and Spring (${VAR:4318}) default syntax;
// the dash is optional and the default must be numeric (a port).
const VAR_RE = /\$\{([A-Z][A-Z0-9_]*):-?(\d+)\}/g

// Build/dependency/output dirs never worth scanning. Hidden dirs (".git",
// ".idea", ".claude", ".gradle", …) are skipped separately so the skill never
// scans its own example snippets or VCS internals.
const IGNORE_DIRS = new Set([
  "node_modules",
  "target",
  "build",
  "dist",
  "out",
  "bin",
  "vendor",
  "venv",
  "__pycache__",
  "coverage",
  "tmp",
])

// Generic config file extensions that may reference ports via env placeholders.
// No framework path is hardcoded — a Spring application.yaml / .properties is
// just one example that matches here because it lives in the tree.
const CONFIG_EXT = /\.(ya?ml|properties|toml|ini|conf|cfg|json|xml)$/i

const MAX_BYTES = 512 * 1024

export function isComposeFile(name: string): boolean {
  return /^(docker-)?compose.*\.ya?ml$/i.test(name)
}

/**
 * Recursively list files under `root`, skipping hidden dirs, build/vendor dirs,
 * and symlinks (avoids loops). Bounded by `maxDepth`. Returns sorted paths so
 * scan order — and therefore allocation — is deterministic.
 */
export function walkFiles(root: string, maxDepth = 10): string[] {
  const out: string[] = []
  const stack: Array<{ dir: string; depth: number }> = [{ dir: root, depth: 0 }]
  while (stack.length) {
    const { dir, depth } = stack.pop()!
    let entries
    try {
      entries = readdirSync(dir, { withFileTypes: true })
    } catch {
      continue
    }
    for (const e of entries) {
      if (e.isSymbolicLink()) continue
      if (e.isDirectory()) {
        if (e.name.startsWith(".") || IGNORE_DIRS.has(e.name)) continue
        if (depth < maxDepth) stack.push({ dir: join(dir, e.name), depth: depth + 1 })
      } else if (e.isFile()) {
        out.push(join(dir, e.name))
      }
    }
  }
  return out.sort()
}

/** All Docker Compose files anywhere under `dir` (recursive). */
export function discoverComposeFiles(dir: string): string[] {
  return walkFiles(dir).filter((f) => isComposeFile(basename(f)))
}

/**
 * Files that may declare port env-var placeholders: every compose file plus any
 * generic config file (yaml / properties / toml / json / …). Generated/local
 * `.env*` files are excluded (they hold values, not placeholders, and are the
 * skill's own output). Framework-agnostic: a Spring `application.yaml` or
 * `application.properties` is picked up the same as any other config file.
 */
export function discoverConfigFiles(dir: string): string[] {
  return walkFiles(dir).filter((f) => {
    const b = basename(f)
    if (b.startsWith(".env")) return false
    return isComposeFile(b) || CONFIG_EXT.test(b)
  })
}

/**
 * Collect `${NAME:-default}` / `${NAME:default}` placeholders whose name passes
 * `filter` (default: ends in _PORT), mapping name -> base (default) value. The
 * first base seen for a name wins; later differing defaults are ignored. Files
 * over 512 KB are skipped (lockfiles, fixtures) — config rarely gets that big.
 */
export function scanPortVars(files: string[], filter = /_PORT$/): Map<string, number> {
  const out = new Map<string, number>()
  for (const f of files) {
    try {
      if (statSync(f).size > MAX_BYTES) continue
    } catch {
      continue
    }
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
