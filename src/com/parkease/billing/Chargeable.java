package com.parkease.billing;

/**
 * Interface defining billing behavior for any chargeable vehicle in ParkEase.
 */
public interface Chargeable {
    /**
     * Calculates the parking fee based on the duration in minutes.
     * Partial hours are rounded up to the nearest whole hour,
     * and daily charges are capped at the vehicle's daily limit per 24-hour cycle.
     *
     * @param durationMinutes Total elapsed parking duration in minutes.
     * @return Total parking fee calculated in Currency (Rs.).
     */
    double calculateFee(long durationMinutes);

    /**
     * Returns the hourly rate for this vehicle.
     */
    double getHourlyRate();

    /**
     * Returns the 24-hour daily maximum charge cap for this vehicle.
     */
    double getDailyCap();

    /**
     * Returns the total billed hours after applying partial-hour rounding up.
     * e.g., 1 min - 60 min -> 1 hr; 61 min - 120 min -> 2 hrs.
     */
    long calculateBilledHours(long durationMinutes);
}
