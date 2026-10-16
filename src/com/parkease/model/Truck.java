package com.parkease.model;

import java.time.LocalDateTime;

/**
 * Concrete subclass representing a Heavy Vehicle / Truck.
 * Consumes 2 consecutive slots.
 * Hourly rate: Rs. 100/hr, Daily Cap: Rs. 600.
 */
public class Truck extends Vehicle {

    public Truck(String plateNumber) {
        this(plateNumber, LocalDateTime.now());
    }

    public Truck(String plateNumber, LocalDateTime entryTime) {
        super(plateNumber, VehicleType.TRUCK, entryTime);
    }
}
