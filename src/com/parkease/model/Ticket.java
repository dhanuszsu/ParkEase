package com.parkease.model;

import com.parkease.util.AnsiColor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
        return allocatedSlots;
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

    public void closeTicket(LocalDateTime exitTime) {
        if (!this.active) {
            throw new IllegalStateException("Ticket " + ticketId + " is already closed.");
        }
        if (exitTime.isBefore(this.entryTime)) {
            throw new IllegalArgumentException("Exit time cannot be before entry time.");
        }

        this.exitTime = exitTime;
        this.durationMinutes = Math.max(0, Duration.between(entryTime, exitTime).toMinutes());
        this.billedHours = vehicle.calculateBilledHours(this.durationMinutes);
        this.feeCharged = vehicle.calculateFee(this.durationMinutes);
        this.active = false;
    }

    public String generateEntryReceipt() {
        String rateInfo = String.format("Rs. %.0f/hr (Max Rs. %.0f/day)",
                vehicle.getHourlyRate(), vehicle.getDailyCap());

        return String.join("\n",
                AnsiColor.orange("+--------------------------------------------------------+"),
                AnsiColor.bold(AnsiColor.orange("|                 PARKEASE ENTRY TICKET                  |")),
                AnsiColor.orange("+--------------------------------------------------------+"),
                AnsiColor.orange("|  Ticket ID     : ") + AnsiColor.bold(AnsiColor.white(String.format("%-37s", ticketId))) + AnsiColor.orange(" |"),
                AnsiColor.orange("|  Plate Number  : ") + AnsiColor.bold(AnsiColor.white(String.format("%-37s", vehicle.getPlateNumber()))) + AnsiColor.orange(" |"),
                AnsiColor.orange("|  Owner Name    : ") + AnsiColor.white(String.format("%-37s", vehicle.getOwnerName())) + AnsiColor.orange(" |"),
                AnsiColor.orange("|  Vehicle Type  : ") + AnsiColor.white(String.format("%-37s", vehicle.getVehicleType().getDisplayName())) + AnsiColor.orange(" |"),
                AnsiColor.orange("|  Allocated     : ") + AnsiColor.bold(AnsiColor.white(String.format("%-37s", getFormattedSlots()))) + AnsiColor.orange(" |"),
                AnsiColor.orange("|  Entry Time    : ") + AnsiColor.white(String.format("%-37s", entryTime.format(TIME_FMT))) + AnsiColor.orange(" |"),
                AnsiColor.orange("|  Rate / Cap    : ") + AnsiColor.white(String.format("%-37s", rateInfo)) + AnsiColor.orange(" |"),
                AnsiColor.orange("+--------------------------------------------------------+")
        );
    }

    public String generateExitReceipt() {
        long days = billedHours / 24;
        long remHours = billedHours % 24;
        String breakdown = days > 0
                ? String.format("%dd @ Rs.%.0f + %dhr @ Rs.%.0f", days, vehicle.getDailyCap(), remHours, vehicle.getHourlyRate())
                : String.format("%d hr(s) x Rs.%.0f = Rs.%.0f (Cap Rs.%.0f)", billedHours, vehicle.getHourlyRate(), billedHours * vehicle.getHourlyRate(), vehicle.getDailyCap());

        String feeFormatted = String.format("Rs. %.2f", feeCharged);

        return String.join("\n",
                AnsiColor.orange("+--------------------------------------------------------+"),
                AnsiColor.bold(AnsiColor.orange("|                 PARKEASE EXIT RECEIPT                  |")),
                AnsiColor.orange("+--------------------------------------------------------+"),
                AnsiColor.orange("|  Ticket ID     : ") + AnsiColor.bold(AnsiColor.white(String.format("%-37s", ticketId))) + AnsiColor.orange(" |"),
                AnsiColor.orange("|  Plate Number  : ") + AnsiColor.bold(AnsiColor.white(String.format("%-37s", vehicle.getPlateNumber()))) + AnsiColor.orange(" |"),
                AnsiColor.orange("|  Owner Name    : ") + AnsiColor.white(String.format("%-37s", vehicle.getOwnerName())) + AnsiColor.orange(" |"),
                AnsiColor.orange("|  Vehicle Type  : ") + AnsiColor.white(String.format("%-37s", vehicle.getVehicleType().getDisplayName())) + AnsiColor.orange(" |"),
                AnsiColor.orange("|  Freed Slots   : ") + AnsiColor.bold(AnsiColor.white(String.format("%-37s", getFormattedSlots()))) + AnsiColor.orange(" |"),
                AnsiColor.orange("|  Entry Time    : ") + AnsiColor.white(String.format("%-37s", entryTime.format(TIME_FMT))) + AnsiColor.orange(" |"),
                AnsiColor.orange("|  Exit Time     : ") + AnsiColor.white(String.format("%-37s", exitTime.format(TIME_FMT))) + AnsiColor.orange(" |"),
                AnsiColor.orange("|  Stay Duration : ") + AnsiColor.white(String.format("%-37s", formatDuration(durationMinutes))) + AnsiColor.orange(" |"),
                AnsiColor.orange("|  Billed Hours  : ") + AnsiColor.white(String.format("%-37s", billedHours + " hour(s) [rounded up]")) + AnsiColor.orange(" |"),
                AnsiColor.orange("|  Billing Rule  : ") + AnsiColor.white(String.format("%-37s", breakdown)) + AnsiColor.orange(" |"),
                AnsiColor.orange("+--------------------------------------------------------+"),
                AnsiColor.bold(AnsiColor.orange("|  TOTAL CHARGED : ")) + AnsiColor.bold(AnsiColor.white(String.format("%-37s", feeFormatted))) + AnsiColor.orange(" |"),
                AnsiColor.orange("+--------------------------------------------------------+")
        );
    }

    private static String formatDuration(long minutes) {
        long hrs = minutes / 60;
        long mins = minutes % 60;
        if (hrs == 0) {
            return mins + " min";
        }
        return String.format("%d hr %d min", hrs, mins);
    }
}
