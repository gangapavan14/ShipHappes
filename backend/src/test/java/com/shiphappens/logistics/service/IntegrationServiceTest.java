package com.shiphappens.logistics.service;

import com.shiphappens.logistics.dto.Requests;
import com.shiphappens.logistics.entity.*;
import com.shiphappens.logistics.mapper.ExternalOrderMapper;
import com.shiphappens.logistics.repository.IntegrationRequestLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IntegrationServiceTest {

    private LogisticsService logisticsService;
    private ExternalOrderMapper mapper;
    private IntegrationRequestLogRepository logsRepository;
    private IntegrationService integrationService;

    @BeforeEach
    void setUp() {
        logisticsService = mock(LogisticsService.class);
        mapper = new ExternalOrderMapper();
        logsRepository = mock(IntegrationRequestLogRepository.class);
        integrationService = new IntegrationService(logisticsService, mapper, logsRepository);
    }

    private Requests.ExternalOrder sampleRequest(String externalId) {
        return new Requests.ExternalOrder(
                externalId,
                "CUST-01",
                new Requests.Address("123 Origin St", null, "Hyderabad", "Telangana", "500001", "India"),
                new Requests.Address("456 Destination Ave", null, "Bengaluru", "Karnataka", "560001", "India"),
                List.of(new Requests.Package("Medical Supplies", new BigDecimal("12.50"), new BigDecimal("30"), new BigDecimal("20"), new BigDecimal("15"), new BigDecimal("5000")))
        );
    }

    @Test
    @DisplayName("Idempotency: New external order creates shipment and returns idempotent=false (201 Created)")
    void testNewExternalOrderCreatesShipment() {
        String externalId = "EXT-ORD-1001";
        Requests.ExternalOrder request = sampleRequest(externalId);

        when(logisticsService.orderByExternalReference(externalId)).thenReturn(Optional.empty());

        Customer customer = new Customer();
        customer.setCustomerCode("CUST-01");
        customer.setName("Acme Health");
        when(logisticsService.customerByCode("CUST-01")).thenReturn(customer);

        LogisticsOrder order = new LogisticsOrder();
        order.setOrderNumber("ORD-999");

        Shipment shipment = new Shipment();
        shipment.setShipmentNumber("SH-999");
        shipment.setStatus(Statuses.ShipmentStatus.CREATED);
        shipment.setOrder(order);

        when(logisticsService.createOrder(any(), eq(externalId))).thenReturn(shipment);

        IntegrationService.Result result = integrationService.submit(request, "REQ-1234");

        assertTrue(result.success());
        assertFalse(result.idempotent(), "First submission should not be idempotent");
        assertEquals(externalId, result.externalOrderId());
        assertEquals("ORD-999", result.orderId());
        assertEquals("SH-999", result.shipmentId());
        assertEquals("CREATED", result.status());

        ArgumentCaptor<IntegrationRequestLog> captor = ArgumentCaptor.forClass(IntegrationRequestLog.class);
        verify(logsRepository).save(captor.capture());
        assertEquals(201, captor.getValue().getHttpStatus());
        assertEquals(externalId, captor.getValue().getExternalReference());
    }

    @Test
    @DisplayName("Idempotency: Duplicate external order returns existing order details with idempotent=true (200 OK)")
    void testDuplicateExternalOrderReturnsExistingOrder() {
        String externalId = "EXT-ORD-1001";
        Requests.ExternalOrder request = sampleRequest(externalId);

        LogisticsOrder existingOrder = new LogisticsOrder();
        existingOrder.setOrderNumber("ORD-999");
        existingOrder.setStatus(Statuses.OrderStatus.CONFIRMED);

        when(logisticsService.orderByExternalReference(externalId)).thenReturn(Optional.of(existingOrder));

        IntegrationService.Result result = integrationService.submit(request, "REQ-5678");

        assertTrue(result.success());
        assertTrue(result.idempotent(), "Duplicate submission must return idempotent=true");
        assertEquals(externalId, result.externalOrderId());
        assertEquals("ORD-999", result.orderId());
        assertEquals("CONFIRMED", result.status());

        // Verify logisticsService.createOrder was NOT called again
        verify(logisticsService, never()).createOrder(any(), any());

        ArgumentCaptor<IntegrationRequestLog> captor = ArgumentCaptor.forClass(IntegrationRequestLog.class);
        verify(logsRepository).save(captor.capture());
        assertEquals(200, captor.getValue().getHttpStatus());
    }
}
