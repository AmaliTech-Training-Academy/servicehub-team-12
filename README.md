# ServiceHub - Internal Service Request System

An internal service request management platform with intelligent routing, SLA tracking, and workflow automation.

## Architecture
- Spring Boot 3.2 + PostgreSQL + Thymeleaf
- JWT authentication, Role-based access control
- Port 8080 (backend), Port 5432 (postgres)

## Domain Split
### Dev A: Request Management (RequestService, RequestController)
### Dev B: Workflow/SLA (WorkflowService, SlaService, AssignmentService)
### Dev C: Auth/Dashboard (AuthService, DashboardService)

## Quick Start: docker-compose up --build

Default Users: emp@servicehub.com, agent@servicehub.com, mgr@servicehub.com (password123)
Swagger: http://localhost:8080/swagger-ui.html

## Git Hooks (Hybrid Monorepo)

Hook sources live in `ops-engine-room/git-hooks/`.

- Manual install from repo root:
	- `bash ops-engine-room/git-hooks/setup.sh`
- Maven auto-install (backend module):
	- Running backend Maven lifecycle from `initialize` onward copies
		`../ops-engine-room/git-hooks/pre-commit.py` to `../.git/hooks/pre-commit`
		and `../ops-engine-room/git-hooks/commit-msg.py` to `../.git/hooks/commit-msg`
		and marks it executable.
	- This runs via the Maven profile `local-git-hooks`, which auto-activates only when
		`ops-engine-room/git-hooks/pre-commit.py` exists.
	- In Docker/CI contexts where those files are not present in the build context,
		the profile stays inactive, so packaging does not fail.

The pre-commit hook runs monorepo path-based checks:

- If `backend/` changed: `mvn clean compile checkstyle:check` in `backend/` (Google Checkstyle rules)
- If `qa/api-tests/` changed: `mvn clean compile` in `qa/api-tests/`
- Infrastructure changes do not trigger Terraform checks in the hook.

Secret scanning is handled in CI with Gitleaks.

The commit message hook enforces Conventional Commits and issue traceability:

- Branch name must include an issue number (example: `feature/123-add-auth`)
- Commit subject format: `<type>(optional-scope): short description`
- Allowed types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `build`, `ci`, `perf`
- Short description must start lowercase, be 72 characters or fewer, and not end with a period
- Commit message must include an issue reference: `(#123)`, `Closes #123`, or `Fixes #123`
- Issue reference in the commit message is always required, even if the branch name has no ticket
- If branch has a ticket (example: `feature/123-add-auth`), commit message must reference the same ticket number

Example: `feat(auth): add login rate limiting (#123)`

## Data Engineering Deployment

Airflow deployment documentation is available at `data-engineering/README.md`.

### Commit Message Examples

Branch name examples (ticketed):
- `feature/23-add-auth`
- `bugfix/23-fix-jwt-expiry`
- `chore/23-update-deps`

Commit message examples (must include #23 when on a branch with ticket 23):
- `feat(auth): add login rate limiting (#23)`
- `fix(requests): handle missing SLA policy (Fixes #23)`
- `docs(readme): add setup steps (Closes #23)`
