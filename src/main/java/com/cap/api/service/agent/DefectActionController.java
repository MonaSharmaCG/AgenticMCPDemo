package com.cap.api.service.agent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RestController
@RequestMapping("/api/defect")
public class DefectActionController {
    @Autowired
    private DefectProcessingAgent defectProcessingAgent;

    /**
     * Trigger processing of a specific JIRA ticket by issue key.
     * Example: POST /api/defect/process?issueKey=SCRUM-123
     */
    @PostMapping("/process")
    public ResponseEntity<String> processDefect(@RequestParam String issueKey) {
        defectProcessingAgent.processDefectByIssueKey(issueKey);
        return ResponseEntity.ok("Processing triggered for JIRA issue: " + issueKey);
    }
}
