# Hume Madrid validation contract

Baseline: `19bd8b1fa2d535929c768e6e197a9f464e89f953` (upstream 7.8).
Read CONTRIBUTING.md; no direct pushes to main, no runtime dependency additions.

## Tool inputs

Use isolated JDK 17.0.21+9, Android SDK platform/build-tools 34, platform-tools 37.0.1,
PMD 7.29.0 and a Python environment with PyYAML 6.0.3/jsonschema 4.25.1.
Set JAVA_HOME, ANDROID_HOME, PMD_BIN and BUILDER_ASSURANCE_ROOT to the installed tools.
Never commit keys, machine-specific paths, device addresses, downloaded tools or raw logs.

## Build

`VERSION_CODE=7801 VERSION_NAME=7.8-madrid.1 ./build.sh`

Use an isolated private signing key. Set RELEASE_KEYSTORE, RELEASE_KEYSTORE_PASS and
RELEASE_KEY_ALIAS for that local fork key; these are not the upstream release credentials.
Verify the APK with `$ANDROID_HOME/build-tools/34.0.0/apksigner verify --print-certs build/tvos-home.apk`.
Never uninstall another installation to bypass signature checks without approval.

## Changed-code assurance

The canonical external dependency is builder-assurance 1.1.0 from shared-skills revision
`f2c8698e6c268d25214628ca313b520170715565`, policy schema 1.0.
The sorted scripts/*.py plus schemas/*.json content identity is SHA-256
`c59c2667dfdf9af598a272e623d4b995d41f451416c6360473b4a0a7d6c9a652`
(hash each relative path, NUL, bytes, NUL in sorted path order).
Its location is supplied through BUILDER_ASSURANCE_ROOT, not assumed from this checkout.

- Validate policy: `python "$BUILDER_ASSURANCE_ROOT/scripts/validate_quality_policy.py" .agents/quality.yaml`.
- Collect current Java evidence: `python checks/collect_java_quality.py` (required before report).
- Report: `python "$BUILDER_ASSURANCE_ROOT/scripts/quality_report.py" --repo . --base 19bd8b1fa2d535929c768e6e197a9f464e89f953 --scope all --level top --config .agents/quality.yaml --format json --output .agents/quality-report.json`.

PMD analyzes every changed Java file with the checked-in ruleset, Java 11 parsing and
Android build classpath. Native priorities 1/2 map to strong, 3 to warning, 4/5 to info.
Normalized v1 evidence is current-only: baseline is null and mapping is unknown; it must
match the canonical reporter's exact current snapshot and contain full Java coverage.
Parse/processing/configuration errors and stale/missing evidence block acceptance.
Raw full-file findings, including inherited monolith design findings, are retained as
advisory evidence; this is not a baseline-aware Java complexity ratchet.

The collection-convention rule ReturnEmptyCollectionRatherThanNull is not selected:
Suggestions.cached explicitly uses null as a missing/stale cache signal. This is a
pre-implementation rule-selection decision, not a source suppression or changed threshold.
All selected native design thresholds are unchanged; no handwritten paths are excluded.

## Regional checks

Host-only test dependencies (never packaged into the app): org.json `20250517`
(SHA-256 `3ea61b2a06e31edf1c91134fe9106b0ebb16628be169f3db75bc7a2b06b45796`)
and kxml2 `2.3.0`
(SHA-256 `f264dd9f79a1fde10ce5ecc53221eff24be4c9331c830b7d52f2f08a7b633de2`).
Resolve them from Maven Central; set TEST_JSON_JAR and TEST_XML_JAR to their verified files.

- Offline checks after build: `sh checks/run_regional_checks.sh`.
- Explicit live provider/feed integration: `sh checks/run_regional_checks.sh live`.

The checks invoke the actual Search/Suggestions request producers, selection migration,
weather seeding and News RSS parser. They cover quoted/accented queries, unrestricted versus
explicit original-language filtering, removed-only/mixed/empty selections, idempotence,
preserved credentials/shelf choices, manual/automatic/disabled weather and stale/imageless/
malformed feed entries. Live mode calls JustWatch Spain, requires nonempty title edges and
at least one offer matching each configured provider's technical name, and parses all configured feeds.
A country-mutated compilation must fail the Spain request assertion (a one-time regression
receipt is retained with task evidence; this does not alter the checked-in source).

Cache filename/consumer review is additionally required for all shelf variants and genres.
Missing/present receiver launch and Android-specific behavior need the device checks below;
JVM fixtures are not Android integration or AirPlay playback proof.

Device checks: Home/Back, speech cancellation/denial, app playback, weather and news, network-off
recovery, restart/wake, stock settings, casting and remote volume. Record screenshots and
Home frame times; record actual output resolution and untested 720p/1080p states.
The owner explicitly approved this initial personal-fork trial/deployment targeted to
Chromecast 4K (`sabrina`), Android 14. This supersedes the upstream Android 12 and
Android 9/1-GB acceptance requirement only for that target. Those versions remain
unverified; Android 14 evidence is not equivalent to testing them. The initial device
outputs 1920x1080; do not claim 720p/4K visual validation without actual checks.

AirPlay needs an authentic separate APK and real Apple-client playback/mirroring. Discovery
alone proves only advertisement. Human permission prompts and DRM limitations stay explicit.

Independent plan/candidate review is required before acceptance/deployment. Build success,
quality output, tests and review are distinct evidence classes. Missing gates remain blocked.
