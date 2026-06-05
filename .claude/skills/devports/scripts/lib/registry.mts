import { readFileSync, writeFileSync, mkdirSync, existsSync, rmSync } from "node:fs"
import { join, dirname } from "node:path"
import os from "node:os"
import { sleep } from "./util.mts"

export interface DirEntry {
  updatedAt: string
  projectName: string
  ports: Record<string, number>
  /** devports-managed HTTP Client private-env overrides, when .http files exist. */
  http?: { env: string; keys: string[] }
}

export interface Registry {
  version: number
  envs: Record<string, DirEntry>
}

/**
 * Global reservation registry path. Honors $DEVPORTS_REGISTRY, else
 * $XDG_CONFIG_HOME/devports/registry.json, else ~/.config/devports/registry.json.
 */
export function registryPath(): string {
  if (process.env.DEVPORTS_REGISTRY) return process.env.DEVPORTS_REGISTRY
  const cfg = process.env.XDG_CONFIG_HOME ?? join(os.homedir(), ".config")
  return join(cfg, "devports", "registry.json")
}

export function loadRegistry(): Registry {
  const p = registryPath()
  if (!existsSync(p)) return { version: 1, envs: {} }
  try {
    const r = JSON.parse(readFileSync(p, "utf8")) as Registry
    return r && r.envs ? r : { version: 1, envs: {} }
  } catch {
    return { version: 1, envs: {} }
  }
}

export function saveRegistry(reg: Registry): void {
  const p = registryPath()
  mkdirSync(dirname(p), { recursive: true })
  writeFileSync(p, JSON.stringify(reg, null, 2) + "\n")
}

/** Ports reserved by every directory except `selfDir`. */
export function reservedByOthers(reg: Registry, selfDir: string): Set<number> {
  const set = new Set<number>()
  for (const [dir, e] of Object.entries(reg.envs)) {
    if (dir === selfDir) continue
    for (const port of Object.values(e.ports)) set.add(port)
  }
  return set
}

/**
 * Cross-process critical section around the registry, using an atomic mkdir
 * lock so parallel worktree allocations never race on read-modify-write.
 */
export async function withLock<T>(fn: () => Promise<T>): Promise<T> {
  const lock = registryPath() + ".lock"
  mkdirSync(dirname(lock), { recursive: true })
  for (let attempt = 0; attempt < 200; attempt++) {
    try {
      mkdirSync(lock) // atomic; throws EEXIST if held
    } catch {
      await sleep(50)
      continue
    }
    try {
      return await fn()
    } finally {
      rmSync(lock, { recursive: true, force: true })
    }
  }
  throw new Error(`Could not acquire registry lock at ${lock} (stale lock? remove it manually)`)
}
