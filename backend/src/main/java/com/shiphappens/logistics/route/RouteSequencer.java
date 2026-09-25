package com.shiphappens.logistics.route;

import com.shiphappens.logistics.entity.Shipment;
import com.shiphappens.logistics.entity.Vehicle;

import java.util.List;

public interface RouteSequencer {
    List<Shipment> sequence(Vehicle vehicle, List<Shipment> shipments);
}
