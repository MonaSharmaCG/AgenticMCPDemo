package com.cap.api.service.agent;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import com.cap.api.service.agent.GitAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Autonomous agent for processing JIRA defects and fixing application bugs.
 */
@Component
public class DefectProcessingAgent {
    private static final Logger log = LoggerFactory.getLogger(DefectProcessingAgent.class);
    // Track last seen bug suggestions by issue key
    private final Map<String, String> lastBugSuggestions = new HashMap<>();
    @Value("${jira.url}")
    private String jiraUrl;
    @Value("${jira.email}")
    private String jiraEmail;
    @Value("${jira.apiToken}")
    private String jiraApiToken;

    private String encodedAuth;

    @Autowired(required = false)
    private GitAgentService gitAgentService;

    @Value("${github.token:}")
    private String githubToken;

    private static final String PROCESSED_ISSUES_FILE = "agent_generated/processed_issues.txt";
    private Set<String> processedIssues = new HashSet<>();

    @PostConstruct
    public void init() {
        if (jiraEmail != null && jiraApiToken != null) {
            String auth = jiraEmail + ":" + jiraApiToken;
            encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
        }
        loadProcessedIssues();
    }

    /**
     * Main method for standalone execution.
     */
    public static void main(String[] args) {
        // For demo only: Spring context should be used in real app
        DefectProcessingAgent agent = new DefectProcessingAgent();
        agent.processDefects();
    }

    public void loadProcessedIssues() {
        try {
            Path path = Path.of(PROCESSED_ISSUES_FILE);
            if (java.nio.file.Files.exists(path)) {
                processedIssues.addAll(java.nio.file.Files.readAllLines(path));
            }
        } catch (Exception e) {
            log.warn("Could not load processed issues: {}", e.getMessage());
        }
    }

    /**
     * Fetches list of bugs from JIRA using AgenticClientUtil and local file for demo.
     */
    public List<String> fetchJiraBugs() {
        List<String> bugs = new java.util.ArrayList<>();
        try {
            if (jiraUrl == null || encodedAuth == null) {
                log.warn("JIRA credentials not initialized.");
                return bugs;
            }
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Basic " + encodedAuth);
            headers.set("Accept", "application/json");
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String searchUrl = jiraUrl + (jiraUrl.endsWith("/") ? "" : "/") + "rest/api/3/search/jql?jql=type=Bug+ORDER+BY+updated+DESC&fields=summary,status,description,comment";
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                searchUrl,
                org.springframework.http.HttpMethod.GET,
                entity,
                String.class
            );
            String jiraJson = response.getBody();
            String extracted = com.cap.api.service.AgenticClientUtil.extractStories(jiraJson);
            for (String line : extracted.split("\n")) {
                if (!line.trim().isEmpty()) {
                    bugs.add(line);
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch JIRA bugs: {}", e.getMessage(), e);
        }
        return bugs;
    }

    /**
     * Parses and analyzes bug descriptions.
     */
    public void analyzeBugs(List<String> bugs) {
        for (String bug : bugs) {
            log.info("Analyzing bug: {}", bug);
            // Extract affected class/method from bug description
            String affectedClass = null;
            String affectedMethod = null;
            String[] lines = bug.split("\n");
            for (String line : lines) {
                if (line.toLowerCase().contains("claimservice")) {
                    affectedClass = "ClaimService";
                }
                if (line.toLowerCase().contains("validation")) {
                    affectedMethod = "validateClaimDate";
                }
            }
            if (affectedClass != null) {
                log.info("Affected class: {}", affectedClass);
            }
            if (affectedMethod != null) {
                log.info("Affected method: {}", affectedMethod);
            }
        }
    }

    /**
     * Logs suggested fixes for bugs and prepares update comments for JIRA.
     */
    public void fixBugs(List<String> bugs) {
        for (String bug : bugs) {
            String suggestion = suggestFix(bug);
            String logEntry = "Suggested fix for bug: " + bug + "\nFix suggestion: " + suggestion + "\n";
            log.info(logEntry);
            // Log to defect_agent_log.txt
            try {
                java.nio.file.Files.write(
                    Path.of("defect_agent_log.txt"),
                    logEntry.getBytes(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
                );
            } catch (Exception e) {
                log.error("Failed to write to defect_agent_log.txt: {}", e.getMessage(), e);
            }
            // Update JIRA with the suggestion as a comment
            updateJiraWithComment(bug, suggestion);
        }
    }

    /**
     * Suggests a fix based on bug description (simple demo logic).
     */
    private String suggestFix(String bugDescription) {
        String suggestion = "Review and address described issue.";
        String[] fields = bugDescription.split("\\|");
        String summary = fields.length > 1 ? fields[1].toLowerCase() : "";
        String description = fields.length > 3 ? fields[3].toLowerCase() : "";

        if (summary.contains("validation") || description.contains("validation")) {
            suggestion = "Check and update validation logic as per requirements.";
        } else if (summary.contains("null pointer") || description.contains("null pointer")) {
            suggestion = "Add null checks to prevent NullPointerException.";
        } else if (summary.contains("calculation") || description.contains("calculation") || summary.contains("amount mismatch") || description.contains("amount mismatch")) {
            suggestion = "Review calculation logic and ensure correct field mapping.";
        } else if (summary.contains("performance") || description.contains("performance")) {
            suggestion = "Optimize code for better performance.";
        } else if (summary.contains("field mismatch") || description.contains("field mismatch")) {
            suggestion = "Check field mapping and data consistency between systems.";
        } else if (!summary.isEmpty() || !description.isEmpty()) {
            suggestion = "Investigate: " + summary + (description.isEmpty() ? "" : ". " + description);
        }
        return "Suggested update: " + suggestion;
    }

    /**
     * Updates JIRA with a comment for the bug using JIRA REST API.
     */
    private void updateJiraWithComment(String bug, String comment) {
        try {
            // Extract JIRA issue key from bug string (supports 'SCRUM-615|...')
            String issueKey = null;
            if (bug != null && !bug.isEmpty()) {
                String[] parts = bug.split("\\|");
                if (parts.length > 0 && parts[0].matches("[A-Z]+-\\d+")) {
                    issueKey = parts[0].trim();
                }
            }
            if (issueKey == null || issueKey.isEmpty()) {
                    log.warn("Could not extract JIRA issue key from bug: {}", bug);
                return;
            }
            if (jiraUrl == null || encodedAuth == null) {
                log.warn("JIRA credentials not initialized.");
                return;
            }
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Basic " + encodedAuth);
            headers.set("Content-Type", "application/json");
            String commentUrl = jiraUrl + (jiraUrl.endsWith("/") ? "" : "/") + "rest/api/3/issue/" + issueKey + "/comment";
            String getCommentsUrl = commentUrl;
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            // Fetch existing comments
            try {
                org.springframework.http.HttpEntity<String> getEntity = new org.springframework.http.HttpEntity<>(headers);
                org.springframework.http.ResponseEntity<String> getResponse = restTemplate.exchange(
                    getCommentsUrl,
                    org.springframework.http.HttpMethod.GET,
                    getEntity,
                    String.class
                );
                String responseBody = getResponse.getBody();
                boolean alreadyExists = false;
                if (responseBody != null && !responseBody.isEmpty()) {
                    // Parse and check if comment already exists
                    com.fasterxml.jackson.databind.JsonNode root = om.readTree(responseBody);
                    if (root.has("comments")) {
                        for (com.fasterxml.jackson.databind.JsonNode c : root.get("comments")) {
                            StringBuilder sb = new StringBuilder();
                            for (com.fasterxml.jackson.databind.JsonNode para : c.path("body").path("content")) {
                                for (com.fasterxml.jackson.databind.JsonNode text : para.path("content")) {
                                    if (text.has("text")) sb.append(text.get("text").asText());
                                }
                            }
                            if (sb.toString().contains(comment)) {
                                alreadyExists = true;
                                break;
                            }
                        }
                    }
                }
                if (alreadyExists) {
                    log.info("Comment already exists for JIRA issue {}, skipping.", issueKey);
                    return;
                }
            } catch (Exception ex) {
                log.error("Exception checking existing comments: {}", ex.getMessage(), ex);
            }
            // Build Atlassian doc format for comment body
            String payload = om.writeValueAsString(
                java.util.Map.of(
                    "body", java.util.Map.of(
                        "type", "doc",
                        "version", 1,
                        "content", java.util.List.of(
                            java.util.Map.of(
                                "type", "paragraph",
                                "content", java.util.List.of(
                                    java.util.Map.of(
                                        "type", "text",
                                        "text", comment
                                    )
                                )
                            )
                        )
                    )
                )
            );
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(payload, headers);
            try {
                org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    commentUrl,
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    String.class
                );
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Successfully posted comment to JIRA issue {}", issueKey);
                } else {
                    log.warn("Failed to post comment to JIRA issue {}: {}", issueKey, response.getStatusCode());
                }
            } catch (Exception ex) {
                log.error("Exception posting comment to JIRA: {}", ex.getMessage(), ex);
            }
        } catch (Exception e) {
            log.error("Error in updateJiraWithComment: {}", e.getMessage(), e);
        }
    }

    /**
     * Logs actions and updates JIRA tickets.
     */
    public void logAndUpdateJira(List<String> bugs) {
        // TODO: Implement JIRA update logic
    }

    /**
     * Main entry point for autonomous defect processing (single run).
     */
    public void processDefects() {
        // keep for backwards compatibility: single-run invocation
        List<String> bugs = fetchJiraBugs();
        analyzeBugs(bugs);
        for (String bug : bugs) {
            String issueKey = extractIssueKey(bug);
            if (issueKey != null && !isProcessed(issueKey)) {
                String branchName = createBranchName(issueKey, bug);
                gitAgentService.createBranchFromMainAndApplyFix(branchName, bug);
                // After fix and PR, comment PR link back to Jira
                String prUrl = "https://github.com/MonaSharmaCG/AgenticMCPDemo/pull/new/" + branchName;
                updateJiraWithComment(bug, "Code fix done and PR raised: " + prUrl);
                markProcessed(issueKey);
            }
        }
        }

    private String createBranchName(String issueKey, String bugDesc) {
        String shortDesc = bugDesc.length() > 20 ? bugDesc.substring(0, 20).replaceAll("[^a-zA-Z0-9]", "-") : bugDesc.replaceAll("[^a-zA-Z0-9]", "-");
        return "fix/" + issueKey + "-" + shortDesc;
    }

    private String extractIssueKey(String bugDesc) {
        // Try to extract JIRA issue key (e.g., SCRUM-123) from bug description
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([A-Z]+-\\d+)").matcher(bugDesc);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private boolean isProcessed(String issueKey) {
        return processedIssues.contains(issueKey);
    }

    private void markProcessed(String issueKey) {
        processedIssues.add(issueKey);
        try {
            Path path = Path.of(PROCESSED_ISSUES_FILE);
            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.write(path, processedIssues);
        } catch (Exception e) {
            log.warn("Could not persist processed issue {}: {}", issueKey, e.getMessage());
        }
    }

    /**
     * Single-run processing logic. Suitable for scheduled invocations.
     */
    public void processDefectsOnce() {
        List<String> bugs = fetchJiraBugs();
        java.util.List<String> newOrChangedBugs = new java.util.ArrayList<>();
        for (String bug : bugs) {
            String issueKey = null;
            if (bug != null && !bug.isEmpty()) {
                String[] parts = bug.split("\\|");
                if (parts.length > 0 && parts[0].matches("[A-Z]+-\\d+")) {
                    issueKey = parts[0].trim();
                }
            }
            String suggestion = suggestFix(bug);
            if (issueKey != null) {
                String lastSuggestion = lastBugSuggestions.get(issueKey);
                if (lastSuggestion == null || !lastSuggestion.equals(suggestion)) {
                    newOrChangedBugs.add(bug);
                    lastBugSuggestions.put(issueKey, suggestion);
                }
            }
        }
        if (!newOrChangedBugs.isEmpty()) {
            analyzeBugs(newOrChangedBugs);
            fixBugs(newOrChangedBugs);
            logAndUpdateJira(newOrChangedBugs);
            // write suggestions to a file to simulate generated code / artifacts
            try {
                java.nio.file.Path out = Path.of("agent_generated/last_suggestions.txt");
                java.nio.file.Files.createDirectories(out.getParent());
                StringBuilder sb = new StringBuilder();
                for (String s : newOrChangedBugs) sb.append(s).append("\n---\n");
                java.nio.file.Files.write(out, sb.toString().getBytes());
                // If GitAgentService is available, commit and push changes
                if (gitAgentService != null) {
                    String repoPath = "."; // root of repo
                    String branch = "agent/" + java.time.Instant.now().getEpochSecond();
                    String msg = "chore(agent): apply suggestions from MCP agent";
                    if (githubToken != null && !githubToken.isEmpty()) {
                        try {
                            String reviewers = System.getenv("AUTO_REVIEWERS");
                            String prResp = gitAgentService.commitPushAndCreatePr(repoPath, branch, msg, "main", "Automated changes from Agentic MCP", "Agent generated suggestions", reviewers == null ? "" : reviewers, githubToken);
                            log.info("PR Response: {}", prResp == null ? "(empty)" : prResp.substring(0, Math.min(prResp.length(), 400)));
                            // After PR creation, comment PR link back to Jira for each bug
                            for (String bug : newOrChangedBugs) {
                                String prUrl = "https://github.com/MonaSharmaCG/AgenticMCPDemo/pull/new/" + branch;
                                updateJiraWithComment(bug, "Code fix done and PR raised: " + prUrl);
                            }
                        } catch (Exception ex) {
                            log.error("Failed to push/create PR: {}", ex.getMessage(), ex);
                        }
                    } else {
                        log.info("GitHub token not set in application.yaml; skipping automated push.");
                    }
                }
            } catch (Exception e) {
                log.error("Failed to write suggestions or push changes: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Scheduled poller that runs every 10 seconds and delegates to processDefectsOnce.
     * The fixedDelay ensures we wait 10s after the completion of the last run.
     */
    @Scheduled(fixedDelayString = "10000")
    public void pollJiraScheduled() {
        try {
            processDefectsOnce();
        } catch (Exception e) {
            log.error("Scheduled defect processing error: {}", e.getMessage(), e);
        }
    }
}
