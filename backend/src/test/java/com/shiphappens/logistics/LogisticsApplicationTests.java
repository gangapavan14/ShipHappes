package com.shiphappens.logistics;

import com.shiphappens.logistics.dto.Requests;
import com.shiphappens.logistics.dto.Responses.CustomerResponse;
import com.shiphappens.logistics.dto.Responses.DashboardResponse;
import com.shiphappens.logistics.entity.Statuses.UserRole;
import com.shiphappens.logistics.mapper.EntityMapper;
import com.shiphappens.logistics.service.AuthService;
import com.shiphappens.logistics.service.LogisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LogisticsApplicationTests {

    @Autowired
    private LogisticsService logisticsService;

    @Autowired
    private AuthService authService;

    @Autowired
    private EntityMapper mapper;

    @Test
    void contextLoads() {
        assertNotNull(logisticsService);
        assertNotNull(authService);
        assertNotNull(mapper);
    }

    @Test
    void testCustomerCreationAndMapping() {
        var req = new Requests.Customer("TEST-CUST-01", "Test Corp", "test@corp.com", "+1 555-0100");
        var customer = logisticsService.createCustomer(req);
        assertNotNull(customer.getId());

        CustomerResponse res = mapper.toCustomerResponse(customer);
        assertEquals("TEST-CUST-01", res.customerCode());
        assertEquals("Test Corp", res.name());
        assertEquals("ACTIVE", res.status());
    }

    @Test
    void testAuthRegistrationAndLogin() {
        var user = authService.register("admin@test.app", "ShipHappens@2026!", UserRole.ADMIN);
        assertNotNull(user.getId());

        var loginResult = authService.login("admin@test.app", "ShipHappens@2026!");
        assertNotNull(loginResult.accessToken());
        assertEquals("ADMIN", loginResult.role());
    }

    @Test
    void testOrderCreationAndPackageMapping() {
        var customer = logisticsService.createCustomer(new Requests.Customer("ORDER-CUST-01", "Acme", "acme@test.com", "+1 555-0200"));
        var orderReq = new Requests.CreateOrder(
                customer.getId(),
                new Requests.Address("123 Start St", null, "Metropolis", "NY", "10001", "USA"),
                new Requests.Address("456 End Ave", null, "Gotham", "NJ", "07001", "USA"),
                List.of(new Requests.Package("Box 1", new BigDecimal("3.5"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("100")))
        );

        var shipment = logisticsService.createOrder(orderReq, "EXT-REF-1");
        assertNotNull(shipment.getId());
        assertNotNull(shipment.getOrder());

        var orderRes = mapper.toOrderResponse(shipment.getOrder(), logisticsService.getOrderPackages(shipment.getOrder().getId()));
        assertEquals(1, orderRes.packages().size());
        assertEquals("Box 1", orderRes.packages().get(0).description());
    }
}
