# Semantic Versioning & Release Discipline

## Overview
Apex adheres to [Semantic Versioning 2.0.0](https://semver.org/) (`MAJOR.MINOR.PATCH`).

## Pre-1.0 Semantics (`0.x.y`)
- **`0.x.y`**: Minor version increments (`0.1.0`, `0.2.0`) indicate architectural enhancements or new port abstractions. Deprecated ports maintain a backwards-compatibility window of at least one minor release.
- **`1.0.0`**: Declared when production governance, operational runbooks, and release engineering are complete.

## Version Derivation
The release version is derived dynamically from git tags (`vX.Y.Z`). Hand-editing version strings in project source files is prohibited.
