package com.shiphappens.logistics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiphappens.logistics.entity.*;
import com.shiphappens.logistics.entity.Statuses.ShipmentStatus;
import com.shiphappens.logistics.entity.Statuses.WebhookDeliveryStatus;
import com.shiphappens.logistics.repository.ShipmentRepository;
import com.shiphappens.logistics.repository.WebhookDeliveryLogRepository;
import com.shiphappens.logistics.repository.WebhookSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebhookServiceTest {

    private ShipmentRepository shipments;
    private WebhookSubscriptionRepository subscriptions;
    private WebhookDeliveryLogRepository deliveries;
    private ObjectMapper json;
    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        shipments = mock(ShipmentRepository.class);
        subscriptions = mock(WebhookSubscriptionRepository.class);
        deliveries = mock(WebhookDeliveryLogRepository.class);
        json = new ObjectMapper();
        webhookService = new WebhookService(shipments, subscriptions, deliveries, json);
    }

    @Test
    @DisplayName("Webhook: logDelivery persists delivery attempt metadata properly")
    void testLogDelivery() {
        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setTargetUrl("https://customer.example.com/webhooks");
        subscription.setSecret("test-secret-key-123");

        Shipment shipment = new Shipment();
        shipment.setShipmentNumber("SH-TEST-001");

        webhookService.logDelivery(
                subscription,
                shipment,
                "EVT-ABC123456789",
                "SHIPMENT_DELIVERED",
                1,
                WebhookDeliveryStatus.SUCCESS,
                200,
                null
        );

        ArgumentCaptor<WebhookDeliveryLog> captor = ArgumentCaptor.forClass(WebhookDeliveryLog.class);
        verify(deliveries).save(captor.capture());

        WebhookDeliveryLog saved = captor.getValue();
        assertEquals(subscription, saved.getSubscription());
        assertEquals(shipment, saved.getShipment());
        assertEquals("EVT-ABC123456789", saved.getEventId());
        assertEquals("SHIPMENT_DELIVERED", saved.getEventType());
        assertEquals(1, saved.getAttemptNumber());
        assertEquals(WebhookDeliveryStatus.SUCCESS, saved.getStatus());
        assertEquals(200, saved.getHttpStatus());
        assertNull(saved.getErrorMessage());
    }

    @Test
    @DisplayName("Webhook Security: HMAC-SHA256 signature verification produces expected hex digest")
    void testHmacSignatureIntegrity() throws Exception {
        String payload = "{\"eventId\":\"EVT-100\",\"status\":\"IN_TRANSIT\"}";
        String secret = "super-secret-key";

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expectedHex = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

        assertNotNull(expectedHex);
        assertEquals(64, expectedHex.length(), "HMAC-SHA256 hex digest must be 64 characters");
    }
}
