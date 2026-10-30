"""Collect pinned PMD diagnostics for the canonical current-only Java quality adapter."""
from __future__ import annotations

import json
import os
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
BASE = "19bd8b1fa2d535929c768e6e197a9f464e89f953"


def report_inputs():
    support = pathlib.Path(os.environ["BUILDER_ASSURANCE_ROOT"]).resolve()
    sys.path.insert(0, str(support / "scripts"))
    from quality_report import build_report, parse_args

    args = parse_args(["--repo", str(ROOT), "--base", BASE, "--scope", "all",
                       "--level", "top", "--config", str(ROOT / ".agents/quality.yaml")])
    report = build_report(args)
    paths = [item["path"] for item in report["scope"]["changes"]
             if item["status"] != "D" and item["path"].endswith(".java")]
    return report["repository"]["current"], paths


def run_pmd(paths):
    executable = os.environ["PMD_BIN"]
    version = subprocess.check_output([executable, "--version"], text=True, timeout=30)
    if "PMD 7.29.0 " not in version:
        raise RuntimeError("PMD 7.29.0 is required")
    if not paths:
        return {"files": []}
    manifest = ROOT / ".agents/pmd-inputs.txt"
    manifest.write_text("\n".join(str(ROOT / path) for path in paths) + "\n")
    raw = ROOT / ".agents/pmd-raw.json"
    android = pathlib.Path(os.environ["ANDROID_HOME"])
    classpath = os.pathsep.join([str(android / "platforms/android-34/android.jar"),
                                str(ROOT / "build/classes")])
    command = [executable, "check", "--file-list", str(manifest), "-R",
               str(ROOT / ".agents/pmd-ruleset.xml"), "--aux-classpath", classpath,
               "--use-version", "java-11", "--no-cache", "--no-progress",
               "--no-fail-on-violation", "-f", "json", "-r", str(raw)]
    subprocess.run(command, cwd=ROOT, check=True, timeout=180)
    data = json.loads(raw.read_text())
    if data.get("processingErrors") or data.get("configurationErrors"):
        raise RuntimeError("PMD reported processing/configuration errors; inspect pmd-raw.json")
    return data


def normalize(data):
    findings = []
    for file in data.get("files", []):
        path = pathlib.Path(file["filename"]).resolve().relative_to(ROOT).as_posix()
        for item in file["violations"]:
            priority = item["priority"]
            severity = "strong" if priority <= 2 else "warning" if priority == 3 else "info"
            symbol = item.get("method") or item.get("class") or path
            findings.append({"metric": "pmd_v1." + item["rule"].lower(), "path": path,
                             "language": "java", "symbol": symbol,
                             "symbol_kind": "method" if item.get("method") else "file",
                             "current": 1, "baseline": None,
                             "mapping_status": "SYMBOL_MAPPING_UNKNOWN", "severity": severity,
                             "message": f"line {item['beginline']}: {item['description']}"})
    return findings


def main():
    snapshot, paths = report_inputs()
    data = run_pmd(paths)
    findings = normalize(data)
    if any(item["path"] not in paths for item in findings):
        raise RuntimeError("PMD returned findings outside the requested input set")
    current_snapshot, current_paths = report_inputs()
    if (snapshot, paths) != (current_snapshot, current_paths):
        raise RuntimeError("Source changed during PMD collection")
    artifact = {"schema_version": "1.0", "adapter_id": "hume-pmd", "adapter_version": "1.0",
                "metric_spec": "pmd_v1.native_rule_occurrences", "tool_name": "PMD",
                "tool_version": "7.29.0", "source_snapshot": snapshot,
                "coverage": {"files": [{"path": path, "language": "java"} for path in paths]},
                "findings": findings}
    (ROOT / ".agents/pmd-findings.json").write_text(json.dumps(artifact, sort_keys=True) + "\n")
    print(f"PMD coverage: {len(paths)} Java files; {len(findings)} native rule diagnostics")


if __name__ == "__main__":
    main()
