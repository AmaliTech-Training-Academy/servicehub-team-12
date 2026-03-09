#!/usr/bin/env python3

import subprocess
import sys
from pathlib import Path


def run_capture(command, cwd=None):
    result = subprocess.run(
        command,
        cwd=cwd,
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    return result


def run_or_fail(command, cwd):
    print(f"[pre-commit] Running in {cwd}: {' '.join(command)}")
    result = subprocess.run(command, cwd=cwd, check=False)
    if result.returncode != 0:
        print(f"[pre-commit] FAILED: {' '.join(command)}", file=sys.stderr)
        return False
    return True


def get_repo_root():
    result = run_capture(["git", "rev-parse", "--show-toplevel"])
    if result.returncode != 0:
        print("[pre-commit] Unable to resolve repository root.", file=sys.stderr)
        return None
    return Path(result.stdout.strip())


def get_staged_files(repo_root):
    result = run_capture(["git", "diff", "--cached", "--name-only"], cwd=repo_root)
    if result.returncode != 0:
        print("[pre-commit] Unable to list staged files.", file=sys.stderr)
        return None
    return [line.strip() for line in result.stdout.splitlines() if line.strip()]


def main():
    repo_root = get_repo_root()
    if repo_root is None:
        return 1

    staged_files = get_staged_files(repo_root)
    if staged_files is None:
        return 1

    if not staged_files:
        print("[pre-commit] No staged files detected. Skipping checks.")
        return 0

    backend_changed = any(path.startswith("backend/") for path in staged_files)
    qa_api_changed = any(path.startswith("qa/api-tests/") for path in staged_files)

    if backend_changed:
        if not run_or_fail(
            ["mvn", "clean", "compile", "checkstyle:check"], repo_root / "backend"
        ):
            return 1

    if qa_api_changed:
        if not run_or_fail(["mvn", "clean", "compile"], repo_root / "qa" / "api-tests"):
            return 1

    print("[pre-commit] All checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
