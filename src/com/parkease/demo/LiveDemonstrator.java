package com.parkease.demo;

import com.parkease.exception.ParkingFullException;
import com.parkease.model.*;
import com.parkease.service.ParkingLot;
import com.parkease.util.AnsiColor;

import java.time.LocalDateTime;

public class LiveDemonstrator {

    private static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void header(int stepNum, String title) {
        System.out.println();
        System.out.println(AnsiColor.bold(AnsiColor.orange("==================================================================")));
        System.out.println(AnsiColor.bold(AnsiColor.orange(String.format("  STEP %d: %s", stepNum, title))));
        System.out.println(AnsiColor.bold(AnsiColor.orange("==================================================================")));
        System.out.println();
        pause(1200);
    }

    public static void main(String[] args) {
        String[] logoLines = {
            "██████╗  █████╗ ██████╗ ██╗  ██╗███████╗ █████╗ ███████╗███████╗",
            "██╔══██╗██╔══██╗██╔══██╗██║ ██╔╝██╔════╝██╔══██╗██╔════╝██╔════╝",
            "██████╔╝███████║██████╔╝█████╔╝ █████╗  ███████║███████╗█████╗  ",
            "██╔═══╝ ██╔══██║██╔══██╗██╔═██╗ ██╔══╝  ██╔══██║╚════██║██╔══╝  ",
            "██║     ██║  ██║██║  ██║██║  ██╗███████╗██║  ██║███████║███████╗",
            "╚═╝     ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚══════╝╚══════╝"
        };
        System.out.println();
        for (String line : logoLines) {
            System.out.println(AnsiColor.bold(AnsiColor.orangeGradientText(line)));
        }
        System.out.println(AnsiColor.bold(AnsiColor.orange("         PARKEASE LIVE SYSTEM DEMONSTRATION WALKTHROUGH        \n")));
        System.out.println(AnsiColor.orange("  The system will demonstrate itself step-by-step.\n"));
        pause(1800);

        ParkingLot lot = new ParkingLot(20);
        LocalDateTime baseTime = LocalDateTime.of(2026, 10, 19, 10, 0, 0);

        header(1, "Park a Car (TN37AB1234)");
        System.out.println("Selecting: [1] Park Vehicle");
        pause(800);
        System.out.println("  Type: Car (Rs. 30/hr)");
        pause(800);
        System.out.println("  Plate: TN37AB1234");
        pause(1000);

        Ticket carTicket = null;
        try {
            carTicket = lot.parkVehicle(new Car("TN37AB1234", "Ramesh Kumar", baseTime));
            System.out.println(AnsiColor.orange(String.format("\n[OK] Ticket %s issued. Slot: %s\n",
                    carTicket.getTicketId(), carTicket.getFormattedSlots())));
            System.out.println(AnsiColor.orange(carTicket.generateEntryReceipt()));
        } catch (Exception e) {
            System.out.println(AnsiColor.orange("Error: " + e.getMessage()));
        }
        pause(2500);

        header(2, "Park a Bike (1 Slot) & a Heavy Truck (2 Consecutive Slots)");
        System.out.println("Parking Bike (KA05MB9876)...");
        pause(800);
        Ticket bikeTicket = null;
        try {
            bikeTicket = lot.parkVehicle(new Bike("KA05MB9876", "Ananya Rao", baseTime.plusMinutes(15)));
            System.out.println(AnsiColor.orange(String.format("  ✔ Ticket %s issued. Slot: %s (Rs. 10/hr)",
                    bikeTicket.getTicketId(), bikeTicket.getFormattedSlots())));
        } catch (Exception e) {
            System.out.println(AnsiColor.orange("Error: " + e.getMessage()));
        }

        pause(1200);
        System.out.println("\nParking Truck (DL01TR9000 - Needs 2 Adjacent Slots)...");
        pause(1000);
        Ticket truckTicket = null;
        try {
            truckTicket = lot.parkVehicle(new Truck("DL01TR9000", "Express Cargo", baseTime.plusMinutes(30)));
            System.out.println(AnsiColor.orange(String.format("  ✔ Ticket %s issued. Tandem Slots: %s (Rs. 60/hr)",
                    truckTicket.getTicketId(), truckTicket.getFormattedSlots())));
        } catch (Exception e) {
            System.out.println(AnsiColor.orange("Error: " + e.getMessage()));
        }
        pause(2500);

        header(3, "View Live Slot Map (Visual 20-Bay Grid)");
        System.out.println("Selecting: [3] Slot Map\n");
        pause(800);
        System.out.print(lot.renderLiveSlotMap());
        System.out.println();
        pause(3000);

        header(4, "Duplicate Entry Defense (Block Same Vehicle Twice)");
        System.out.println("Car with plate 'TN37AB1234' tries to enter again while already parked...");
        pause(1200);

        try {
            lot.parkVehicle(new Car("TN37AB1234", "Imposter Driver", baseTime.plusMinutes(45)));
            System.out.println(AnsiColor.orange("  [FAIL] Duplicate was allowed!"));
        } catch (IllegalArgumentException e) {
            System.out.println(AnsiColor.orange("  ✔ [BLOCKED BY SYSTEM]:"));
            System.out.println(AnsiColor.orange("    " + e.getMessage()));
        } catch (Exception e) {
            System.out.println(AnsiColor.orange("Error: " + e.getMessage()));
        }
        pause(2500);

        header(5, "Exit Vehicle & Billing");
        System.out.println("Car TN37AB1234 leaves after 3 hours.");
        pause(800);
        System.out.println("Selecting: [2] Exit Vehicle");
        pause(800);
        System.out.println(String.format("  Ticket No: %s", carTicket.getTicketId()));
        pause(800);
        System.out.println("  Hours parked: 3");
        pause(1200);

        try {
            LocalDateTime exitTime = carTicket.getEntryTime().plusHours(3);
            Ticket closed = lot.exitVehicle(carTicket.getTicketId(), exitTime);
            System.out.println(AnsiColor.orange("\n[OK] Vehicle checkout complete!"));
            System.out.println(AnsiColor.bold(AnsiColor.orange(String.format("Charge = %d x Rs. %.0f = Rs. %.2f",
                    closed.getBilledHours(), closed.getVehicle().getHourlyRate(), closed.getFeeCharged()))));
            System.out.println(AnsiColor.bold(AnsiColor.orange(String.format("Slot %s is now free.\n", closed.getFormattedSlots()))));
            pause(1200);
            System.out.println(AnsiColor.orange(closed.generateExitReceipt()));
        } catch (Exception e) {
            System.out.println(AnsiColor.orange("Error: " + e.getMessage()));
        }
        pause(2500);

        header(6, "Daily Cap Rule (Max 8 Hours Charge)");
        System.out.println("Bike KA05MB9876 stayed for 12 hours.");
        pause(800);
        System.out.println("  Standard rate: 12 hrs x Rs. 10 = Rs. 120.00");
        pause(800);
        System.out.println("  Applying daily cap: Capped at Rs. 80.00 max...");
        pause(1200);

        try {
            LocalDateTime exitBikeTime = bikeTicket.getEntryTime().plusHours(12);
            Ticket closedBike = lot.exitVehicle(bikeTicket.getTicketId(), exitBikeTime);
            System.out.println(AnsiColor.orange(String.format("\n[OK] Fee Charged: Rs. %.2f (Capped at 8 hours max!)", closedBike.getFeeCharged())));
            System.out.println(AnsiColor.orange(String.format("Slot %s is now free.", closedBike.getFormattedSlots())));
        } catch (Exception e) {
            System.out.println(AnsiColor.orange("Error: " + e.getMessage()));
        }
        pause(2500);

        header(7, "ParkingFullException Demonstration");
        System.out.println("Simulating lot at 100% capacity (using 2-slot lot)...");
        pause(800);

        ParkingLot smallLot = new ParkingLot(2);
        try {
            smallLot.parkVehicle(new Car("TN01AA1111", baseTime));
            smallLot.parkVehicle(new Car("TN01BB2222", baseTime));
            System.out.println("  Slot 1 & Slot 2 are both occupied.");
            pause(800);
            System.out.println("  Attempting to park a 3rd vehicle...");
            pause(1000);
            smallLot.parkVehicle(new Bike("TN01CC3333", baseTime));
        } catch (ParkingFullException e) {
            System.out.println(AnsiColor.orange("\n  ✔ [EXCEPTION CAUGHT]:"));
            System.out.println(AnsiColor.orange("    " + e.getMessage()));
        } catch (Exception e) {
            System.out.println(AnsiColor.orange("Error: " + e.getMessage()));
        }
        pause(2500);

        header(8, "Summary & Database Persistence");
        SummaryStatistics stats = lot.getSummary();
        System.out.println(AnsiColor.mutedOrange("+----------------------------------------------------------------------------------------+"));
        System.out.printf(AnsiColor.orange("| Total Capacity: %-4d | Occupied: %-4d | Vacant: %-4d | Total Revenue: Rs. %-15.2f |\n"),
                stats.getTotalSlots(), stats.getOccupiedSlots(), stats.getVacantSlots(), stats.getTotalRevenueCollected());
        System.out.println(AnsiColor.mutedOrange("+----------------------------------------------------------------------------------------+"));
        System.out.printf(AnsiColor.orange("  * Total Tickets Issued  : %d\n"), stats.getTotalTicketsIssued());
        System.out.printf(AnsiColor.orange("  * Completed Checkouts   : %d\n"), stats.getCompletedExits());
        System.out.printf(AnsiColor.orange("  * Currently Parked      : %d (Truck DL01TR9000 in slots %s)\n"),
                stats.getTotalParkedVehicles(), truckTicket.getFormattedSlots());
        System.out.println(AnsiColor.orange("  * Database File         : parkease.db (All records saved to SQLite)"));
        System.out.println(AnsiColor.mutedOrange("+----------------------------------------------------------------------------------------+"));
        pause(1500);

        System.out.println(AnsiColor.bold(AnsiColor.orange("""

            ==================================================================
                           DEMONSTRATION COMPLETED SUCCESSFULLY!
               Run again anytime with: ./demo.sh
               Or test manually with: ./run.sh
            ==================================================================
            """)));
    }
}
