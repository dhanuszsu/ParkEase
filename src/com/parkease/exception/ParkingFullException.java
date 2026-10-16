package com.parkease.exception;

import com.parkease.model.VehicleType;

/**
 * Custom checked exception thrown when a vehicle cannot be accommodated
 * in the parking lot due to lack of available slots or failure to find
 * consecutive slots (e.g. for trucks).
 */
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
