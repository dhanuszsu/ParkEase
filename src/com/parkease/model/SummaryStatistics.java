package com.parkease.model;

import java.util.Map;

public class SummaryStatistics {
    private final int totalSlots;
    private final int occupiedSlots;
    private final int vacantSlots;
    private final int parkedBikes;
    private final int parkedCars;
    private final int parkedTrucks;
    private final int totalParkedVehicles;
    private final int totalTicketsIssued;
    private final int completedExits;
    private final double totalRevenueCollected;
    private final Map<VehicleType, Double> revenueByType;

    public SummaryStatistics(int totalSlots, int occupiedSlots, int vacantSlots,
                             int parkedBikes, int parkedCars, int parkedTrucks,
                             int totalTicketsIssued, int completedExits,
                             double totalRevenueCollected, Map<VehicleType, Double> revenueByType) {
        this.totalSlots = totalSlots;
        this.occupiedSlots = occupiedSlots;
        this.vacantSlots = vacantSlots;
        this.parkedBikes = parkedBikes;
        this.parkedCars = parkedCars;
        this.parkedTrucks = parkedTrucks;
        this.totalParkedVehicles = parkedBikes + parkedCars + parkedTrucks;
        this.totalTicketsIssued = totalTicketsIssued;
        this.completedExits = completedExits;
        this.totalRevenueCollected = totalRevenueCollected;
        this.revenueByType = revenueByType;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public int getOccupiedSlots() {
        return occupiedSlots;
    }

    public int getVacantSlots() {
        return vacantSlots;
    }

    public int getParkedBikes() {
        return parkedBikes;
    }

    public int getParkedCars() {
        return parkedCars;
    }

    public int getParkedTrucks() {
        return parkedTrucks;
    }

    public int getTotalParkedVehicles() {
        return totalParkedVehicles;
    }

    public int getTotalTicketsIssued() {
        return totalTicketsIssued;
    }

    public int getCompletedExits() {
        return completedExits;
    }

    public double getTotalRevenueCollected() {
        return totalRevenueCollected;
    }

    public double getRevenue(VehicleType type) {
        return revenueByType.getOrDefault(type, 0.0);
    }

    public double getOccupancyRatePercentage() {
        return totalSlots > 0 ? ((double) occupiedSlots / totalSlots) * 100.0 : 0.0;
    }
}
