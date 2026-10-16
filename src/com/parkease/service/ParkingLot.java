package com.parkease.service;

import com.parkease.exception.ParkingFullException;
import com.parkease.model.*;
import com.parkease.util.AnsiColor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core service managing slots, vehicle entry, slot allocation,
 * duplicate prevention, exit billing, and live lot map rendering.
 */
public class ParkingLot {
    public static final int DEFAULT_TOTAL_SLOTS = 20;

    private final int totalSlots;
    private final List<ParkingSlot> slots;
    private final Map<String, Ticket> activeTicketsById;
    private final Map<String, Ticket> activeTicketsByPlate;
    private final List<Ticket> completedTickets;
    private final Map<VehicleType, Double> revenueByType;
    private final AtomicInteger ticketCounter;
    private double totalRevenueCollected;

    public ParkingLot() {
        this(DEFAULT_TOTAL_SLOTS);
    }

    public ParkingLot(int totalSlots) {
        if (totalSlots < 2) {
            throw new IllegalArgumentException("Total slots must be at least 2 to support vehicles and trucks.");
        }
        this.totalSlots = totalSlots;
        this.slots = new ArrayList<>(totalSlots);
        for (int i = 1; i <= totalSlots; i++) {
            this.slots.add(new ParkingSlot(i));
        }
        this.activeTicketsById = new LinkedHashMap<>();
        this.activeTicketsByPlate = new HashMap<>();
        this.completedTickets = new ArrayList<>();
        this.revenueByType = new EnumMap<>(VehicleType.class);
        for (VehicleType vt : VehicleType.values()) {
            revenueByType.put(vt, 0.0);
        }
        this.ticketCounter = new AtomicInteger(1001);
        this.totalRevenueCollected = 0.0;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public List<ParkingSlot> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    public int getOccupiedSlotsCount() {
        int count = 0;
        for (ParkingSlot slot : slots) {
            if (slot.isOccupied()) count++;
        }
        return count;
    }

    public int getVacantSlotsCount() {
        return totalSlots - getOccupiedSlotsCount();
    }

    public Collection<Ticket> getActiveTickets() {
        return Collections.unmodifiableCollection(activeTicketsById.values());
    }

    public List<Ticket> getCompletedTickets() {
        return Collections.unmodifiableList(completedTickets);
    }

    /**
     * Parks a vehicle into the lot:
     * 1. Validates duplicate entry.
     * 2. Finds correct slots (1 slot for Bike/Car, 2 consecutive slots for Truck).
     * 3. Throws ParkingFullException if unavailable.
     * 4. Allocates slots, generates ticket, and tracks active status.
     *
     * @param vehicle Vehicle requesting entry.
     * @return Generated active Ticket.
     * @throws ParkingFullException If suitable slots are not available.
     * @throws IllegalArgumentException If duplicate entry or null vehicle.
     */
    public synchronized Ticket parkVehicle(Vehicle vehicle) throws ParkingFullException {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle cannot be null.");
        }

        String plate = vehicle.getPlateNumber();

        // 1. Duplicate-entry prevention
        if (activeTicketsByPlate.containsKey(plate)) {
            Ticket existing = activeTicketsByPlate.get(plate);
            throw new IllegalArgumentException(String.format(
                    "Duplicate entry rejected! Vehicle with plate '%s' is already parked in slot(s) %s (Ticket: %s).",
                    plate, existing.getFormattedSlots(), existing.getTicketId()));
        }

        VehicleType type = vehicle.getVehicleType();
        List<Integer> allocatedSlotIndices = new ArrayList<>();

        // 2. Slot allocation logic
        if (type == VehicleType.TRUCK) {
            // Trucks require TWO CONSECUTIVE vacant slots
            int foundIndex = -1;
            for (int i = 0; i < totalSlots - 1; i++) {
                if (slots.get(i).isVacant() && slots.get(i + 1).isVacant()) {
                    foundIndex = i;
                    break;
                }
            }

            if (foundIndex == -1) {
                int vacantCount = getVacantSlotsCount();
                if (vacantCount == 0) {
                    throw new ParkingFullException(VehicleType.TRUCK,
                            "Parking full: Lot is at 100% capacity (0 slots free).");
                } else {
                    throw new ParkingFullException(VehicleType.TRUCK, String.format(
                            "Parking full for Truck: Requires 2 CONSECUTIVE slots, but only %d isolated non-adjacent slot(s) are free.",
                            vacantCount));
                }
            }

            allocatedSlotIndices.add(foundIndex);
            allocatedSlotIndices.add(foundIndex + 1);
        } else {
            // Bike and Car require 1 slot
            int foundIndex = -1;
            for (int i = 0; i < totalSlots; i++) {
                if (slots.get(i).isVacant()) {
                    foundIndex = i;
                    break;
                }
            }

            if (foundIndex == -1) {
                throw new ParkingFullException(type,
                        "Parking full: No vacant slots available for " + type.getDisplayName() + ".");
            }

            allocatedSlotIndices.add(foundIndex);
        }

        // 3. Generate Ticket ID and allocate
        String ticketId = "TKT-" + ticketCounter.getAndIncrement();
        List<Integer> slotNumbers = new ArrayList<>();

        if (type == VehicleType.TRUCK) {
            int slot1Num = slots.get(allocatedSlotIndices.get(0)).getSlotNumber();
            int slot2Num = slots.get(allocatedSlotIndices.get(1)).getSlotNumber();
            slotNumbers.add(slot1Num);
            slotNumbers.add(slot2Num);

            slots.get(allocatedSlotIndices.get(0)).occupy(vehicle, ticketId, true, slot2Num);
            slots.get(allocatedSlotIndices.get(1)).occupy(vehicle, ticketId, true, slot1Num);
        } else {
            int slotNum = slots.get(allocatedSlotIndices.get(0)).getSlotNumber();
            slotNumbers.add(slotNum);
            slots.get(allocatedSlotIndices.get(0)).occupy(vehicle, ticketId, false, 0);
        }

        Ticket ticket = new Ticket(ticketId, vehicle, slotNumbers, vehicle.getEntryTime());
        activeTicketsById.put(ticketId, ticket);
        activeTicketsByPlate.put(plate, ticket);

        return ticket;
    }

    /**
     * Exits a parked vehicle using either Ticket ID or Registration Plate Number.
     * Computes parking fee with partial hour rounded up and daily cap applied.
     * Frees occupied slots.
     *
     * @param identifier Ticket ID (e.g. TKT-1001) or Plate Number (e.g. MH12AB1234).
     * @param exitTime   Exit timestamp.
     * @return Closed ticket with payment breakdown.
     */
    public synchronized Ticket exitVehicle(String identifier, LocalDateTime exitTime) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifier (Ticket ID or Plate Number) cannot be empty.");
        }

        String searchKey = identifier.trim().toUpperCase();
        Ticket ticket = activeTicketsById.get(searchKey);

        if (ticket == null) {
            // Try matching normalized plate number
            try {
                String normalizedPlate = Vehicle.validateAndNormalizePlate(identifier);
                ticket = activeTicketsByPlate.get(normalizedPlate);
            } catch (Exception ignored) {
                // If not standard plate format, look up raw uppercase
                ticket = activeTicketsByPlate.get(searchKey);
            }
        }

        if (ticket == null) {
            throw new IllegalArgumentException(String.format(
                    "No active parked vehicle found matching '%s'. Please verify Ticket ID or Plate Number.",
                    identifier));
        }

        // Close ticket and compute billing
        ticket.closeTicket(exitTime);

        // Free slots
        for (int slotNum : ticket.getAllocatedSlots()) {
            slots.get(slotNum - 1).vacate();
        }

        // Remove from active maps
        activeTicketsById.remove(ticket.getTicketId());
        activeTicketsByPlate.remove(ticket.getVehicle().getPlateNumber());

        // Update historical tracking and revenue
        completedTickets.add(ticket);
        totalRevenueCollected += ticket.getFeeCharged();
        revenueByType.merge(ticket.getVehicle().getVehicleType(), ticket.getFeeCharged(), Double::sum);

        return ticket;
    }

    /**
     * Looks up an active ticket by Ticket ID or Plate Number.
     */
    public synchronized Optional<Ticket> findActiveTicket(String identifier) {
        if (identifier == null) return Optional.empty();
        String searchKey = identifier.trim().toUpperCase();
        if (activeTicketsById.containsKey(searchKey)) {
            return Optional.of(activeTicketsById.get(searchKey));
        }
        try {
            String normalizedPlate = Vehicle.validateAndNormalizePlate(identifier);
            if (activeTicketsByPlate.containsKey(normalizedPlate)) {
                return Optional.of(activeTicketsByPlate.get(normalizedPlate));
            }
        } catch (Exception ignored) {}
        return Optional.ofNullable(activeTicketsByPlate.get(searchKey));
    }

    /**
     * Generates a live summary snapshot of lot metrics.
     */
    public synchronized SummaryStatistics getSummary() {
        int bikeCount = 0;
        int carCount = 0;
        int truckCount = 0;

        for (Ticket t : activeTicketsById.values()) {
            switch (t.getVehicle().getVehicleType()) {
                case BIKE -> bikeCount++;
                case CAR -> carCount++;
                case TRUCK -> truckCount++;
            }
        }

        return new SummaryStatistics(
                totalSlots,
                getOccupiedSlotsCount(),
                getVacantSlotsCount(),
                bikeCount,
                carCount,
                truckCount,
                ticketCounter.get() - 1001,
                completedTickets.size(),
                totalRevenueCollected,
                new EnumMap<>(revenueByType)
        );
    }

    /**
     * Renders a clean ASCII visual slot map of the parking lot with consistent cell widths.
     */
    public synchronized String renderLiveSlotMap() {
        StringBuilder sb = new StringBuilder();
        String headerBorder = "+" + "-".repeat(93) + "+\n";
        sb.append(AnsiColor.cyan(headerBorder));
        sb.append(AnsiColor.bold(AnsiColor.cyan(String.format("|%32sPARKEASE LIVE PARKING LOT MAP%32s|\n", "", ""))));
        sb.append(AnsiColor.cyan(headerBorder));

        int cols = 4; // 4 slots per row for clean terminal presentation
        int rows = (int) Math.ceil((double) totalSlots / cols);

        for (int r = 0; r < rows; r++) {
            // Top border of row
            for (int c = 0; c < cols; c++) {
                int index = r * cols + c;
                if (index < totalSlots) {
                    sb.append("+---------------------+");
                    if (c < cols - 1 && index + 1 < totalSlots) sb.append(" ");
                }
            }
            sb.append("\n");

            // Line 1: Slot Number & Status Tag
            for (int c = 0; c < cols; c++) {
                int index = r * cols + c;
                if (index < totalSlots) {
                    ParkingSlot slot = slots.get(index);
                    String slotId = slot.getFormattedSlotId();
                    if (slot.isVacant()) {
                        String inner = String.format(" %-4s  %-14s", slotId, "[VACANT]");
                        sb.append("|").append(AnsiColor.green(inner)).append("|");
                    } else {
                        VehicleType vt = slot.getCurrentVehicle().getVehicleType();
                        String tag = "[" + vt.name() + "]";
                        String inner = String.format(" %-4s  %-14s", slotId, tag);
                        String colored = switch (vt) {
                            case BIKE -> AnsiColor.yellow(inner);
                            case CAR -> AnsiColor.blue(inner);
                            case TRUCK -> AnsiColor.magenta(inner);
                        };
                        sb.append("|").append(colored).append("|");
                    }
                    if (c < cols - 1 && index + 1 < totalSlots) sb.append(" ");
                }
            }
            sb.append("\n");

            // Line 2: Plate Number or vacancy placeholder
            for (int c = 0; c < cols; c++) {
                int index = r * cols + c;
                if (index < totalSlots) {
                    ParkingSlot slot = slots.get(index);
                    if (slot.isVacant()) {
                        sb.append(String.format("| %-20s|", "(Ready to park)"));
                    } else {
                        String plate = slot.getCurrentVehicle().getPlateNumber();
                        sb.append(String.format("| Plate: %-13s|", plate));
                    }
                    if (c < cols - 1 && index + 1 < totalSlots) sb.append(" ");
                }
            }
            sb.append("\n");

            // Line 3: Ticket ID & Tandem linkage
            for (int c = 0; c < cols; c++) {
                int index = r * cols + c;
                if (index < totalSlots) {
                    ParkingSlot slot = slots.get(index);
                    if (slot.isVacant()) {
                        sb.append("|                     |");
                    } else if (slot.isLinkedTandem()) {
                        String tandem = String.format(" %-8s <-> %-6s",
                                slot.getCurrentTicketId(), String.format("S-%02d", slot.getLinkedSlotNumber()));
                        sb.append(String.format("|%-21s|", tandem));
                    } else {
                        sb.append(String.format("| Ticket: %-12s|", slot.getCurrentTicketId()));
                    }
                    if (c < cols - 1 && index + 1 < totalSlots) sb.append(" ");
                }
            }
            sb.append("\n");

            // Bottom border of row
            for (int c = 0; c < cols; c++) {
                int index = r * cols + c;
                if (index < totalSlots) {
                    sb.append("+---------------------+");
                    if (c < cols - 1 && index + 1 < totalSlots) sb.append(" ");
                }
            }
            sb.append("\n\n");
        }

        // Legend with plain ASCII labels and Rs.
        sb.append(String.format("%s Vacant   %s Bike (Rs. 20/hr, Cap Rs. 100)   %s Car (Rs. 40/hr, Cap Rs. 250)   %s Truck (Rs. 100/hr, Cap Rs. 600, 2 Slots)\n",
                AnsiColor.green("[VACANT]"),
                AnsiColor.yellow("[BIKE]"),
                AnsiColor.blue("[CAR]"),
                AnsiColor.magenta("[TRUCK]")));

        return sb.toString();
    }
}
