package com.parkease.model;

public class ParkingSlot {
    private final int slotNumber;
    private Vehicle currentVehicle;
    private String currentTicketId;
    private boolean isLinkedTandem;
    private int linkedSlotNumber;

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

    public void occupy(Vehicle vehicle, String ticketId, boolean isLinked, int pairedSlot) {
        this.currentVehicle = vehicle;
        this.currentTicketId = ticketId;
        this.isLinkedTandem = isLinked;
        this.linkedSlotNumber = pairedSlot;
    }

    public void vacate() {
        this.currentVehicle = null;
        this.currentTicketId = null;
        this.isLinkedTandem = false;
        this.linkedSlotNumber = 0;
    }

    public String getFormattedSlotId() {
        return String.format("S-%02d", slotNumber);
    }
}
