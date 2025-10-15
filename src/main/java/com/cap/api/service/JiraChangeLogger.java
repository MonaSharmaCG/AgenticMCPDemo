package com.cap.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JiraChangeLogger {

    private static final Logger log = LoggerFactory.getLogger(JiraChangeLogger.class);
    private static final String CHANGE_LOG_FILE = "jira_update.txt";

    public static void logJiraChanges(String previousJson, String currentJson) {
        try {
            List<String> changes = getJiraStoryChanges(previousJson, currentJson);
            if (!changes.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String change : changes) {
                    sb.append(change).append("\n");
                }
                log.info("[JiraChangeLogger] Writing change log to: {}", CHANGE_LOG_FILE);
                log.info("[JiraChangeLogger] Change log contents:\n{}", sb.toString());
                Files.write(Path.of(CHANGE_LOG_FILE), sb.toString().getBytes());
            } else {
                log.info("[JiraChangeLogger] No changes detected, no file written.");
            }
        } catch (Exception e) {
            log.error("Failed to log Jira changes: {}", e.getMessage(), e);
            e.printStackTrace();
        }
    }

    // Returns a list of change descriptions for updated stories
    private static List<String> getJiraStoryChanges(String previousJson, String currentJson) {
        List<String> changes = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode prevRoot = mapper.readTree(previousJson);
            JsonNode currRoot = mapper.readTree(currentJson);
            JsonNode prevIssues = prevRoot.path("issues");
            JsonNode currIssues = currRoot.path("issues");
            if (!prevIssues.isArray() || !currIssues.isArray()) return changes;
            
            // Map previous issues by key
            java.util.Map<String, JsonNode> prevMap = new java.util.HashMap<>();
            for (JsonNode issue : prevIssues) {
                String key = issue.path("key").asText("");
                prevMap.put(key, issue);
            }
            
            for (JsonNode currIssue : currIssues) {
                String key = currIssue.path("key").asText("");
                JsonNode prevIssue = prevMap.get(key);
                
                if (prevIssue != null) {
                    String prevSummary = prevIssue.path("fields").path("summary").asText("");
                    String currSummary = currIssue.path("fields").path("summary").asText("");
                    String prevStatus = prevIssue.path("fields").path("status").path("name").asText("");
                    String currStatus = currIssue.path("fields").path("status").path("name").asText("");
                    String prevDescription = prevIssue.path("fields").path("description").asText("");
                    String currDescription = currIssue.path("fields").path("description").asText("");
                    String prevConfluence = prevIssue.path("fields").path("customfield_confluence").asText("");
                    String currConfluence = currIssue.path("fields").path("customfield_confluence").asText("");
                    
                    // Collect all comments bodies
                    StringBuilder prevComments = new StringBuilder();
                    JsonNode prevCommentsNode = prevIssue.path("fields").path("comment").path("comments");
                    if (prevCommentsNode.isArray()) {
                        for (JsonNode comment : prevCommentsNode) {
                            prevComments.append(comment.path("body").asText("")).append("||");
                        }
                    }
                    
                    StringBuilder currComments = new StringBuilder();
                    JsonNode currCommentsNode = currIssue.path("fields").path("comment").path("comments");
                    if (currCommentsNode.isArray()) {
                        for (JsonNode comment : currCommentsNode) {
                            currComments.append(comment.path("body").asText("")).append("||");
                        }
                    }
                    
                    boolean changed = false;
                    if (!prevSummary.equals(currSummary) || 
                        !prevStatus.equals(currStatus) || 
                        !prevDescription.equals(currDescription) || 
                        !prevConfluence.equals(currConfluence) || 
                        !prevComments.toString().equals(currComments.toString())) {
                        changed = true;
                    }
                    
                    if (changed) {
                        // Add the whole story content instead of just the changes
                        StringBuilder storyContent = new StringBuilder();
                        storyContent.append("Key: ").append(key).append("\n");
                        storyContent.append("Summary: ").append(currSummary).append("\n");
                        storyContent.append("Status: ").append(currStatus).append("\n");
                        storyContent.append("Description: ").append(currDescription).append("\n");
                        if (!currConfluence.isEmpty()) {
                            storyContent.append("Confluence Content: ").append(currConfluence).append("\n");
                        }
                        if (currComments.length() > 0) {
                            storyContent.append("Comments: ").append(currComments).append("\n");
                        }
                        changes.add(storyContent.toString());
                    }
                } else {
                    // New story added - include full content
                    StringBuilder storyContent = new StringBuilder();
                    storyContent.append("Key: ").append(key).append("\n");
                    storyContent.append("Summary: ").append(currIssue.path("fields").path("summary").asText("")).append("\n");
                    storyContent.append("Status: ").append(currIssue.path("fields").path("status").path("name").asText("")).append("\n");
                    storyContent.append("Description: ").append(currIssue.path("fields").path("description").asText("")).append("\n");
                    String confluence = currIssue.path("fields").path("customfield_confluence").asText("");
                    if (!confluence.isEmpty()) {
                        storyContent.append("Confluence Content: ").append(confluence).append("\n");
                    }
                    
                    StringBuilder comments = new StringBuilder();
                    JsonNode commentsNode = currIssue.path("fields").path("comment").path("comments");
                    if (commentsNode.isArray()) {
                        for (JsonNode comment : commentsNode) {
                            comments.append(comment.path("body").asText("")).append("||");
                        }
                    }
                    if (comments.length() > 0) {
                        storyContent.append("Comments: ").append(comments).append("\n");
                    }
                    changes.add(storyContent.toString());
                }
            }
            
            // Detect removed stories
            for (String key : prevMap.keySet()) {
                boolean found = false;
                for (JsonNode currIssue : currIssues) {
                    if (currIssue.path("key").asText("").equals(key)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    JsonNode prevIssue = prevMap.get(key);
                    StringBuilder storyContent = new StringBuilder();
                    storyContent.append("REMOVED - Key: ").append(key).append("\n");
                    storyContent.append("Summary: ").append(prevIssue.path("fields").path("summary").asText("")).append("\n");
                    storyContent.append("Status: ").append(prevIssue.path("fields").path("status").path("name").asText("")).append("\n");
                    storyContent.append("Description: ").append(prevIssue.path("fields").path("description").asText("")).append("\n");
                    changes.add(storyContent.toString());
                }
            }
        } catch (Exception e) {
            // Ignore parse errors
            log.error("Error parsing JSON: {}", e.getMessage(), e);
        }
        return changes;
    }
}