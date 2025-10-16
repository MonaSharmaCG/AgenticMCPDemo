package com.cap.api.service.agent;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;

import java.nio.file.Path;
import java.util.List;
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
    private final java.util.Map<String, String> lastBugSuggestions = new java.util.HashMap<>();
    @Value("${jira.url}")
    private String jiraUrl;
    @Value("${jira.email}")
    private String jiraEmail;
    @Value("${jira.apiToken}")
    private String jiraApiToken;

    @Value("${github.token:}")
    private String githubToken;

    private String encodedAuth;

    @Autowired(required = false)
    private GitAgentService gitAgentService;

    @PostConstruct
    public void init() {
        if (jiraEmail != null && jiraApiToken != null) {
            String auth = jiraEmail + ":" + jiraApiToken;
            encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
        }
    }
    /**
     * Main method for standalone execution.
     */
    public static void main(String[] args) {
        DefectProcessingAgent agent = new DefectProcessingAgent();
        agent.processDefects();
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
            // Use POST /rest/api/3/search/jql with JSON body to avoid URL-encoding issues
            String searchUrl = jiraUrl + (jiraUrl.endsWith("/") ? "" : "/") + "rest/api/3/search/jql";
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String,Object> bodyMap = new java.util.HashMap<>();
            bodyMap.put("jql", "type=Bug ORDER BY updated DESC");
            bodyMap.put("fields", java.util.List.of("summary","status","description","comment"));
            String payload = om.writeValueAsString(bodyMap);
            headers.set("Content-Type", "application/json");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(payload, headers);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                searchUrl,
                org.springframework.http.HttpMethod.POST,
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
        processDefectsOnce();
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
                    // token: prefer injected application property `github.token`, fall back to env var GITHUB_TOKEN
                    String token = this.githubToken;
                    if (token == null || token.isEmpty()) token = System.getenv("GITHUB_TOKEN");
                    if (token != null && !token.isEmpty()) {
                        try {
                            String reviewers = System.getenv("AUTO_REVIEWERS");
                            // attempt to create a more meaningful branch name using the first issue key
                            String firstIssueKey = null;
                            for (String s : newOrChangedBugs) {
                                if (s != null && !s.isEmpty()) {
                                    String[] p = s.split("\\|");
                                    if (p.length > 0 && p[0].matches("[A-Z]+-\\d+")) {
                                        firstIssueKey = p[0];
                                        break;
                                    }
                                }
                            }
                            if (firstIssueKey != null) {
                                branch = "agent/fix-" + firstIssueKey + "-" + java.time.Instant.now().getEpochSecond();
                            }
                            String prResp = gitAgentService.commitPushAndCreatePr(repoPath, branch, msg, "main", "Automated changes from Agentic MCP", "Agent generated suggestions", reviewers == null ? "" : reviewers);
                            log.info("PR Response: {}", prResp == null ? "(empty)" : prResp.substring(0, Math.min(prResp.length(), 400)));
                            // Try to parse PR URL and comment it back to Jira issues
                            try {
                                if (prResp != null && !prResp.isBlank()) {
                                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                                    com.fasterxml.jackson.databind.JsonNode root = om.readTree(prResp);
                                    String prUrl = null;
                                    if (root.has("html_url")) prUrl = root.get("html_url").asText();
                                    else if (root.has("url")) prUrl = root.get("url").asText();
                                    if (prUrl != null) {
                                        for (String bugStr : newOrChangedBugs) {
                                            String issueKey = null;
                                            if (bugStr != null && !bugStr.isEmpty()) {
                                                String[] parts = bugStr.split("\\|");
                                                if (parts.length > 0 && parts[0].matches("[A-Z]+-\\d+")) {
                                                    issueKey = parts[0].trim();
                                                }
                                            }
                                            if (issueKey != null) {
                                                String comment = "Automated PR created: " + prUrl + "\nPlease review the proposed fix.";
                                                updateJiraWithComment(issueKey + "|", comment);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                log.error("Failed to parse PR response or post PR link to JIRA: {}", ex.getMessage(), ex);
                            }
                        } catch (Exception ex) {
                            log.error("Failed to push/create PR: {}", ex.getMessage(), ex);
                        }
                    } else {
                        log.info("No GitHub token found in application property 'github.token' nor environment variable GITHUB_TOKEN; skipping automated push.");
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
