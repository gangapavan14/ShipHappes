package com.shiphappens.logistics.config;

import com.shiphappens.logistics.dto.Requests;
import com.shiphappens.logistics.entity.*;
import com.shiphappens.logistics.entity.Statuses.*;
import com.shiphappens.logistics.repository.*;
import com.shiphappens.logistics.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DemoDataConfiguration {

    private static final double DEPOT_LAT = 17.385044;
    private static final double DEPOT_LON = 78.486671;

    @Bean
    @Order(1)
    CommandLineRunner safeDemoData(
            CustomerRepository customers,
            DriverRepository drivers,
            VehicleRepository vehicles,
            WarehouseRepository warehouses,
            LogisticsService service) {
        return args -> {
            if (customers.count() > 0) return;

            // 1. Customer
            Customer customer = service.createCustomer(
                    new Requests.Customer("CUST-DEMO", "Apex Commerce", "ops@demo.invalid", "+91 90000 00000")
            );

            // 2. Warehouse
            Warehouse warehouse = new Warehouse();
            warehouse.setWarehouseCode("WH-HYD-01");
            warehouse.setName("Central Telangana Distribution Center");
            warehouse.setAddress("Industrial Area, Phase 2, Sanath Nagar");
            warehouse.setCity("Hyderabad");
            warehouse.setState("Telangana");
            warehouse.setLatitude(DEPOT_LAT);
            warehouse.setLongitude(DEPOT_LON);
            warehouse.setCapacity(5000);
            warehouse.setCurrentLoad(320);
            warehouse.setStatus("ACTIVE");
            warehouses.save(warehouse);

            // 3. Drivers
            Driver d1 = new Driver();
            d1.setDriverCode("DRV-001");
            d1.setName("Aarav Sharma");
            d1.setPhone("+91 98480 11221");
            d1.setLicenseNumber("TS-09-2023-001");
            d1.setStatus(DriverStatus.AVAILABLE);
            drivers.save(d1);

            Driver d2 = new Driver();
            d2.setDriverCode("DRV-002");
            d2.setName("Priya Verma");
            d2.setPhone("+91 98480 33442");
            d2.setLicenseNumber("TS-09-2023-002");
            d2.setStatus(DriverStatus.AVAILABLE);
            drivers.save(d2);

            // 4. Vehicles with starting coordinates & capacities
            Vehicle v1 = new Vehicle();
            v1.setVehicleNumber("TS-09-EV-1001");
            v1.setVehicleType("Electric Van (Medium)");
            v1.setCapacityKg(new BigDecimal("1000.00"));
            v1.setStatus(VehicleStatus.AVAILABLE);
            v1.setStartLatitude(DEPOT_LAT);
            v1.setStartLongitude(DEPOT_LON);
            vehicles.save(v1);

            Vehicle v2 = new Vehicle();
            v2.setVehicleNumber("TS-09-TR-2002");
            v2.setVehicleType("City Freight Carrier");
            v2.setCapacityKg(new BigDecimal("500.00"));
            v2.setStatus(VehicleStatus.AVAILABLE);
            v2.setStartLatitude(DEPOT_LAT);
            v2.setStartLongitude(DEPOT_LON);
            vehicles.save(v2);

            // 5. In-transit sample shipment
            Shipment inTransitShipment = service.createOrder(
                    new Requests.CreateOrder(
                            customer.getId(),
                            new Requests.Address("100 Demo Warehouse Road", null, "Hyderabad", "Telangana", "500001", "India"),
                            new Requests.Address("42 Commercial Street", null, "Bengaluru", "Karnataka", "560100", "India"),
                            List.of(new Requests.Package("Demo electronics batch", new BigDecimal("45.00"), new BigDecimal("30"), new BigDecimal("20"), new BigDecimal("15"), new BigDecimal("12000")))
                    ),
                    "DEMO-EXT-1001"
            );
            inTransitShipment.setDestinationLatitude(12.971599);
            inTransitShipment.setDestinationLongitude(77.594566);
            inTransitShipment.setWeightKg(new BigDecimal("45.00"));
            service.updateShipment(inTransitShipment.getId(), new Requests.ShipmentStatus(ShipmentStatus.PICKED_UP, "Hyderabad", "Demo pickup completed"));
            service.updateShipment(inTransitShipment.getId(), new Requests.ShipmentStatus(ShipmentStatus.IN_TRANSIT, "NH 44", "Demo shipment in transit"));

            // 6. Seed multiple CREATED shipments ready for Route Planning & Optimization
            // Stop A: Banjara Hills (approx ~5.5 km from depot)
            Shipment s1 = service.createOrder(
                    new Requests.CreateOrder(
                            customer.getId(),
                            new Requests.Address("Road No. 1, Banjara Hills", null, "Hyderabad", "Telangana", "500034", "India"),
                            new Requests.Address("Road No. 12, Banjara Hills", null, "Hyderabad", "Telangana", "500034", "India"),
                            List.of(new Requests.Package("Precision Diagnostics Equipment", new BigDecimal("180.00"), new BigDecimal("40"), new BigDecimal("30"), new BigDecimal("25"), new BigDecimal("55000")))
                    ),
                    "DEMO-EXT-2001"
            );
            s1.setDestinationLatitude(17.415563);
            s1.setDestinationLongitude(78.435776);
            s1.setWeightKg(new BigDecimal("180.00"));
            s1.setDestinationAddress("Road No. 12, Banjara Hills, Hyderabad 500034");

            // Stop B: Hitec City (approx ~12 km from depot)
            Shipment s2 = service.createOrder(
                    new Requests.CreateOrder(
                            customer.getId(),
                            new Requests.Address("Cyber Gateway, Madhapur", null, "Hyderabad", "Telangana", "500081", "India"),
                            new Requests.Address("Knowledge City, Hitec City", null, "Hyderabad", "Telangana", "500081", "India"),
                            List.of(new Requests.Package("Server Hardware & Modules", new BigDecimal("320.00"), new BigDecimal("60"), new BigDecimal("40"), new BigDecimal("35"), new BigDecimal("98000")))
                    ),
                    "DEMO-EXT-2002"
            );
            s2.setDestinationLatitude(17.447413);
            s2.setDestinationLongitude(78.376230);
            s2.setWeightKg(new BigDecimal("320.00"));
            s2.setDestinationAddress("Knowledge City, Hitec City, Hyderabad 500081");

            // Stop C: Gachibowli Financial District (approx ~16 km from depot)
            Shipment s3 = service.createOrder(
                    new Requests.CreateOrder(
                            customer.getId(),
                            new Requests.Address("ISB Road, Gachibowli", null, "Hyderabad", "Telangana", "500032", "India"),
                            new Requests.Address("Financial District, Nanakramguda", null, "Hyderabad", "Telangana", "500032", "India"),
                            List.of(new Requests.Package("Solar Micro-inverters", new BigDecimal("220.00"), new BigDecimal("50"), new BigDecimal("45"), new BigDecimal("30"), new BigDecimal("42000")))
                    ),
                    "DEMO-EXT-2003"
            );
            s3.setDestinationLatitude(17.419088);
            s3.setDestinationLongitude(78.342896);
            s3.setWeightKg(new BigDecimal("220.00"));
            s3.setDestinationAddress("Financial District, Nanakramguda, Hyderabad 500032");

            // Stop D: Secunderabad Hub (approx ~9 km from depot in different direction)
            Shipment s4 = service.createOrder(
                    new Requests.CreateOrder(
                            customer.getId(),
                            new Requests.Address("Station Road, Secunderabad", null, "Hyderabad", "Telangana", "500003", "India"),
                            new Requests.Address("Paradise Circle, Secunderabad", null, "Hyderabad", "Telangana", "500003", "India"),
                            List.of(new Requests.Package("Industrial Pump Spares", new BigDecimal("140.00"), new BigDecimal("35"), new BigDecimal("35"), new BigDecimal("25"), new BigDecimal("18500")))
                    ),
                    "DEMO-EXT-2004"
            );
            s4.setDestinationLatitude(17.439930);
            s4.setDestinationLongitude(78.498274);
            s4.setWeightKg(new BigDecimal("140.00"));
            s4.setDestinationAddress("Paradise Circle, Secunderabad 500003");
        };
    }

    @Bean
    @Order(2)
    CommandLineRunner optionalDemoAdmin(
            AuthService auth,
            AppUserRepository users,
            PasswordEncoder passwords,
            @Value("${app.security.demo-password:}") String password) {
        return args -> {
            if (!password.isBlank()) {
                var existing = users.findByEmail("demo@shiphappens.app");
                if (existing.isEmpty()) {
                    auth.register("demo@shiphappens.app", password, UserRole.ADMIN);
                } else {
                    AppUser u = existing.get();
                    u.setPasswordHash(passwords.encode(password));
                    users.save(u);
                }
            }
        };
    }
}
