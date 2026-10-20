package com.parkease.exception;

import com.parkease.model.VehicleType;

public class ParkingFullException extends Exception {
    private final VehicleType vehicleType;
    private final int requiredSlots;

    public ParkingFullException(String message) {
        super(message);
        this.vehicleType = null;
        this.requiredSlots = 0;
    }

    public ParkingFullException(VehicleType vehicleType, String message) {
        super(message);
        this.vehicleType = vehicleType;
        this.requiredSlots = vehicleType != null ? vehicleType.getRequiredSlots() : 0;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public int getRequiredSlots() {
        return requiredSlots;
    }
}
