# Simple helper to run the MCP DefectProcessingAgent once
# Requires: JAVA_HOME or `java` on PATH and mvn build completed
# Set environment variables before running:
# $env:JIRA_URL, $env:JIRA_EMAIL, $env:JIRA_API_TOKEN, $env:GITHUB_TOKEN (optional)

Write-Host "Running DefectProcessingAgent.processDefectsOnce() via spring boot java -jar"
$jar = Join-Path -Path "$(Get-Location)" -ChildPath "target\ClaimService-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $jar)) {
    Write-Host "Jar not found at $jar. Run mvn package first." -ForegroundColor Yellow
    exit 1
}
java -jar $jar --spring.main.web-application-type=none -Dspring.batch.job.enabled=false
