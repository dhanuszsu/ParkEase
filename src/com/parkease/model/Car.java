package com.parkease.model;

import java.time.LocalDateTime;

/**
 * Concrete subclass representing a Four-Wheeler / Car.
 * Consumes 1 slot.
 * Hourly rate: Rs. 40/hr, Daily Cap: Rs. 250.
 */
public class Car extends Vehicle {

    public Car(String plateNumber) {
        this(plateNumber, LocalDateTime.now());
    }

    public Car(String plateNumber, LocalDateTime entryTime) {
        super(plateNumber, VehicleType.CAR, entryTime);
    }
}
