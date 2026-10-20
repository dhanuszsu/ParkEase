package com.parkease.model;

import java.time.LocalDateTime;

public class Truck extends Vehicle {

    public Truck(String plateNumber) {
        this(plateNumber, "N/A", LocalDateTime.now());
    }

    public Truck(String plateNumber, String ownerName) {
        this(plateNumber, ownerName, LocalDateTime.now());
    }

    public Truck(String plateNumber, LocalDateTime entryTime) {
        this(plateNumber, "N/A", entryTime);
    }

    public Truck(String plateNumber, String ownerName, LocalDateTime entryTime) {
        super(plateNumber, ownerName, VehicleType.TRUCK, entryTime);
    }
}
