package com.parkease.model;

import java.time.LocalDateTime;

public class Bike extends Vehicle {

    public Bike(String plateNumber) {
        this(plateNumber, "N/A", LocalDateTime.now());
    }

    public Bike(String plateNumber, String ownerName) {
        this(plateNumber, ownerName, LocalDateTime.now());
    }

    public Bike(String plateNumber, LocalDateTime entryTime) {
        this(plateNumber, "N/A", entryTime);
    }

    public Bike(String plateNumber, String ownerName, LocalDateTime entryTime) {
        super(plateNumber, ownerName, VehicleType.BIKE, entryTime);
    }
}
