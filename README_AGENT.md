Agentic MCP Demo - agent run instructions

This repository contains a demo autonomous agent that polls JIRA, suggests fixes, posts comments, and can trigger Git operations to create a branch, commit, push and open a PR.

Quick run (local, simulated):

Prerequisites:
- Java 17+ and Maven
- A compiled jar: mvn -DskipTests package
- Set environment vars for JIRA and (optionally) GitHub:
  - JIRA_URL (e.g. https://your-domain.atlassian.net/)
  - JIRA_EMAIL
  - JIRA_API_TOKEN
  - GITHUB_TOKEN (optional, required to push/create PRs)
  - Alternatively you may set `github.token` in a local `application.yaml` (the app prefers it), but keep in mind the repo's `.gitignore` now ignores `application.yaml` so it won't be committed. Using env vars or a secret manager is still recommended.
  - AUTO_REVIEWERS (optional, comma-separated GitHub usernames)

Run once via PowerShell:

PS> $env:JIRA_URL = 'https://your-domain.atlassian.net/'; $env:JIRA_EMAIL='you@company.com'; $env:JIRA_API_TOKEN='xxxxx'; $env:GITHUB_TOKEN='ghp_xxx'; .\scripts\agent-run.ps1

Notes:
- The agent polls JIRA when the Spring context is running; running the jar will start scheduled polling (every 10s). The PowerShell script runs the jar in a non-web mode suitable for demo runs.
- The Git operations require a valid GITHUB_TOKEN with repo permissions. The token is applied temporarily to the remote URL to push.
- The agent writes suggestion outputs to `agent_generated/last_suggestions.txt` and `defect_agent_log.txt`.

If you want me to run the simulated flow locally (create branch, commit, push) I can prepare and run git commands — but pushing and creating PRs requires your GH token configured in the environment and network access.
