package com.cap.api.service.agent;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RestController
@RequestMapping("/api/prompt")
public class PromptController {
    private String customPrompt = null;

    @PostMapping
    public ResponseEntity<String> setPrompt(@RequestBody String prompt) {
        this.customPrompt = prompt;
        return ResponseEntity.ok("Prompt updated");
    }

    @GetMapping
    public ResponseEntity<String> getPrompt() {
        return ResponseEntity.ok(customPrompt == null ? "" : customPrompt);
    }

    public String consumePrompt() {
        String p = customPrompt;
        customPrompt = null;
        return p;
    }
}
