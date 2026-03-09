#!/usr/bin/env python3

import re
import subprocess
import sys
from pathlib import Path


ALLOWED_TYPES = "feat|fix|docs|style|refactor|test|chore|build|ci|perf"
HEADER_PATTERN = re.compile(
    rf"^(?P<type>{ALLOWED_TYPES})(?:\((?P<scope>[a-z0-9._\-/]+)\))?: (?P<description>.+)$"
)
ISSUE_REFERENCE_PATTERN = re.compile(
    r"\(#(?P<issue1>\d+)\)|(?:(?:Closes|Fixes)\s+#(?P<issue2>\d+))"
)
BRANCH_TICKET_PATTERN = re.compile(r"(?<!\d)(?P<issue>\d+)(?!\d)")
MAX_DESCRIPTION_LENGTH = 72
VALID_EXAMPLE = "feat(auth): add login rate limiting (#123)"


def run_capture(command):
    return subprocess.run(
        command,
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )


def get_current_branch_name():
    result = run_capture(["git", "rev-parse", "--abbrev-ref", "HEAD"])
    if result.returncode != 0:
        return None
    branch = result.stdout.strip()
    if branch == "HEAD":
        return None
    return branch


def extract_branch_ticket(branch_name):
    if not branch_name:
        return None
    match = BRANCH_TICKET_PATTERN.search(branch_name)
    if not match:
        return None
    return match.group("issue")


def extract_issue_references(message):
    references = []
    for match in ISSUE_REFERENCE_PATTERN.finditer(message):
        issue_number = match.group("issue1") or match.group("issue2")
        if issue_number:
            references.append(issue_number)
    return references


def validate_subject(subject):
    errors = []
    match = HEADER_PATTERN.match(subject)
    if not match:
        errors.append(
            "Commit subject must match '<type>(optional-scope): short description' with an allowed type."
        )
        return errors

    description = match.group("description")
    if not description:
        errors.append("Short description is required.")
        return errors

    if not description[0].islower():
        errors.append("Short description must start with a lowercase letter.")

    if len(description) > MAX_DESCRIPTION_LENGTH:
        errors.append(
            f"Short description must not exceed {MAX_DESCRIPTION_LENGTH} characters."
        )

    if description.endswith("."):
        errors.append("Short description must not end with a period.")

    return errors


def print_errors(errors):
    print("[commit-msg] Commit message validation failed:", file=sys.stderr)
    for error in errors:
        print(f"[commit-msg] - {error}", file=sys.stderr)
    print(f"[commit-msg] Example valid message: {VALID_EXAMPLE}", file=sys.stderr)


def main():
    if len(sys.argv) < 2:
        print("[commit-msg] Missing commit message file path.", file=sys.stderr)
        return 1

    commit_msg_file = Path(sys.argv[1])
    if not commit_msg_file.is_file():
        print("[commit-msg] Commit message file not found.", file=sys.stderr)
        return 1

    content = commit_msg_file.read_text(encoding="utf-8", errors="ignore")
    lines = [line.strip() for line in content.splitlines() if line.strip()]
    if not lines:
        print("[commit-msg] Empty commit message is not allowed.", file=sys.stderr)
        return 1

    subject = lines[0]
    branch_name = get_current_branch_name()
    branch_ticket = extract_branch_ticket(branch_name)
    issue_references = extract_issue_references(content)

    errors = []
    errors.extend(validate_subject(subject))

    if not issue_references:
        errors.append(
            "Commit message must include an issue reference: '(#123)', 'Closes #123', or 'Fixes #123'."
        )

    if branch_ticket and branch_ticket not in issue_references:
        errors.append(
            f"Branch ticket #{branch_ticket} must be referenced in commit message."
        )

    if errors:
        print_errors(errors)
        return 1

    return 0


if __name__ == "__main__":
    sys.exit(main())
