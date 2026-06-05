export function sleep(ms: number): Promise<void> {
  return new Promise((r) => setTimeout(r, ms))
}

export function escapeRegExp(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")
}

export interface Args {
  _: string[]
  flags: Record<string, string | boolean>
}

/**
 * Minimal argv parser. `booleans` lists flag names that take no value, so a
 * following positional (e.g. a directory) is not swallowed as their value.
 */
export function parseArgs(argv: string[], booleans: string[] = []): Args {
  const out: Args = { _: [], flags: {} }
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i]
    if (a.startsWith("--")) {
      const body = a.slice(2)
      const eq = body.indexOf("=")
      if (eq >= 0) {
        out.flags[body.slice(0, eq)] = body.slice(eq + 1)
      } else if (booleans.includes(body)) {
        out.flags[body] = true
      } else if (i + 1 < argv.length && !argv[i + 1].startsWith("--")) {
        out.flags[body] = argv[++i]
      } else {
        out.flags[body] = true
      }
    } else {
      out._.push(a)
    }
  }
  return out
}
