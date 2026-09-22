package com.shiphappens.logistics.controller;

import com.shiphappens.logistics.dto.Requests;
import com.shiphappens.logistics.dto.Responses.WebhookSubscriptionResponse;
import com.shiphappens.logistics.mapper.EntityMapper;
import com.shiphappens.logistics.service.LogisticsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final LogisticsService service;
    private final EntityMapper mapper;

    public WebhookController(LogisticsService service, EntityMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<WebhookSubscriptionResponse> create(@Valid @RequestBody Requests.WebhookSubscription request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toWebhookSubscriptionResponse(service.createWebhookSubscription(request)));
    }

    @GetMapping("/subscriptions")
    public List<WebhookSubscriptionResponse> list() {
        return service.listWebhookSubscriptions().stream()
                .map(mapper::toWebhookSubscriptionResponse)
                .toList();
    }

    @DeleteMapping("/subscriptions/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteWebhookSubscription(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mock-receiver")
    public Map<String, Object> mock(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestBody Map<String, Object> payload) {
        return Map.of(
                "received", true,
                "eventId", payload.get("eventId"),
                "signaturePresent", signature != null
        );
    }
}
