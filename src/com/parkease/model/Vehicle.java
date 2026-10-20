package com.parkease.model;

import com.parkease.billing.Chargeable;
import java.time.LocalDateTime;
import java.util.Objects;

public abstract class Vehicle implements Chargeable {
    private final String plateNumber;
    private final String ownerName;
    private final VehicleType vehicleType;
    private final LocalDateTime entryTime;

    public Vehicle(String rawPlateNumber, VehicleType vehicleType, LocalDateTime entryTime) {
        this(rawPlateNumber, "N/A", vehicleType, entryTime);
    }

    public Vehicle(String rawPlateNumber, String ownerName, VehicleType vehicleType, LocalDateTime entryTime) {
        this.plateNumber = validateAndNormalizePlate(rawPlateNumber);
        this.ownerName = (ownerName != null && !ownerName.trim().isEmpty()) ? ownerName.trim() : "N/A";
        this.vehicleType = vehicleType;
        this.entryTime = entryTime != null ? entryTime : LocalDateTime.now();
    }

    public static String validateAndNormalizePlate(String rawPlate) {
        if (rawPlate == null || rawPlate.trim().isEmpty()) {
            throw new IllegalArgumentException("Registration plate cannot be empty.");
        }

        String clean = rawPlate.trim().replace(" ", "").replace("-", "").replace(".", "").toUpperCase();
        if (!clean.matches("^[A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4}$")) {
            throw new IllegalArgumentException("Invalid registration plate: " + rawPlate);
        }

        return clean;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    @Override
    public double getHourlyRate() {
        return vehicleType.getHourlyRate();
    }

    @Override
    public double getDailyCap() {
        return vehicleType.getDailyCap();
    }

    @Override
    public long calculateBilledHours(long durationMinutes) {
        if (durationMinutes <= 0) {
            return 1;
        }
        return (durationMinutes + 59) / 60;
    }

    @Override
    public double calculateFee(long durationMinutes) {
        long totalHours = calculateBilledHours(durationMinutes);
        long fullDays = totalHours / 24;
        long remainderHours = totalHours % 24;

        double fullDaysCost = fullDays * getDailyCap();
        double remainderCost = Math.min(remainderHours * getHourlyRate(), getDailyCap());

        return fullDaysCost + remainderCost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vehicle vehicle = (Vehicle) o;
        return Objects.equals(plateNumber, vehicle.plateNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plateNumber);
    }

    @Override
    public String toString() {
        return plateNumber + " (" + vehicleType.getDisplayName() + ")";
    }
}
