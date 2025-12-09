package com.codesage.webhook;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/webhook")
@Slf4j
public class GitHubWebhookController {

    private final com.codesage.queue.AnalysisProducer analysisProducer;

    public GitHubWebhookController(com.codesage.queue.AnalysisProducer analysisProducer) {
        this.analysisProducer = analysisProducer;
    }

    @PostMapping("/github")
    public void handleGitHubEvent(
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "ping") String eventType,
            @RequestBody Map<String, Object> payload) {

        log.info("Received GitHub event: {}", eventType);
        analysisProducer.sendToQueue(payload);
    }

    @org.springframework.web.bind.annotation.GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
                "status", "running",
                "service", "CodeSage Webhook Handler",
                "timestamp", java.time.Instant.now().toString(),
                "message", "Ready to receive GitHub webhook events");
    }
}
