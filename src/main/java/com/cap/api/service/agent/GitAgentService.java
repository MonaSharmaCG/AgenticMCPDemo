package com.cap.api.service.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import com.cap.api.service.agent.NotificationAgent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class GitAgentService {
    @Value("${github.token:}")
    private String githubToken;

    @Autowired(required = false)
    private NotificationAgent notificationAgent;

    private static final Logger log = LoggerFactory.getLogger(GitAgentService.class);

    @PostConstruct
    public void logToken() {
        if (githubToken != null && !githubToken.isEmpty()) {
            log.info("GitHub token successfully injected from application.yaml");
        } else {
            log.warn("GitHub token NOT injected from application.yaml");
        }
    }

    /**
     * Creates a branch from main, applies the fix, pushes, and creates a PR.
     */
    public void createBranchFromMainAndApplyFix(String branchName, String bugDesc, String codeFix) {
        try {
            File repoDir = new File(".");
            runCommand(repoDir, "git", "checkout", "main");
            runCommand(repoDir, "git", "pull", "origin", "main");
            runCommand(repoDir, "git", "checkout", "-b", branchName);
            // Apply fix for bugDesc: write codeFix to agent_generated/fixes/<branchName>/fix.txt
            try {
                if (codeFix != null && !codeFix.isBlank()) {
                    Path outDir = Path.of("agent_generated", "fixes", branchName);
                    Files.createDirectories(outDir);
                    Path outFile = outDir.resolve("fix.txt");
                    Files.writeString(outFile, codeFix);
                }
            } catch (Exception e) {
                log.error("Failed to write code fix file: {}", e.getMessage(), e);
            }
            runCommand(repoDir, "git", "add", ".");
            runCommand(repoDir, "git", "commit", "-m", "Fix for " + branchName);
            runCommand(repoDir, "git", "push", "-u", "origin", branchName);
            // After pushing, create PR via GitHub API and post a summary comment
            try {
                if (githubToken != null && !githubToken.isBlank()) {
                    String prTitle = "Automated fix: " + branchName;
                    String prBody = "Agent-generated suggested fix. See agent_generated/fixes/" + branchName + "/fix.txt";
                    String prUrl = createPrOnly(branchName, prTitle, prBody, "");
                    log.info("Created PR: {}", prUrl == null ? "(no url)" : prUrl);
                    // Notify DLs after PR creation
                    if (notificationAgent != null) {
                        notificationAgent.notifyDLs("Automated PR created: " + prTitle);
                    } else {
                        log.info("NotificationAgent not available. PR: {}", prTitle);
                    }
                } else {
                    log.warn("GitHub token not configured; skipping PR creation for branch {}", branchName);
                }
            } catch (Exception e) {
                log.error("Failed to create PR: {}", e.getMessage(), e);
            }
            // Create PR (reuse commitPushAndCreatePr logic or call GitHub API)
            // Optionally trigger GitHub Actions workflow if needed
        } catch (Exception e) {
            log.error("Failed to create branch and apply fix: {}", e.getMessage(), e);
        }
    }

    /**
     * Create a PR for an existing branch and post a comment summarizing the agent's changes.
     * Returns the PR html_url if successful.
     */
    public String createPrOnly(String branchName, String prTitle, String prBody, String reviewersCsv) throws Exception {
        File repoDir = new File(".");
        // derive remote url
        ProcessBuilder rb = new ProcessBuilder("git", "config", "--get", "remote.origin.url");
        rb.directory(repoDir);
        Process pr = rb.start();
        String remoteUrl;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
            remoteUrl = r.readLine();
        }
        pr.waitFor();
        if (remoteUrl == null) throw new RuntimeException("Could not determine remote.origin.url");
        String owner = null, repo = null;
        String path = remoteUrl;
        if (path.endsWith(".git")) path = path.substring(0, path.length()-4);
        if (path.contains("github.com")) {
            if (path.startsWith("https://")) {
                String[] parts = path.split("github.com/");
                if (parts.length > 1) {
                    String[] prt = parts[1].split("/");
                    if (prt.length >= 2) { owner = prt[0]; repo = prt[1]; }
                }
            } else if (path.startsWith("git@")) {
                String[] parts = path.split(":");
                if (parts.length > 1) {
                    String[] prt = parts[1].split("/");
                    if (prt.length >= 2) { owner = prt[0]; repo = prt[1]; }
                }
            }
        }
        if (owner == null || repo == null) throw new RuntimeException("Could not parse owner/repo from remote url");

        String prApi = String.format("https://api.github.com/repos/%s/%s/pulls", owner, repo);
        String json = String.format("{\"title\":\"%s\",\"head\":\"%s\",\"base\":\"%s\",\"body\":\"%s\"}", escape(prTitle), escape(branchName), escape("main"), escape(prBody));
        URL url = new URL(prApi);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "token " + githubToken);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setDoOutput(true);
        conn.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
        int code = conn.getResponseCode();
        StringBuilder resp = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream()))) {
            String ln;
            while ((ln = r.readLine()) != null) resp.append(ln).append('\n');
        }
        ObjectMapper om = new ObjectMapper();
        try {
            JsonNode root = om.readTree(resp.toString());
            String prUrl = root.has("html_url") ? root.get("html_url").asText() : null;
            String prNum = root.has("number") ? root.get("number").asText() : null;
            // request reviewers if provided
            if (reviewersCsv != null && !reviewersCsv.isBlank() && prNum != null) {
                try {
                    String reviewersApi = String.format("https://api.github.com/repos/%s/%s/pulls/%s/requested_reviewers", owner, repo, prNum);
                    String revJson = String.format("{\"reviewers\":[%s]}", joinReviewers(reviewersCsv));
                    URL rurl = new URL(reviewersApi);
                    HttpURLConnection rc2 = (HttpURLConnection) rurl.openConnection();
                    rc2.setRequestMethod("POST");
                    rc2.setRequestProperty("Authorization", "token " + githubToken);
                    rc2.setRequestProperty("Accept", "application/vnd.github+json");
                    rc2.setDoOutput(true);
                    rc2.getOutputStream().write(revJson.getBytes(StandardCharsets.UTF_8));
                    int rcCode = rc2.getResponseCode();
                    log.info("Requested reviewers; HTTP code: {}", rcCode);
                } catch (Exception ex) {
                    log.error("Failed to request reviewers: {}", ex.getMessage(), ex);
                }
            }
            // Post summary comment to PR (issues API)
            if (prNum != null) {
                try {
                    String commentApi = String.format("https://api.github.com/repos/%s/%s/issues/%s/comments", owner, repo, prNum);
                    String commentJson = String.format("{\"body\":\"%s\"}", escape("[agenticBot] Agent applied suggested fix. See: " + prBody));
                    URL curl = new URL(commentApi);
                    HttpURLConnection cc = (HttpURLConnection) curl.openConnection();
                    cc.setRequestMethod("POST");
                    cc.setRequestProperty("Authorization", "token " + githubToken);
                    cc.setRequestProperty("Accept", "application/vnd.github+json");
                    cc.setDoOutput(true);
                    cc.getOutputStream().write(commentJson.getBytes(StandardCharsets.UTF_8));
                    int ccode = cc.getResponseCode();
                    log.info("Posted PR comment; HTTP code: {}", ccode);
                } catch (Exception ex) {
                    log.error("Failed to post PR comment: {}", ex.getMessage(), ex);
                }
            }
            return prUrl;
        } catch (Exception ex) {
            log.error("Failed to parse PR response: {}", ex.getMessage());
            return null;
        }
    }

    private int runCommand(File dir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        if (dir != null) pb.directory(dir);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                log.info(line);
            }
        }
        return p.waitFor();
    }

    /**
     * Commit all changes, create a branch, push to remote and create PR via GitHub API.
     * The method expects GITHUB_TOKEN in environment for authentication.
     */
    public String commitPushAndCreatePr(String repoPath, String branchName, String commitMessage, String baseBranch, String prTitle, String prBody, String reviewersCsv, String githubToken) throws Exception {
        File repoDir = new File(repoPath);
        // git checkout -b branchName
        int rc = runCommand(repoDir, "git", "checkout", "-b", branchName);
        if (rc != 0) throw new RuntimeException("Failed to create branch");
        // git add .
        runCommand(repoDir, "git", "add", ".");
        // git commit -m commitMessage
        runCommand(repoDir, "git", "commit", "-m", commitMessage, "--allow-empty");
        // git push origin branchName
        // Use token from application.yaml
        String token = githubToken;
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("GitHub token is required to push and create PR");
        }
        // update remote url to include token temporarily
        // derive remote url
        ProcessBuilder rb = new ProcessBuilder("git", "config", "--get", "remote.origin.url");
        rb.directory(repoDir);
        Process pr = rb.start();
        String remoteUrl;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream()))) {
            remoteUrl = r.readLine();
        }
        pr.waitFor();
        if (remoteUrl == null) throw new RuntimeException("Could not determine remote.origin.url");
        String authRemote = remoteUrl;
        if (remoteUrl.startsWith("https://")) {
            authRemote = remoteUrl.replaceFirst("https://", "https://" + token + "@");
            runCommand(repoDir, "git", "remote", "set-url", "origin", authRemote);
        }
        runCommand(repoDir, "git", "push", "-u", "origin", branchName);
        // restore original remote if we changed it
        if (!authRemote.equals(remoteUrl) && remoteUrl.startsWith("https://")) {
            runCommand(repoDir, "git", "remote", "set-url", "origin", remoteUrl);
        }

        // create PR via GitHub API; infer owner/repo from remote url
        // remote formats: https://github.com/owner/repo.git or git@github.com:owner/repo.git
        String owner = null, repo = null;
        if (remoteUrl.contains("github.com")) {
            String path = remoteUrl;
            if (path.endsWith(".git")) path = path.substring(0, path.length()-4);
            if (path.startsWith("https://")) {
                String[] parts = path.split("github.com/");
                if (parts.length > 1) {
                    String[] prt = parts[1].split("/");
                    if (prt.length >= 2) { owner = prt[0]; repo = prt[1]; }
                }
            } else if (path.startsWith("git@")) {
                String[] parts = path.split(":" );
                if (parts.length > 1) {
                    String[] prt = parts[1].split("/");
                    if (prt.length >= 2) { owner = prt[0]; repo = prt[1]; }
                }
            }
        }
        if (owner == null || repo == null) throw new RuntimeException("Could not parse owner/repo from remote url");

        String prApi = String.format("https://api.github.com/repos/%s/%s/pulls", owner, repo);
        String json = String.format("{\"title\":\"%s\",\"head\":\"%s\",\"base\":\"%s\",\"body\":\"%s\"}", escape(prTitle), escape(branchName), escape(baseBranch), escape(prBody));
        URL url = new URL(prApi);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "token " + token);
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setDoOutput(true);
        conn.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
        int code = conn.getResponseCode();
        StringBuilder resp = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream()))) {
            String ln;
            while ((ln = r.readLine()) != null) resp.append(ln).append('\n');
        }
        ObjectMapper om = new ObjectMapper();
        // optionally request reviewers
    if (reviewersCsv != null && !reviewersCsv.isBlank()) {
            // try to parse PR number via JSON
            try {
                JsonNode root = om.readTree(resp.toString());
                if (root.has("number")) {
                    String prNum = root.get("number").asText();
                    String reviewersApi = String.format("https://api.github.com/repos/%s/%s/pulls/%s/requested_reviewers", owner, repo, prNum);
                    String revJson = String.format("{\"reviewers\":[%s]}", joinReviewers(reviewersCsv));
                    URL rurl = new URL(reviewersApi);
                    HttpURLConnection rc2 = (HttpURLConnection) rurl.openConnection();
                    rc2.setRequestMethod("POST");
                    rc2.setRequestProperty("Authorization", "token " + token);
                    rc2.setRequestProperty("Accept", "application/vnd.github+json");
                    rc2.setDoOutput(true);
                    rc2.getOutputStream().write(revJson.getBytes(StandardCharsets.UTF_8));
                    int rcCode = rc2.getResponseCode();
                    log.info("Requested reviewers; HTTP code: {}", rcCode);
                } else {
                    log.warn("PR created but could not find PR number in response: {}", resp.toString());
                }
            } catch (Exception ex) {
                log.error("Failed to request reviewers: {}", ex.getMessage(), ex);
            }
        }
        return resp.toString();
    }

    private String joinReviewers(String csv) {
        String[] parts = csv.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<parts.length;i++) {
            if (i>0) sb.append(',');
            sb.append('"').append(parts[i].trim()).append('"');
        }
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");
    }
}
