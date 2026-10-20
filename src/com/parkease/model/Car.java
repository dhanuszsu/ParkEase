package com.parkease.model;

import java.time.LocalDateTime;

public class Car extends Vehicle {

    public Car(String plateNumber) {
        this(plateNumber, "N/A", LocalDateTime.now());
    }

    public Car(String plateNumber, String ownerName) {
        this(plateNumber, ownerName, LocalDateTime.now());
    }

    public Car(String plateNumber, LocalDateTime entryTime) {
        this(plateNumber, "N/A", entryTime);
    }

    public Car(String plateNumber, String ownerName, LocalDateTime entryTime) {
        super(plateNumber, ownerName, VehicleType.CAR, entryTime);
    }
}
