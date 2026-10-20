package com.parkease.model;

public enum VehicleType {
    BIKE("Bike", 1, 10.0, 80.0),
    CAR("Car", 1, 30.0, 240.0),
    TRUCK("Truck", 2, 60.0, 480.0);

    private final String displayName;
    private final int requiredSlots;
    private final double hourlyRate;
    private final double dailyCap;

    VehicleType(String displayName, int requiredSlots, double hourlyRate, double dailyCap) {
        this.displayName = displayName;
        this.requiredSlots = requiredSlots;
        this.hourlyRate = hourlyRate;
        this.dailyCap = dailyCap;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getRequiredSlots() {
        return requiredSlots;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public double getDailyCap() {
        return dailyCap;
    }
}
