package com.parkease.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an entry/exit parking ticket in the ParkEase system.
 */
public class Ticket {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String ticketId;
    private final Vehicle vehicle;
    private final List<Integer> allocatedSlots;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private long durationMinutes;
    private long billedHours;
    private double feeCharged;
    private boolean active;

    public Ticket(String ticketId, Vehicle vehicle, List<Integer> allocatedSlots, LocalDateTime entryTime) {
        this.ticketId = ticketId;
        this.vehicle = vehicle;
        this.allocatedSlots = new ArrayList<>(allocatedSlots);
        this.entryTime = entryTime != null ? entryTime : LocalDateTime.now();
        this.active = true;
    }

    public String getTicketId() {
        return ticketId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public List<Integer> getAllocatedSlots() {
        return Collections.unmodifiableList(allocatedSlots);
    }

    public String getFormattedSlots() {
        if (allocatedSlots.size() == 1) {
            return String.format("S-%02d", allocatedSlots.get(0));
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < allocatedSlots.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(String.format("S-%02d", allocatedSlots.get(i)));
            }
            return sb.toString();
        }
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public long getDurationMinutes() {
        return durationMinutes;
    }

    public long getBilledHours() {
        return billedHours;
    }

    public double getFeeCharged() {
        return feeCharged;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Closes the ticket upon vehicle departure and calculates the fee.
     *
     * @param exitTime Departure time.
     */
    public void closeTicket(LocalDateTime exitTime) {
        if (!this.active) {
            throw new IllegalStateException("Ticket " + ticketId + " has already been closed.");
        }
        if (exitTime.isBefore(this.entryTime)) {
            throw new IllegalArgumentException("Exit time (" + exitTime.format(TIME_FMT) +
                    ") cannot be before entry time (" + this.entryTime.format(TIME_FMT) + ").");
        }

        this.exitTime = exitTime;
        this.durationMinutes = Math.max(0, Duration.between(entryTime, exitTime).toMinutes());
        this.billedHours = vehicle.calculateBilledHours(this.durationMinutes);
        this.feeCharged = vehicle.calculateFee(this.durationMinutes);
        this.active = false;
    }

    /**
     * Generates a printable entry receipt using clean ASCII borders.
     */
    public String generateEntryReceipt() {
        String rateInfo = String.format("Rs. %.0f/hr (Max Rs. %.0f/day)",
                vehicle.getHourlyRate(), vehicle.getDailyCap());

        return String.join("\n",
                "+--------------------------------------------------------+",
                "|                 PARKEASE ENTRY TICKET                  |",
                "+--------------------------------------------------------+",
                String.format("|  %-14s: %-37s |", "Ticket ID", ticketId),
                String.format("|  %-14s: %-37s |", "Plate Number", vehicle.getPlateNumber()),
                String.format("|  %-14s: %-37s |", "Vehicle Type", vehicle.getVehicleType().getDisplayName()),
                String.format("|  %-14s: %-37s |", "Allocated", getFormattedSlots()),
                String.format("|  %-14s: %-37s |", "Entry Time", entryTime.format(TIME_FMT)),
                String.format("|  %-14s: %-37s |", "Rate / Cap", rateInfo),
                "|                                                        |",
                "|  * Please retain this ticket for departure.            |",
                "|  * Partial hours are rounded up to the full hour.      |",
                "+--------------------------------------------------------+"
        );
    }

    /**
     * Generates a printable departure receipt with detailed billing breakdown using clean ASCII borders.
     */
    public String generateExitReceipt() {
        long days = billedHours / 24;
        long remHours = billedHours % 24;
        String breakdown = days > 0
                ? String.format("%dd @ Rs.%.0f + %dhr @ Rs.%.0f", days, vehicle.getDailyCap(), remHours, vehicle.getHourlyRate())
                : String.format("%d hr(s) @ Rs.%.0f/hr (Cap: Rs.%.0f)", billedHours, vehicle.getHourlyRate(), vehicle.getDailyCap());

        String feeFormatted = String.format("Rs. %.2f", feeCharged);

        return String.join("\n",
                "+--------------------------------------------------------+",
                "|                 PARKEASE EXIT RECEIPT                  |",
                "+--------------------------------------------------------+",
                String.format("|  %-14s: %-37s |", "Ticket ID", ticketId),
                String.format("|  %-14s: %-37s |", "Plate Number", vehicle.getPlateNumber()),
                String.format("|  %-14s: %-37s |", "Vehicle Type", vehicle.getVehicleType().getDisplayName()),
                String.format("|  %-14s: %-37s |", "Freed Slots", getFormattedSlots()),
                String.format("|  %-14s: %-37s |", "Entry Time", entryTime.format(TIME_FMT)),
                String.format("|  %-14s: %-37s |", "Exit Time", exitTime.format(TIME_FMT)),
                String.format("|  %-14s: %-37s |", "Stay Duration", formatDuration(durationMinutes)),
                String.format("|  %-14s: %-37s |", "Billed Hours", billedHours + " hour(s) [rounded up]"),
                String.format("|  %-14s: %-37s |", "Billing Rule", breakdown),
                "+--------------------------------------------------------+",
                String.format("|  %-14s: %-37s |", "TOTAL CHARGED", feeFormatted),
                "|  PAYMENT STATUS: PAID (Thank you for parking!)         |",
                "+--------------------------------------------------------+"
        );
    }

    private static String formatDuration(long minutes) {
        long hrs = minutes / 60;
        long mins = minutes % 60;
        if (hrs == 0) {
            return mins + " minute(s)";
        }
        return String.format("%d hour(s) %d minute(s)", hrs, mins);
    }
}
