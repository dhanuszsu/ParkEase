package com.parkease.ui;

import com.parkease.exception.ParkingFullException;
import com.parkease.model.*;
import com.parkease.service.ParkingLot;
import com.parkease.util.AnsiColor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class ParkEaseApp {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ParkingLot parkingLot;
    private final Scanner scanner;

    public ParkEaseApp() {
        this(new ParkingLot(ParkingLot.DEFAULT_TOTAL_SLOTS));
    }

    public ParkEaseApp(ParkingLot parkingLot) {
        this.parkingLot = parkingLot;
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        try {
            System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
        ParkEaseApp app = new ParkEaseApp();
        app.run();
    }

    public void run() {
        printAnimatedWelcomeBanner();

        boolean running = true;
        while (running) {
            displayStatusBar();
            displayMenu();
            System.out.print(AnsiColor.bold(AnsiColor.orange("╭─ Select option (1-6): \n╰─➤ ")));
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> handleVehicleEntry();
                case "2" -> handleVehicleExit();
                case "3" -> handleViewSlotMap();
                case "4" -> handleViewSummary();
                case "5" -> handleLoadDemoPresets();
                case "6" -> {
                    System.out.println();
                    AnsiColor.showSpinner("Closing active lot sessions and saving transactions to disk...", 800);
                    System.out.println(AnsiColor.bold(AnsiColor.orange("\n✨ Thank you for using ParkEase Smart Parking System. Have a great day! ✨\n")));
                    running = false;
                }
                default -> System.out.println(AnsiColor.orange("\n  ⚠ [!] Invalid option. Please enter a number between 1 and 6.\n"));
            }
        }
    }

    private void printAnimatedWelcomeBanner() {
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
            try { Thread.sleep(30); } catch (InterruptedException ignored) {}
        }
        System.out.println(AnsiColor.bold(AnsiColor.orange("  ⚡ SMART PARKING LOT AUTOMATION & BILLING ENGINE (JAVA 17 + SQL) ⚡\n")));
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
    }

    private void displayStatusBar() {
        SummaryStatistics stats = parkingLot.getSummary();
        int total = stats.getTotalSlots();
        int occ = stats.getOccupiedSlots();
        int vac = stats.getVacantSlots();
        double rev = stats.getTotalRevenueCollected();

        double occPct = total > 0 ? ((double) occ / total) * 100.0 : 0.0;
        int barLength = 20;
        int filled = (int) Math.round((occPct / 100.0) * barLength);
        String progressBar = "█".repeat(filled) + "░".repeat(Math.max(0, barLength - filled));

        System.out.println(AnsiColor.mutedOrange("┌" + "─".repeat(86) + "┐"));
        StringBuilder sb = new StringBuilder();
        sb.append(AnsiColor.mutedOrange("│ "))
          .append(AnsiColor.orange("CAPACITY: "))
          .append(AnsiColor.bold(AnsiColor.white(String.format("%-2d", total))))
          .append(AnsiColor.gray(" Slots"))
          .append(AnsiColor.mutedOrange("  │ "))
          .append(AnsiColor.orange("OCCUPIED: "))
          .append(AnsiColor.bold(AnsiColor.white(String.format("%-2d", occ))))
          .append(AnsiColor.mutedOrange("  │ "))
          .append(AnsiColor.orange("VACANT: "))
          .append(AnsiColor.bold(AnsiColor.white(String.format("%-2d", vac))))
          .append(AnsiColor.mutedOrange("  │ "))
          .append(AnsiColor.orange("REVENUE: "))
          .append(AnsiColor.bold(AnsiColor.white(String.format("Rs. %-10.2f", rev))))
          .append(AnsiColor.mutedOrange("│ "))
          .append(AnsiColor.orange("LOAD: "))
          .append("[")
          .append(AnsiColor.orange(progressBar))
          .append("] ")
          .append(AnsiColor.bold(AnsiColor.white(String.format("%3.0f%%", occPct))))
          .append(AnsiColor.mutedOrange(" │"));
        System.out.println(sb.toString());
        System.out.println(AnsiColor.mutedOrange("└" + "─".repeat(86) + "┘"));
    }

    private void displayMenu() {
        System.out.println(AnsiColor.bold(AnsiColor.orange("✦ MAIN CONTROL DASHBOARD:")));
        System.out.println("  " + AnsiColor.bold(AnsiColor.orange("【1】")) + " " + AnsiColor.bold(AnsiColor.white("Park Vehicle")) + "    " + AnsiColor.gray("→ Issue Ticket & Smart Slot Allocation"));
        System.out.println("  " + AnsiColor.bold(AnsiColor.orange("【2】")) + " " + AnsiColor.bold(AnsiColor.white("Exit Vehicle")) + "    " + AnsiColor.gray("→ Hourly Billing Calculation & Vacate Slot"));
        System.out.println("  " + AnsiColor.bold(AnsiColor.orange("【3】")) + " " + AnsiColor.bold(AnsiColor.white("Live Slot Map")) + "   " + AnsiColor.gray("→ Real-Time 20-Bay Visual 2D Grid"));
        System.out.println("  " + AnsiColor.bold(AnsiColor.orange("【4】")) + " " + AnsiColor.bold(AnsiColor.white("Reports")) + "         " + AnsiColor.gray("→ Summary Metrics & Financial Audit"));
        System.out.println("  " + AnsiColor.bold(AnsiColor.orange("【5】")) + " " + AnsiColor.bold(AnsiColor.white("Quick Demo")) + "      " + AnsiColor.gray("→ Seed 5 Sample Cars, Bikes & Tandem Truck"));
        System.out.println("  " + AnsiColor.bold(AnsiColor.orange("【6】")) + " " + AnsiColor.bold(AnsiColor.white("Quit")) + "            " + AnsiColor.gray("→ Save & Exit Application"));
        System.out.println();
    }

    private void handleVehicleEntry() {
        System.out.println(AnsiColor.bold(AnsiColor.orange("\n╭───────────────────────────────────────────────────────────╮")));
        System.out.println(AnsiColor.bold(AnsiColor.orange("│             [1] VEHICLE ENTRY & ALLOCATION                │")));
        System.out.println(AnsiColor.bold(AnsiColor.orange("╰───────────────────────────────────────────────────────────╯")));

        System.out.println(AnsiColor.bold(AnsiColor.orange("Select Vehicle Classification:")));
        System.out.println("  " + AnsiColor.bold(AnsiColor.orange("1) ")) + AnsiColor.bold(AnsiColor.white("Two-Wheeler (Bike)")) + "   " + AnsiColor.gray("1 Slot  | Rs. 10/hr | Daily Cap Rs. 80 (8 hrs max)"));
        System.out.println("  " + AnsiColor.bold(AnsiColor.orange("2) ")) + AnsiColor.bold(AnsiColor.white("Four-Wheeler (Car)")) + "    " + AnsiColor.gray("1 Slot  | Rs. 30/hr | Daily Cap Rs. 240 (8 hrs max)"));
        System.out.println("  " + AnsiColor.bold(AnsiColor.orange("3) ")) + AnsiColor.bold(AnsiColor.white("Heavy Truck (Cargo)")) + "   " + AnsiColor.gray("2 Slots | Rs. 60/hr | Daily Cap Rs. 480 (8 hrs max)"));
        System.out.print(AnsiColor.orange("Enter vehicle type (1-3): "));
        String typeChoice = scanner.nextLine().trim();

        VehicleType type;
        switch (typeChoice) {
            case "1" -> type = VehicleType.BIKE;
            case "2" -> type = VehicleType.CAR;
            case "3" -> type = VehicleType.TRUCK;
            default -> {
                System.out.println(AnsiColor.orange("\n  ⚠ [!] Invalid vehicle classification. Entry aborted.\n"));
                return;
            }
        }

        System.out.print(AnsiColor.orange("Enter Registration Plate Number (e.g. TN37AB1234, DL01C9999): "));
        String plateInput = scanner.nextLine().trim();

        String normalizedPlate;
        try {
            normalizedPlate = Vehicle.validateAndNormalizePlate(plateInput);
        } catch (IllegalArgumentException e) {
            System.out.println(AnsiColor.orange("\n  ⚠ [!] Validation Error: " + e.getMessage() + "\n"));
            return;
        }

        System.out.print(AnsiColor.orange("Enter Owner Name (optional, press Enter for 'Walk-in Guest'): "));
        String ownerInput = scanner.nextLine().trim();
        String ownerName = ownerInput.isEmpty() ? "Walk-in Guest" : ownerInput;

        Vehicle vehicle;
        switch (type) {
            case BIKE -> vehicle = new Bike(normalizedPlate, ownerName);
            case CAR -> vehicle = new Car(normalizedPlate, ownerName);
            case TRUCK -> vehicle = new Truck(normalizedPlate, ownerName);
            default -> throw new IllegalStateException();
        }

        AnsiColor.showSpinner("Scanning bay array & committing SQL transaction...", 500);

        try {
            Ticket ticket = parkingLot.parkVehicle(vehicle);
            System.out.println(AnsiColor.bold(AnsiColor.orange("\n  ✔ SUCCESS: Vehicle accepted into parking facility!")));
            System.out.println("    " + AnsiColor.bold(AnsiColor.orange("Ticket ID        : ")) + AnsiColor.bold(AnsiColor.white(ticket.getTicketId())) +
                               "\n    " + AnsiColor.bold(AnsiColor.orange("Assigned Slot(s) : ")) + AnsiColor.bold(AnsiColor.white(ticket.getFormattedSlots())));
            System.out.println();
            System.out.println(ticket.generateEntryReceipt());
            System.out.println();
        } catch (ParkingFullException e) {
            System.out.println(AnsiColor.bold(AnsiColor.orange("\n  ✖ [PARKING FULL EXCEPTION]")));
            System.out.println(AnsiColor.orange("    " + e.getMessage()));
            if (e.getVehicleType() == VehicleType.TRUCK) {
                System.out.println(AnsiColor.orange("    Note: Trucks require 2 adjacent contiguous slots. View live map [3].\n"));
            } else {
                System.out.println(AnsiColor.orange("    The parking facility is at 100% capacity.\n"));
            }
        } catch (IllegalArgumentException e) {
            System.out.println(AnsiColor.bold(AnsiColor.orange("\n  ✖ [ENTRY REJECTED]")));
            System.out.println(AnsiColor.orange("    " + e.getMessage() + "\n"));
        }
    }

    private void handleVehicleExit() {
        System.out.println(AnsiColor.bold(AnsiColor.orange("\n╭───────────────────────────────────────────────────────────╮")));
        System.out.println(AnsiColor.bold(AnsiColor.orange("│             [2] VEHICLE EXIT & BILLING CHECKOUT           │")));
        System.out.println(AnsiColor.bold(AnsiColor.orange("╰───────────────────────────────────────────────────────────╯")));
        System.out.print(AnsiColor.orange("Enter Ticket ID (e.g. TKT-1001) or Plate Number (e.g. TN37AB1234): "));
        String identifier = scanner.nextLine().trim();

        var optTicket = parkingLot.findActiveTicket(identifier);
        if (optTicket.isEmpty()) {
            System.out.println(AnsiColor.orange("\n  ⚠ [!] No active vehicle found matching '" + identifier + "'. Please re-verify.\n"));
            return;
        }

        Ticket activeTicket = optTicket.get();
        System.out.println(AnsiColor.orange("\n  ✔ Vehicle Located: ") +
                AnsiColor.bold(AnsiColor.white(activeTicket.getVehicle().getVehicleType().getDisplayName())) +
                AnsiColor.white(" (" + activeTicket.getVehicle().getPlateNumber() + ")") +
                AnsiColor.mutedOrange(" | ") +
                AnsiColor.orange("Owner: ") + AnsiColor.white(activeTicket.getVehicle().getOwnerName()) +
                AnsiColor.mutedOrange(" | ") +
                AnsiColor.orange("Slot(s): ") + AnsiColor.bold(AnsiColor.white(activeTicket.getFormattedSlots())));
        System.out.println(AnsiColor.orange("  Entry Time: ") + AnsiColor.white(activeTicket.getEntryTime().format(TIME_FMT)));

        System.out.println(AnsiColor.bold(AnsiColor.orange("\nSelect Checkout Mode:")));
        System.out.println("  " + AnsiColor.bold(AnsiColor.orange("1) ")) + AnsiColor.bold(AnsiColor.white("Exit Now")) + " " + AnsiColor.gray("(Current Clock: " + LocalDateTime.now().format(TIME_FMT) + ")"));
        System.out.println("  " + AnsiColor.bold(AnsiColor.orange("2) ")) + AnsiColor.bold(AnsiColor.white("Enter Hours Parked")) + " " + AnsiColor.gray("(Manual Simulation / Test flow)"));
        System.out.print(AnsiColor.orange("Enter choice (1 or 2): "));
        String timeChoice = scanner.nextLine().trim();

        LocalDateTime exitTime;
        if ("2".equals(timeChoice)) {
            System.out.print(AnsiColor.orange("Hours parked (e.g. 1.5, 3, 9, 24): "));
            try {
                double hours = Double.parseDouble(scanner.nextLine().trim());
                if (hours < 0) {
                    System.out.println(AnsiColor.orange("  ⚠ [!] Duration cannot be negative.\n"));
                    return;
                }
                long minutes = (long) Math.round(hours * 60);
                exitTime = activeTicket.getEntryTime().plusMinutes(minutes);
            } catch (NumberFormatException e) {
                System.out.println(AnsiColor.orange("  ⚠ [!] Invalid numerical input. Checkout cancelled.\n"));
                return;
            }
        } else {
            exitTime = LocalDateTime.now();
        }

        AnsiColor.showSpinner("Computing billing fee and freeing slot in database...", 500);

        try {
            Ticket closedTicket = parkingLot.exitVehicle(identifier, exitTime);
            System.out.println(AnsiColor.bold(AnsiColor.orange("\n  ✔ CHECKOUT COMPLETED SUCCESSFULLY!")));
            System.out.println("  " + AnsiColor.orange("Ticket No     : ") + AnsiColor.bold(AnsiColor.white(closedTicket.getTicketId())));
            System.out.println("  " + AnsiColor.orange("Hours Parked  : ") + AnsiColor.bold(AnsiColor.white(closedTicket.getBilledHours() + " hour(s)")));
            System.out.println("  " + AnsiColor.orange("Calculation   : ") + AnsiColor.white(closedTicket.getBilledHours() + " x Rs. " + String.format("%.0f", closedTicket.getVehicle().getHourlyRate()) + " = ") + AnsiColor.bold(AnsiColor.white("Rs. " + String.format("%.2f", closedTicket.getFeeCharged()))));
            System.out.println("  " + AnsiColor.orange("Status        : ") + AnsiColor.white("Slot " + closedTicket.getFormattedSlots() + " is now free.\n"));
            System.out.println(closedTicket.generateExitReceipt());
            System.out.println();
        } catch (Exception e) {
            System.out.println(AnsiColor.orange("  ✖ Checkout failed: " + e.getMessage() + "\n"));
        }
    }

    private void handleViewSlotMap() {
        System.out.println();
        System.out.print(parkingLot.renderLiveSlotMap());
        System.out.println();
    }

    private void handleViewSummary() {
        SummaryStatistics stats = parkingLot.getSummary();
        String border = "═".repeat(88);
        System.out.println(AnsiColor.bold(AnsiColor.orange("\n╔" + border + "╗")));
        System.out.println(AnsiColor.bold(AnsiColor.orange("║                    PARKEASE EXECUTIVE SUMMARY & REVENUE REPORT                         ║")));
        System.out.println(AnsiColor.bold(AnsiColor.orange("╠" + border + "╣")));
        System.out.println(AnsiColor.mutedOrange("║  ") + AnsiColor.orange(String.format("%-30s: ", "Total Parking Capacity")) + AnsiColor.bold(AnsiColor.white(String.format("%-51d", stats.getTotalSlots()))) + AnsiColor.mutedOrange(" ║"));
        System.out.println(AnsiColor.mutedOrange("║  ") + AnsiColor.orange(String.format("%-30s: ", "Occupied Slots")) + AnsiColor.bold(AnsiColor.white(String.format("%-51d", stats.getOccupiedSlots()))) + AnsiColor.mutedOrange(" ║"));
        System.out.println(AnsiColor.mutedOrange("║  ") + AnsiColor.orange(String.format("%-30s: ", "Vacant Slots Available")) + AnsiColor.bold(AnsiColor.white(String.format("%-51d", stats.getVacantSlots()))) + AnsiColor.mutedOrange(" ║"));
        System.out.println(AnsiColor.mutedOrange("║  ") + AnsiColor.orange(String.format("%-30s: ", "Facility Occupancy Rate")) + AnsiColor.bold(AnsiColor.white(String.format("%-50.1f%%", stats.getOccupancyRatePercentage()))) + AnsiColor.mutedOrange(" ║"));
        System.out.println(AnsiColor.bold(AnsiColor.orange("╟" + border + "╢")));
        System.out.println(AnsiColor.mutedOrange("║  ") + AnsiColor.bold(AnsiColor.orange("ACTIVE VEHICLES BREAKDOWN:")) + " ".repeat(60) + AnsiColor.mutedOrange("║"));
        System.out.println(AnsiColor.mutedOrange("║    • ") + AnsiColor.orange(String.format("%-26s: ", "Two-Wheelers (Bikes)")) + AnsiColor.bold(AnsiColor.white(String.format("%-51d", stats.getParkedBikes()))) + AnsiColor.mutedOrange(" ║"));
        System.out.println(AnsiColor.mutedOrange("║    • ") + AnsiColor.orange(String.format("%-26s: ", "Four-Wheelers (Cars)")) + AnsiColor.bold(AnsiColor.white(String.format("%-51d", stats.getParkedCars()))) + AnsiColor.mutedOrange(" ║"));
        System.out.println(AnsiColor.mutedOrange("║    • ") + AnsiColor.orange(String.format("%-26s: ", "Heavy Trucks (Tandem)")) + AnsiColor.bold(AnsiColor.white(String.format("%-51d", stats.getParkedTrucks()))) + AnsiColor.mutedOrange(" ║"));
        System.out.println(AnsiColor.mutedOrange("║    • ") + AnsiColor.orange(String.format("%-26s: ", "Total Active Parked")) + AnsiColor.bold(AnsiColor.white(String.format("%-51d", stats.getTotalParkedVehicles()))) + AnsiColor.mutedOrange(" ║"));
        System.out.println(AnsiColor.bold(AnsiColor.orange("╟" + border + "╢")));
        System.out.println(AnsiColor.mutedOrange("║  ") + AnsiColor.bold(AnsiColor.orange("HISTORICAL LIFECYCLE & FINANCIAL TOTALS:")) + " ".repeat(46) + AnsiColor.mutedOrange("║"));
        System.out.println(AnsiColor.mutedOrange("║    • ") + AnsiColor.orange(String.format("%-26s: ", "Total Tickets Issued")) + AnsiColor.bold(AnsiColor.white(String.format("%-51d", stats.getTotalTicketsIssued()))) + AnsiColor.mutedOrange(" ║"));
        System.out.println(AnsiColor.mutedOrange("║    • ") + AnsiColor.orange(String.format("%-26s: ", "Completed Departures")) + AnsiColor.bold(AnsiColor.white(String.format("%-51d", stats.getCompletedExits()))) + AnsiColor.mutedOrange(" ║"));
        System.out.println(AnsiColor.mutedOrange("║    • ") + AnsiColor.orange(String.format("%-26s: ", "Bike Revenue Collection")) + AnsiColor.bold(AnsiColor.white(String.format("Rs. %-47.2f", stats.getRevenue(VehicleType.BIKE)))) + AnsiColor.mutedOrange(" ║"));
        System.out.println(AnsiColor.mutedOrange("║    • ") + AnsiColor.orange(String.format("%-26s: ", "Car Revenue Collection")) + AnsiColor.bold(AnsiColor.white(String.format("Rs. %-47.2f", stats.getRevenue(VehicleType.CAR)))) + AnsiColor.mutedOrange(" ║"));
        System.out.println(AnsiColor.mutedOrange("║    • ") + AnsiColor.orange(String.format("%-26s: ", "Truck Revenue Collection")) + AnsiColor.bold(AnsiColor.white(String.format("Rs. %-47.2f", stats.getRevenue(VehicleType.TRUCK)))) + AnsiColor.mutedOrange(" ║"));
        System.out.println(AnsiColor.mutedOrange("║    • ") + AnsiColor.bold(AnsiColor.orange(String.format("%-26s: ", "TOTAL CASH REVENUE"))) + AnsiColor.bold(AnsiColor.white(String.format("Rs. %-47.2f", stats.getTotalRevenueCollected()))) + AnsiColor.mutedOrange(" ║"));
        System.out.println(AnsiColor.bold(AnsiColor.orange("╚" + border + "╝\n")));

        if (parkingLot.getDatabaseManager() != null && parkingLot.getDatabaseManager().isDriverAvailable()) {
            System.out.println(AnsiColor.bold(AnsiColor.orange("✦ DATABASE SQL AUDIT VIEW (Direct SQL query on `vw_revenue_summary`):")));
            System.out.println(AnsiColor.orange(parkingLot.getDatabaseManager().executeQueryToAscii(
                    "SELECT vehicle_type AS 'Vehicle', total_tickets_issued AS 'Tickets', completed_checkouts AS 'Exits', currently_parked AS 'Parked', 'Rs. ' || total_revenue AS 'Revenue' FROM vw_revenue_summary;")));
        }
    }

    private void handleLoadDemoPresets() {
        System.out.println(AnsiColor.bold(AnsiColor.orange("\n--- SEEDING QUICK-START DEMO VEHICLES ---")));
        AnsiColor.showProgressBar("Injecting Sample Fleet into Database", 600);
        try {
            LocalDateTime baseTime = LocalDateTime.now().minusHours(3);

            parkingLot.parkVehicle(new Bike("TN-37-AB-1234", "Karthik Raja", baseTime));
            parkingLot.parkVehicle(new Car("TN-09-CD-5678", "Dr. Senthil Kumar", baseTime.plusMinutes(20)));
            parkingLot.parkVehicle(new Truck("TN-01-TR-9000", "Chennai Freight Corp", baseTime.plusMinutes(45)));
            parkingLot.parkVehicle(new Car("KA-05-XY-3344", "Priya Sharma", baseTime.plusMinutes(60)));
            parkingLot.parkVehicle(new Bike("MH-12-MM-7788", "Rohan Mehta", baseTime.plusMinutes(75)));

            System.out.println(AnsiColor.orange("\n  ✔ [OK] Successfully parked 5 sample vehicles with owners!"));
            System.out.println(AnsiColor.mutedOrange("  Check the visual live map (Option [3]) to see their allocated bays.\n"));
        } catch (Exception e) {
            System.out.println(AnsiColor.orange("  ⚠ Notice: " + e.getMessage() + "\n"));
        }
    }
}
