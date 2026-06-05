import { readFileSync, writeFileSync, existsSync, rmSync } from "node:fs"
import { join } from "node:path"

// JetBrains HTTP Client reads this gitignored file for private/local variable
// values; values here override the committed http-client.env.json for the same
// environment. devports owns only the keys it writes (tracked in the registry),
// merging into any user-authored environments/keys without clobbering them.
export const HTTP_PRIVATE_ENV = "http-client.private.env.json"

type EnvJson = Record<string, Record<string, string>>

function read(path: string): EnvJson {
  if (!existsSync(path)) return {}
  try {
    const j = JSON.parse(readFileSync(path, "utf8"))
    return j && typeof j === "object" ? (j as EnvJson) : {}
  } catch {
    return {}
  }
}

/**
 * Merge allocated ports into `envName` of the project's private HTTP env file,
 * as string values (HTTP Client variables are strings). Returns the keys
 * written so `release` can remove exactly those later.
 */
export function writeHttpEnv(projectDir: string, envName: string, ports: Record<string, number>): string[] {
  const path = join(projectDir, HTTP_PRIVATE_ENV)
  const json = read(path)
  const env = (json[envName] ??= {})
  const keys = Object.keys(ports).sort()
  for (const k of keys) env[k] = String(ports[k])
  writeFileSync(path, JSON.stringify(json, null, 2) + "\n")
  return keys
}

/**
 * Remove only the devports-managed keys from `envName`; drop the environment if
 * it becomes empty, and delete the file if nothing else remains. User-authored
 * environments and keys are preserved. Returns true if the file was touched.
 */
export function clearHttpEnv(projectDir: string, envName: string, keys: string[]): boolean {
  const path = join(projectDir, HTTP_PRIVATE_ENV)
  if (!existsSync(path)) return false
  const json = read(path)
  const env = json[envName]
  if (!env) return false
  for (const k of keys) delete env[k]
  if (Object.keys(env).length === 0) delete json[envName]
  if (Object.keys(json).length === 0) rmSync(path)
  else writeFileSync(path, JSON.stringify(json, null, 2) + "\n")
  return true
}
