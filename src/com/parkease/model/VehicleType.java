package com.parkease.model;

/**
 * Enumeration representing supported vehicle types in ParkEase.
 * Holds business rules for slot requirements, hourly billing rates, and daily caps.
 */
public enum VehicleType {
    BIKE("Bike", 1, 20.0, 100.0),
    CAR("Car", 1, 40.0, 250.0),
    TRUCK("Truck", 2, 100.0, 600.0);

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

    @Override
    public String toString() {
        return String.format("%s [Slots: %d, Rate: Rs. %.2f/hr, Daily Cap: Rs. %.2f]",
                displayName, requiredSlots, hourlyRate, dailyCap);
    }
}
