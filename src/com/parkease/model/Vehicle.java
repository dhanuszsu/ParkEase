package com.parkease.model;

import com.parkease.billing.Chargeable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Abstract parent class representing a vehicle in the ParkEase system.
 * Implements the Chargeable interface for vehicle-specific billing,
 * partial-hour rounding, and daily charge caps.
 */
public abstract class Vehicle implements Chargeable {
    // Standard registration format: e.g. MH12AB1234, DL01C9999, KA05MB9876
    private static final Pattern PLATE_PATTERN = Pattern.compile("^[A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4}$");

    private final String plateNumber;
    private final VehicleType vehicleType;
    private final LocalDateTime entryTime;

    /**
     * Constructs and validates a Vehicle instance.
     *
     * @param rawPlateNumber Vehicle registration plate (raw user input).
     * @param vehicleType    VehicleType enum value.
     * @param entryTime      Timestamp of entry.
     */
    public Vehicle(String rawPlateNumber, VehicleType vehicleType, LocalDateTime entryTime) {
        this.plateNumber = validateAndNormalizePlate(rawPlateNumber);
        this.vehicleType = Objects.requireNonNull(vehicleType, "Vehicle type cannot be null");
        this.entryTime = entryTime != null ? entryTime : LocalDateTime.now();
    }

    /**
     * Validates and normalizes registration plate number.
     * Strips spaces and hyphens, converts to uppercase, and verifies against standard syntax.
     *
     * @param rawPlate Raw input string.
     * @return Clean uppercase registration plate string.
     */
    public static String validateAndNormalizePlate(String rawPlate) {
        if (rawPlate == null || rawPlate.trim().isEmpty()) {
            throw new IllegalArgumentException("Registration plate cannot be empty or null.");
        }

        // Strip hyphens, dots, and whitespace
        String normalized = rawPlate.replaceAll("[\\s\\-\\.]", "").toUpperCase();

        if (!PLATE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(String.format(
                    "Invalid registration plate '%s'. Must match standard format (e.g. MH-12-AB-1234, DL 01 C 9999, KA05MB9876).",
                    rawPlate));
        }

        return normalized;
    }

    public String getPlateNumber() {
        return plateNumber;
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
            return 1; // Minimum billable unit is 1 hour
        }
        // Round up any partial hour to the nearest whole hour (e.g., 61 mins -> 2 hrs)
        return (durationMinutes + 59) / 60;
    }

    @Override
    public double calculateFee(long durationMinutes) {
        long totalHours = calculateBilledHours(durationMinutes);

        // Daily cap calculation: 24-hour day cycle
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
        return String.format("%s [%s, Plate: %s, In: %s]",
                vehicleType.getDisplayName(),
                getClass().getSimpleName(),
                plateNumber,
                entryTime);
    }
}
