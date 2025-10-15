package com.cap.api.service.agent;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;

import java.util.List;

/**
 * Autonomous agent for processing JIRA defects and fixing application bugs.
 */
@Component
public class DefectProcessingAgent {
    // Track last seen bug suggestions by issue key
    private final java.util.Map<String, String> lastBugSuggestions = new java.util.HashMap<>();
    @Value("${jira.url}")
    private String jiraUrl;
    @Value("${jira.email}")
    private String jiraEmail;
    @Value("${jira.apiToken}")
    private String jiraApiToken;

    private String encodedAuth;

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
                System.out.println("JIRA credentials not initialized.");
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
            System.out.println("Failed to fetch JIRA bugs: " + e.getMessage());
        }
        return bugs;
    }

    /**
     * Parses and analyzes bug descriptions.
     */
    public void analyzeBugs(List<String> bugs) {
        for (String bug : bugs) {
            System.out.println("Analyzing bug: " + bug);
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
                System.out.println("Affected class: " + affectedClass);
            }
            if (affectedMethod != null) {
                System.out.println("Affected method: " + affectedMethod);
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
            System.out.println(logEntry);
            // Log to defect_agent_log.txt
            try {
                java.nio.file.Files.write(
                    java.nio.file.Paths.get("defect_agent_log.txt"),
                    logEntry.getBytes(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
                );
            } catch (Exception e) {
                System.out.println("Failed to write to defect_agent_log.txt: " + e.getMessage());
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
                System.out.println("Could not extract JIRA issue key from bug: " + bug);
                return;
            }
            if (jiraUrl == null || encodedAuth == null) {
                System.out.println("JIRA credentials not initialized.");
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
                    System.out.println("Comment already exists for JIRA issue " + issueKey + ", skipping.");
                    return;
                }
            } catch (Exception ex) {
                System.out.println("Exception checking existing comments: " + ex.getMessage());
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
                    System.out.println("Successfully posted comment to JIRA issue " + issueKey);
                } else {
                    System.out.println("Failed to post comment to JIRA issue " + issueKey + ": " + response.getStatusCode());
                }
            } catch (Exception ex) {
                System.out.println("Exception posting comment to JIRA: " + ex.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Error in updateJiraWithComment: " + e.getMessage());
        }
    }

    /**
     * Logs actions and updates JIRA tickets.
     */
    public void logAndUpdateJira(List<String> bugs) {
        // TODO: Implement JIRA update logic
    }

    /**
     * Main entry point for autonomous defect processing.
     */
    public void processDefects() {
        while (true) {
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
            }
            try {
                Thread.sleep(5000); // Poll every 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
