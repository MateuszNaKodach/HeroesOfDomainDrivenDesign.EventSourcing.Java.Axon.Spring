#!/usr/bin/env python3
"""Collect and triage Axon Framework 5 / Axoniq Framework 5 release notes between two versions.

Usage:
  release_notes.py --from 5.3.0-SNAPSHOT --to 5.4.0 --out /tmp/notes

Fetches every release of AxonIQ/AxonFramework (tags axon-X.Y.Z[-RCn]) and
AxonIQ/axoniq-framework (tags axoniq-X.Y.Z[-RCn]) with FROM < version <= TO, using the
`gh` CLI when it is available and authenticated, else the public GitHub API (rate limited,
set GITHUB_TOKEN to raise the limit). Writes the raw notes per release and TRIAGE.md with
noise removed and behavior-sensitive items flagged. Standard library only.

Version order: X.Y.Z-SNAPSHOT < X.Y.Z-RC1 < X.Y.Z-RC2 < X.Y.Z. A SNAPSHOT baseline is fuzzy
(it may already contain part of the next release), so its own release is included.
"""
import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import urllib.request

REPOS = [("AxonIQ/AxonFramework", "axon-"), ("AxonIQ/axoniq-framework", "axoniq-")]

NOISE = re.compile(r"\b(bump|dependabot|renovate|sonar|github-dependencies|antora)\b|^\s*-\s*\[@", re.I)

# Categories of changes that can alter behavior without failing a compile. Keep the
# patterns broad: false positives cost a line of reading, a miss costs a production bug.
FLAGS = {
    "defaults": r"\bdefault|\bby default|enabled by|opt-?in|opt-?out",
    "autoconfig/beans": r"auto-?config|autoconfigur|\bbean|component registr|registerComponent|autowir|primary|"
                        r"\bstarter\b|optional|spring boot|conditional",
    "dependencies": r"\bdepend|module|artifact|classpath|move .* to\b",
    "connector/distribution": r"connector|axon ?server|distributed|routing|dispatch|local dispatch|shortcut|grpc",
    "processors/tokens": r"token|segment|claim|sequenc|processor|replay|reset|gap|persistent stream|\bpsep\b",
    "timeouts": r"timeout|janitor",
    "event store/snapshots": r"event ?stor|storage engine|snapshot|append|consistency|\bdcb\b|tag",
    "conversion/serialization": r"convert|serializ|jackson|xstream|payload|upcast|revision|type name",
    "messages/results": r"\bnull|result|interceptor|unit ?of ?work|processing ?context|handler",
    "dead letters": r"dead.?letter|\bdlq\b",
    "multi-tenancy/licensing": r"tenan|licen[cs]e|entitlement",
    "api/breaking": r"breaking|deprecat|remov|renam|replace|drop",
    "schema/sql": r"schema|\btable\b|column|isolation|transaction|jdbc|jpa|postgres",
}
FLAG_RE = {k: re.compile(v, re.I) for k, v in FLAGS.items()}


def version_key(v):
    m = re.match(r"^(\d+)\.(\d+)\.(\d+)(?:[-.]?(SNAPSHOT|RC|M)(\d*))?", v, re.I)
    if not m:
        raise ValueError(f"unrecognized version: {v}")
    major, minor, patch = int(m.group(1)), int(m.group(2)), int(m.group(3))
    kind, num = (m.group(4) or "").upper(), int(m.group(5) or 0)
    rank = {"SNAPSHOT": 0, "M": 1, "RC": 2, "": 3}[kind]
    return (major, minor, patch, rank, num)


def gh_ok():
    if not shutil.which("gh"):
        return False
    return subprocess.run(["gh", "auth", "status"], capture_output=True).returncode == 0


def fetch_page(repo, page, use_gh):
    path = f"repos/{repo}/releases?per_page=100&page={page}"
    if use_gh:
        out = subprocess.run(["gh", "api", path], capture_output=True, text=True)
        if out.returncode != 0:
            raise RuntimeError(out.stderr.strip())
        return json.loads(out.stdout)
    req = urllib.request.Request(f"https://api.github.com/{path}", headers={"Accept": "application/vnd.github+json"})
    if os.environ.get("GITHUB_TOKEN"):
        req.add_header("Authorization", f"Bearer {os.environ['GITHUB_TOKEN']}")
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.loads(r.read().decode())


def releases(repo, prefix, use_gh):
    page, result = 1, []
    while True:
        batch = fetch_page(repo, page, use_gh)
        if not batch:
            return result
        for rel in batch:
            tag = rel.get("tag_name", "")
            if tag.startswith(prefix):
                try:
                    key = version_key(tag[len(prefix):])
                except ValueError:
                    continue
                result.append((key, tag, rel))
        page += 1


def clean(line):
    line = re.sub(r"\[(PR )?#(\d+)\]\([^)]*\)", r"#\2", line)       # [PR #123](url) -> #123
    line = re.sub(r"\[([^\]]+)\]\(https?://[^)]*\)", r"\1", line)  # [text](url) -> text
    line = re.sub(r",? by @[\w-]+(\[bot\])?", "", line)
    return line.rstrip()


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--from", dest="frm", required=True, help="current version (exclusive)")
    ap.add_argument("--to", required=True, help="target version (inclusive)")
    ap.add_argument("--out", required=True, help="output directory")
    args = ap.parse_args()

    lo, hi = version_key(args.frm), version_key(args.to)
    if lo[0] != hi[0]:
        print("Major version change: this is a migration, not an update. Stopping.", file=sys.stderr)
        return 2
    os.makedirs(args.out, exist_ok=True)
    use_gh = gh_ok()
    print(f"source: {'gh api' if use_gh else 'api.github.com (unauthenticated unless GITHUB_TOKEN)'}")

    triage = [f"# Release notes triage: {args.frm} to {args.to}\n",
              "Flagged items come first in each release; categories are keyword matches, "
              "so read each one and decide whether the app is affected.\n"]
    summary = []
    for repo, prefix in REPOS:
        try:
            rels = releases(repo, prefix, use_gh)
        except Exception as e:  # noqa: BLE001 - surface and continue with the other repo
            print(f"! {repo}: {e}. Fallback: open https://github.com/{repo}/releases", file=sys.stderr)
            triage.append(f"\n## {repo}\n\nFETCH FAILED: {e}. Read https://github.com/{repo}/releases manually.\n")
            continue
        selected = sorted((r for r in rels if lo < r[0] <= hi), key=lambda r: r[0])
        triage.append(f"\n## {repo}\n")
        seen = {}
        if not selected:
            triage.append("No releases in range.\n")
        for _, tag, rel in selected:
            body = rel.get("body") or ""
            with open(os.path.join(args.out, f"{tag}.md"), "w", encoding="utf-8") as fh:
                fh.write(body)
            flagged, other, noise, repeated, section = [], [], 0, 0, ""
            for raw in body.splitlines():
                if raw.startswith("#"):
                    section = raw.lstrip("#").strip()
                    continue
                if not raw.strip().startswith(("-", "*")):
                    continue
                if NOISE.search(raw) or re.search(r"contributor", section, re.I):
                    noise += 1
                    continue
                line = clean(raw.strip().lstrip("-* ").strip())
                ident = re.sub(r"\s*\(#[^)]*\)\s*$", "", line).lower()
                if ident in seen:
                    repeated += 1
                    continue
                seen[ident] = tag
                cats = [k for k, rx in FLAG_RE.items() if rx.search(line)]
                (flagged if cats else other).append((section, line, cats))
            pre = " (pre-release)" if rel.get("prerelease") else ""
            triage.append(f"\n### {tag}{pre}, {(rel.get('published_at') or '')[:10]}\n")
            triage.append(f"{rel.get('html_url', '')}\n")
            if body.strip().startswith("**Full Changelog**") or len(body.strip()) < 200:
                triage.append("\nNotes are (almost) empty: the content is probably in the GA release notes, "
                              f"or read the compare view: https://github.com/{repo}/compare\n")
            for section_, line, cats in flagged:
                triage.append(f"- [{', '.join(cats)}] ({section_}) {line}")
            for section_, line, _ in other:
                triage.append(f"- ({section_}) {line}")
            triage.append(f"\n({len(flagged)} flagged, {len(other)} other, {noise} noise lines dropped, "
                          f"{repeated} already listed under an earlier tag)\n")
            summary.append(f"{tag}: {len(flagged)} flagged, {len(other)} other")

    with open(os.path.join(args.out, "TRIAGE.md"), "w", encoding="utf-8") as fh:
        fh.write("\n".join(triage) + "\n")
    print("\n".join(summary) if summary else "no releases found in range")
    print(f"wrote {os.path.join(args.out, 'TRIAGE.md')}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
