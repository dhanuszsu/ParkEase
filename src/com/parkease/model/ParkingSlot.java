package com.parkease.model;

/**
 * Represents a discrete physical parking slot within the ParkEase lot.
 */
public class ParkingSlot {
    private final int slotNumber;
    private Vehicle currentVehicle;
    private String currentTicketId;
    private boolean isLinkedTandem;
    private int linkedSlotNumber; // e.g. slot 5 linked to slot 6 for a truck

    public ParkingSlot(int slotNumber) {
        this.slotNumber = slotNumber;
        this.currentVehicle = null;
        this.currentTicketId = null;
        this.isLinkedTandem = false;
        this.linkedSlotNumber = 0;
    }

    public int getSlotNumber() {
        return slotNumber;
    }

    public boolean isVacant() {
        return currentVehicle == null;
    }

    public boolean isOccupied() {
        return currentVehicle != null;
    }

    public Vehicle getCurrentVehicle() {
        return currentVehicle;
    }

    public String getCurrentTicketId() {
        return currentTicketId;
    }

    public boolean isLinkedTandem() {
        return isLinkedTandem;
    }

    public int getLinkedSlotNumber() {
        return linkedSlotNumber;
    }

    /**
     * Allocates this slot to a vehicle.
     */
    public void occupy(Vehicle vehicle, String ticketId, boolean isLinked, int pairedSlot) {
        this.currentVehicle = vehicle;
        this.currentTicketId = ticketId;
        this.isLinkedTandem = isLinked;
        this.linkedSlotNumber = pairedSlot;
    }

    /**
     * Vacates this slot.
     */
    public void vacate() {
        this.currentVehicle = null;
        this.currentTicketId = null;
        this.isLinkedTandem = false;
        this.linkedSlotNumber = 0;
    }

    public String getFormattedSlotId() {
        return String.format("S-%02d", slotNumber);
    }

    @Override
    public String toString() {
        if (isVacant()) {
            return String.format("[%s: VACANT]", getFormattedSlotId());
        }
        return String.format("[%s: %s | %s | %s%s]",
                getFormattedSlotId(),
                currentVehicle.getVehicleType().getDisplayName(),
                currentVehicle.getPlateNumber(),
                currentTicketId,
                isLinkedTandem ? " (Tandem S-" + String.format("%02d", linkedSlotNumber) + ")" : "");
    }
}
