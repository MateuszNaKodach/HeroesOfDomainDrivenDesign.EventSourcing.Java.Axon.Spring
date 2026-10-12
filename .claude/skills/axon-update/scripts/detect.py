#!/usr/bin/env python3
"""Detect the Axon Framework 5 / Axoniq Framework 5 setup of a project.

Run from the project root (or pass --root). Standard library only.

Prints:
  1. build tool and build files
  2. Axon/Axoniq coordinates with declared versions and the properties that drive them
  3. latest versions on Maven Central for those coordinates (skip with --offline)
  4. storage/processor related configuration per Spring profile file
  5. engine, token store, snapshot store and dead-letter classes wired in code
  6. which profiles the tests activate
"""
import argparse
import json
import os
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET

AXON_GROUP = re.compile(r"^(org\.axonframework|io\.axoniq)")
SKIP_DIRS = {".git", "target", "build", "node_modules", ".gradle", ".idea", "out"}

CONFIG_KEY = re.compile(
    r"(^axon\.|^axoniq\.|eventstore|event-store|event_store|storage|dcb|aggregate-based|"
    r"token|snapshot|dlq|dead-letter|processors?\.|datasource\.url|profiles\.(active|group|include))",
    re.I,
)

STORAGE_SWITCH = re.compile(r"(axonserver\.enabled|axonserver\.context|eventstore|event-store|storage|dcb|"
                            r"datasource\.url|token-?store)", re.I)

CODE_PATTERNS = {
    "event storage engine": r"\b(\w*EventStorageEngine)\b",
    "event store / connection": r"\b(AxonServerConnectionManager|AxonServerConfiguration|\w*EventStore\b)",
    "token store": r"\b(\w*TokenStore)\b",
    "snapshot store / snapshotting": r"\b(\w*SnapshotStore|@Snapshotting)\b",
    "dead letter queue": r"\b(\w*DeadLetter\w*|\w*SequencedDeadLetterQueue)\b",
    "processor type": r"\b(PooledStreamingEventProcessor\w*|SubscribingEventProcessor\w*|PersistentStream\w*)\b",
    "sequencing policy": r"\b(\w*SequencingPolicy)\b",
    "conditional switch": r"(@ConditionalOnProperty\([^)]*\))",
}


def walk(root, predicate):
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS and not d.startswith(".")]
        for name in filenames:
            path = os.path.join(dirpath, name)
            if predicate(path, name):
                yield path


def rel(root, path):
    return os.path.relpath(path, root)


# ---------------------------------------------------------------- build files

def strip_ns(tag):
    return tag.split("}", 1)[-1]


def child(el, name):
    for c in el:
        if strip_ns(c.tag) == name:
            return c
    return None


def text(el, name):
    c = child(el, name)
    return c.text.strip() if c is not None and c.text else None


def maven(root):
    coords, props = [], {}
    poms = list(walk(root, lambda p, n: n == "pom.xml"))
    for pom in poms:
        try:
            tree = ET.parse(pom).getroot()
        except ET.ParseError as e:
            print(f"  ! cannot parse {rel(root, pom)}: {e}")
            continue
        p = child(tree, "properties")
        if p is not None:
            for c in p:
                k = strip_ns(c.tag)
                if "axon" in k.lower():
                    props[k] = (c.text or "").strip()
        for el in tree.iter():
            if strip_ns(el.tag) not in ("dependency", "plugin"):
                continue
            g, a, v = text(el, "groupId"), text(el, "artifactId"), text(el, "version")
            if g and AXON_GROUP.match(g):
                scope = text(el, "scope") or ""
                typ = text(el, "type") or ""
                coords.append((g, a, v or "(managed)", f"{scope} {typ}".strip(), rel(root, pom)))
    return poms, coords, props


GRADLE_COORD = re.compile(r"""["'](org\.axonframework[\w.]*|io\.axoniq[\w.]*):([\w.-]+)(?::([^"'\s]+))?["']""")


def gradle(root):
    files = list(walk(root, lambda p, n: n in ("build.gradle", "build.gradle.kts", "settings.gradle",
                                                "settings.gradle.kts", "gradle.properties")
                      or n.endswith(".versions.toml")))
    coords, props = [], {}
    for f in files:
        with open(f, encoding="utf-8", errors="replace") as fh:
            for line in fh:
                for m in GRADLE_COORD.finditer(line):
                    coords.append((m.group(1), m.group(2), m.group(3) or "(platform/catalog)", "", rel(root, f)))
                if "axon" in line.lower() and re.search(r"(version|=)", line) and not GRADLE_COORD.search(line):
                    props[f"{rel(root, f)}"] = props.get(f"{rel(root, f)}", "") + "\n      " + line.strip()
    return files, coords, props


# ------------------------------------------------------------ maven central

def central_versions(group, artifact):
    url = f"https://repo1.maven.org/maven2/{group.replace('.', '/')}/{artifact}/maven-metadata.xml"
    try:
        with urllib.request.urlopen(url, timeout=10) as r:
            data = r.read().decode()
    except Exception as e:  # noqa: BLE001 - report and continue
        return None, [], str(e)
    tree = ET.fromstring(data)
    versions = [v.text for v in tree.iter("version")]
    release = next((v.text for v in tree.iter("release")), None)
    return release, versions, None


# ---------------------------------------------------------------- configuration

def flatten_yaml(path):
    """Minimal YAML flattener for typical Spring config (maps, scalars, multi-doc)."""
    out, stack = [], []
    with open(path, encoding="utf-8", errors="replace") as fh:
        for raw in fh:
            line = raw.rstrip("\n")
            if line.strip() == "---":
                stack = []
                out.append(("---", ""))
                continue
            if not line.strip() or line.lstrip().startswith("#") or line.lstrip().startswith("- "):
                continue
            m = re.match(r"^(\s*)([^:#\s][^:#]*?)\s*:(?:\s+(.*))?$", line)
            if not m:
                continue
            indent, key, value = len(m.group(1)), m.group(2).strip().strip("\"'"), (m.group(3) or "")
            value = re.sub(r"\s+#.*$", "", value).strip()
            while stack and stack[-1][0] >= indent:
                stack.pop()
            stack.append((indent, key))
            if value and value not in ("|", ">", "|-", ">-"):
                out.append((".".join(k for _, k in stack), value))
    return out


def flatten_properties(path):
    out = []
    with open(path, encoding="utf-8", errors="replace") as fh:
        for line in fh:
            line = line.strip()
            if not line or line.startswith(("#", "!")):
                continue
            m = re.match(r"^([^=:\s]+)\s*[=:]\s*(.*)$", line)
            if m:
                out.append((m.group(1), m.group(2)))
    return out


def config(root):
    files = sorted(walk(root, lambda p, n: re.match(r"^(application|bootstrap)(-[\w.-]+)?\.(ya?ml|properties)$", n)
                        is not None and "/resources" in p.replace(os.sep, "/")))
    print("\n== 4. Storage / processor configuration per profile file")
    if not files:
        print("  (no application*.yml/properties found; configuration is probably done in code)")
    base_flags, modes = {}, []
    for f in files:
        name = os.path.basename(f)
        m = re.match(r"^(?:application|bootstrap)(?:-([\w.-]+))?\.", name)
        profile = m.group(1) if m and m.group(1) else "(default)"
        entries = flatten_yaml(f) if f.endswith((".yml", ".yaml")) else flatten_properties(f)
        hits = [(k, v) for k, v in entries if k != "---" and CONFIG_KEY.search(k)]
        where = "test" if "/src/test/" in f.replace(os.sep, "/") else "main"
        print(f"\n  [{where}] profile {profile}: {rel(root, f)}")
        if not hits:
            print("    (no storage-related keys)")
        for k, v in hits:
            print(f"    {k} = {v}")
            if profile == "(default)" and where == "main":
                base_flags[k] = v
        switches = [f"{k}={v}" for k, v in hits if STORAGE_SWITCH.search(k)]
        if switches and where == "main":
            modes.append((profile, switches))
    if modes:
        print("\n  candidate storage modes (profiles that set storage switches; confirm by reading the config classes):")
        for profile, switches in modes:
            print(f"    {profile}: {', '.join(switches)}")
    return base_flags


def code(root):
    print("\n== 5. Storage wiring in code (src/main)")
    sources = [p for p in walk(root, lambda p, n: n.endswith((".java", ".kt", ".kts")))
               if "/src/main/" in p.replace(os.sep, "/")]
    found = {k: {} for k in CODE_PATTERNS}
    for path in sources:
        with open(path, encoding="utf-8", errors="replace") as fh:
            content = fh.read()
        for label, pat in CODE_PATTERNS.items():
            for m in re.finditer(pat, content):
                name = m.group(1)
                if label != "conditional switch" and name in ("EventStore",):
                    continue
                found[label].setdefault(name, set()).add(rel(root, path))
    for label, names in found.items():
        if not names:
            continue
        print(f"  {label}:")
        for name, paths in sorted(names.items()):
            ps = sorted(paths)
            more = f" (+{len(ps) - 3} more)" if len(ps) > 3 else ""
            print(f"    {name}  <- {', '.join(ps[:3])}{more}")


def tests(root):
    print("\n== 6. Profiles activated by tests")
    counts = {}
    for path in walk(root, lambda p, n: n.endswith((".java", ".kt"))):
        if "/src/test/" not in path.replace(os.sep, "/"):
            continue
        with open(path, encoding="utf-8", errors="replace") as fh:
            for m in re.finditer(r"@ActiveProfiles\(([^)]*)\)", fh.read()):
                for prof in re.findall(r"\"([^\"]+)\"", m.group(1)):
                    counts.setdefault(prof, []).append(os.path.basename(path))
    if not counts:
        print("  (no @ActiveProfiles in tests: tests run with the default configuration)")
    for prof, classes in sorted(counts.items()):
        print(f"  {prof}: {len(classes)} test class(es), e.g. {', '.join(sorted(set(classes))[:3])}")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--root", default=".")
    ap.add_argument("--offline", action="store_true", help="skip Maven Central lookups")
    args = ap.parse_args()
    root = os.path.abspath(args.root)

    print(f"== 1. Build tool ({root})")
    poms, mcoords, mprops = maven(root)
    gfiles, gcoords, gprops = gradle(root)
    if poms:
        print(f"  Maven: {', '.join(rel(root, p) for p in poms)}")
        if os.path.exists(os.path.join(root, "mvnw")):
            print("  wrapper: ./mvnw")
    if gfiles:
        print(f"  Gradle: {', '.join(rel(root, p) for p in gfiles)}")
        if os.path.exists(os.path.join(root, "gradlew")):
            print("  wrapper: ./gradlew")
    if not poms and not gfiles:
        print("  no pom.xml or Gradle build found")
        return 1

    coords = mcoords + gcoords
    print("\n== 2. Axon / Axoniq coordinates")
    for g, a, v, extra, where in coords:
        print(f"  {g}:{a}:{v} {('[' + extra + ']') if extra else ''}  ({where})")
    if not coords:
        print("  none found: is this an Axon application?")
    if mprops:
        print("  version properties (pom):")
        for k, v in mprops.items():
            print(f"    {k} = {v}")
    if gprops:
        print("  axon-related lines (gradle):")
        for k, v in gprops.items():
            print(f"    {k}:{v}")
    declared = {v for _, _, v, _, _ in coords if v and not v.startswith("(")}
    resolved = set()
    for v in declared:
        m = re.match(r"^\$\{(.+)\}$", v)
        resolved.add(mprops.get(m.group(1), v) if m else v)
    majors = {v.split(".")[0] for v in resolved if re.match(r"^\d", v)}
    if majors and majors != {"5"}:
        print(f"  ! major version(s) {sorted(majors)} found: an AF4 to AF5 move is a migration, not an update")

    if not args.offline:
        print("\n== 3. Latest on Maven Central")
        seen = set()
        for g, a, *_ in coords:
            if (g, a) in seen:
                continue
            seen.add((g, a))
            release, versions, err = central_versions(g, a)
            if err:
                print(f"  {g}:{a}: lookup failed ({err})")
                continue
            recent = [v for v in versions if v.startswith("5.")][-6:]
            print(f"  {g}:{a}: release={release}  recent={recent}")

    flags = config(root)
    enabled = {k: v for k, v in flags.items() if k.endswith("axonserver.enabled")}
    print("\n  hint: default-profile Axon Server switch:", enabled or "not set (check the connector on the classpath; "
          "AF5 enables Axon Server by default when the connector is present)")
    code(root)
    tests(root)
    return 0


if __name__ == "__main__":
    sys.exit(main())
