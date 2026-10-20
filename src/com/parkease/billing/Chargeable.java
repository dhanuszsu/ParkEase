package com.parkease.billing;

public interface Chargeable {
    double calculateFee(long durationMinutes);

    default double calculateCharge(int hours) {
        return calculateFee((long) Math.max(1, hours) * 60);
    }

    double getHourlyRate();
    double getDailyCap();
    long calculateBilledHours(long durationMinutes);
}
