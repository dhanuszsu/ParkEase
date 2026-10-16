package com.parkease.model;

import java.time.LocalDateTime;

/**
 * Concrete subclass representing a Two-Wheeler / Bike.
 * Consumes 1 slot.
 * Hourly rate: Rs. 20/hr, Daily Cap: Rs. 100.
 */
public class Bike extends Vehicle {

    public Bike(String plateNumber) {
        this(plateNumber, LocalDateTime.now());
    }

    public Bike(String plateNumber, LocalDateTime entryTime) {
        super(plateNumber, VehicleType.BIKE, entryTime);
    }
}
