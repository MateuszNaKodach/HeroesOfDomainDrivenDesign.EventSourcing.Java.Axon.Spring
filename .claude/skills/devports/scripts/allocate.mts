import { realpathSync } from "node:fs"
import { resolve, join } from "node:path"
import { parseArgs } from "./lib/util.mts"
import { deterministicOffset, findFree, isPortFree } from "./lib/ports.mts"
import { discoverConfigFiles, discoverHttpFiles, scanPortVars } from "./lib/scan.mts"
import { loadRegistry, saveRegistry, reservedByOthers, withLock } from "./lib/registry.mts"
import { projectName, readEnvBlock, renderBlock, upsertEnvBlock } from "./lib/env.mts"
import { writeHttpEnv, HTTP_PRIVATE_ENV } from "./lib/httpenv.mts"

// Usage: node allocate.mts [dir] [--span N] [--env-file .env] [--http-env dev] [--reallocate] [--dry-run] [--json]
const args = parseArgs(process.argv.slice(2), ["dry-run", "json", "reallocate"])
const dir = resolve(String(args._[0] ?? process.cwd()))
const absDir = (() => {
  try {
    return realpathSync(dir)
  } catch {
    return dir
  }
})()
const span = Number(args.flags.span ?? 10000)
const envFile = join(absDir, String(args.flags["env-file"] ?? ".env"))
const httpEnvName = String(args.flags["http-env"] ?? "dev")
const dryRun = Boolean(args.flags["dry-run"])
const reallocate = Boolean(args.flags.reallocate)
const httpFiles = discoverHttpFiles(absDir)

const files = discoverConfigFiles(absDir)
const vars = scanPortVars(files)
if (vars.size === 0) {
  console.error(`devports: no \${NAME_PORT:-default} placeholders found under ${absDir}`)
  console.error(`Parameterize ports first - run suggest.mts and apply, or see SKILL.md "prepare".`)
  process.exit(2)
}

const result = await withLock(async () => {
  const reg = loadRegistry()
  const registered = reg.envs[absDir]
  // Ports already published for this directory. The registry entry is the first
  // source; failing that, an existing .env block (registry lost, or another
  // checkout's .env copied in). Adopting them keeps a running stack addressable
  // instead of moving ports out from under it; --reallocate opts out.
  // --reallocate discards both sources, so every port is picked fresh.
  const onDisk = reallocate ? null : readEnvBlock(envFile)
  const prev = reallocate ? {} : (registered?.ports ?? onDisk?.ports ?? {})
  const ownName = projectName(absDir)
  const othersReserved = reservedByOthers(reg, absDir)
  const offset = deterministicOffset(absDir, span)
  const ports: Record<string, number> = {}
  const shared: string[] = []

  for (const [name, base] of [...vars].sort((a, b) => (a[0] < b[0] ? -1 : 1))) {
    const usedThisRun = new Set<number>(Object.values(ports))
    const pinned = prev[name] !== undefined && prev[name] <= 65535 ? prev[name] : undefined
    if (pinned !== undefined && !usedThisRun.has(pinned)) {
      // Keep it even when currently bound (likely our own running stack) and even
      // when another entry reserves it (an adopted .env). A reservation clash is
      // reported below rather than silently resolved by moving the port.
      ports[name] = pinned
      if (othersReserved.has(pinned)) shared.push(name)
      continue
    }
    // Fresh hint: require it to be actually free and unreserved, else probe upward.
    const hint = base + offset
    const usable = !othersReserved.has(hint) && !usedThisRun.has(hint) && (await isPortFree(hint))
    ports[name] = usable ? hint : await findFree(base + offset, new Set([...othersReserved, ...usedThisRun]))
  }

  const pname = reallocate ? ownName : (registered?.projectName ?? onDisk?.projectName ?? ownName)
  const http = httpFiles.length ? { env: httpEnvName, keys: Object.keys(ports).sort() } : undefined
  reg.envs[absDir] = { updatedAt: new Date().toISOString(), projectName: pname, ports, ...(http ? { http } : {}) }
  if (!dryRun) saveRegistry(reg)

  // Directories whose reservations overlap the ports we just kept.
  const sharedPorts = new Set(shared.map((name) => ports[name]))
  const sharedWith = Object.entries(reg.envs)
    .filter(([d, e]) => d !== absDir && Object.values(e.ports).some((p) => sharedPorts.has(p)))
    .map(([d]) => d)
    .sort()

  return {
    ports,
    projectName: pname,
    http,
    adopted: !registered && onDisk ? Object.keys(onDisk.ports).sort() : [],
    foreignName: !registered && onDisk?.projectName && onDisk.projectName !== ownName ? ownName : undefined,
    shared,
    sharedWith,
  }
})

if (!dryRun) {
  upsertEnvBlock(envFile, renderBlock(result.projectName, result.ports))
  if (result.http) writeHttpEnv(absDir, result.http.env, result.ports)
}

if (args.flags.json) {
  console.log(JSON.stringify({ dir: absDir, envFile, ...result }, null, 2))
} else {
  console.log(`devports: ${dryRun ? "[dry-run] " : ""}allocated for ${absDir}`)
  console.log(`  COMPOSE_PROJECT_NAME=${result.projectName}`)
  for (const k of Object.keys(result.ports).sort()) console.log(`  ${k}=${result.ports[k]}`)
  if (!dryRun) console.log(`  -> written to ${envFile}`)
  if (!dryRun && result.http) {
    console.log(`  -> ${HTTP_PRIVATE_ENV} updated (env "${result.http.env}") for ${httpFiles.length} .http file(s)`)
  }
  if (result.adopted.length) {
    console.log(
      `\nAdopted ${result.adopted.length} port(s) from the existing ${envFile} block:\n` +
        `  this directory had no registry entry, so its published ports were kept rather than reassigned.`,
    )
  }
  if (result.foreignName) {
    console.log(
      `\n!! That block names COMPOSE_PROJECT_NAME=${result.projectName}, but this directory\n` +
        `   hashes to "${result.foreignName}" - the .env was written for a different checkout\n` +
        `   and copied in (Conductor "files to copy", a manual cp, ...).`,
    )
  }
  if (result.sharedWith.length) {
    console.log(
      `\n!! ${result.shared.length} port(s) are also reserved by:\n` +
        result.sharedWith.map((d) => `     ${d}`).join("\n") +
        `\n   Both checkouts cannot run at the same time. For isolated ports:\n` +
        `     docker compose down --remove-orphans && node allocate.mts --reallocate`,
    )
  }
  console.log(`\nCompose auto-loads .env. A host process (not in compose) needs them exported:`)
  console.log(`  set -a && . ${envFile} && set +a && <your app start command>`)
  console.log(`  (e.g. Spring: ./mvnw spring-boot:run)`)
}
