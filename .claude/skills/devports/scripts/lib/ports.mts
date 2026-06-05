import net from "node:net"
import { createHash } from "node:crypto"

/** Stable short hex digest of a seed (used for project-name suffixes). */
export function shortHash(seed: string, len = 6): string {
  return createHash("sha1").update(seed).digest("hex").slice(0, len)
}

/** Deterministic offset in [0, span) derived from a seed (e.g. an abs path). */
export function deterministicOffset(seed: string, span: number): number {
  const n = createHash("sha1").update(seed).digest().readUInt32BE(0)
  return n % span
}

/**
 * True if `port` can be bound on `host` right now. Binds a throwaway server on
 * 0.0.0.0 by default — the same interface Docker publishes to — so the check
 * matches what `compose up` will attempt.
 */
export function isPortFree(port: number, host = "0.0.0.0"): Promise<boolean> {
  return new Promise((resolve) => {
    const srv = net.createServer()
    srv.once("error", () => resolve(false))
    srv.once("listening", () => srv.close(() => resolve(true)))
    try {
      srv.listen(port, host)
    } catch {
      resolve(false)
    }
  })
}

/**
 * First port >= max(start, 1024) that is neither in `reserved` nor bound by any
 * process. Linear probe upward.
 */
export async function findFree(start: number, reserved: Set<number>, max = 65535): Promise<number> {
  for (let p = Math.max(start, 1024); p <= max; p++) {
    if (reserved.has(p)) continue
    if (await isPortFree(p)) return p
  }
  throw new Error(`No free port available at or above ${start}`)
}
