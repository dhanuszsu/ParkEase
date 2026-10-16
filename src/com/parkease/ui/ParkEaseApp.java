package com.parkease.ui;

import com.parkease.exception.ParkingFullException;
import com.parkease.model.*;
import com.parkease.service.ParkingLot;
import com.parkease.test.TestRunner;
import com.parkease.util.AnsiColor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * Interactive, polished Console UI for ParkEase Smart Parking Lot System.
 */
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
        printWelcomeBanner();

        boolean running = true;
        while (running) {
            displayStatusBar();
            displayMenu();
            System.out.print(AnsiColor.bold(AnsiColor.cyan("Select an option (1-7): ")));
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> handleVehicleEntry();
                case "2" -> handleVehicleExit();
                case "3" -> handleViewSlotMap();
                case "4" -> handleViewSummary();
                case "5" -> handleLoadDemoPresets();
                case "6" -> handleRunAutomatedTests();
                case "7" -> {
                    System.out.println(AnsiColor.bold(AnsiColor.green("\nThank you for choosing ParkEase Smart Parking System. Goodbye!\n")));
                    running = false;
                }
                default -> System.out.println(AnsiColor.red("[!] Invalid selection. Please enter a number between 1 and 7.\n"));
            }
        }
    }

    private void printWelcomeBanner() {
        System.out.println(AnsiColor.bold(AnsiColor.cyan(
                "\n" +
                "██████╗  █████╗ ██████╗ ██╗  ██╗███████╗ █████╗ ███████╗███████╗\n" +
                "██╔══██╗██╔══██╗██╔══██╗██║ ██╔╝██╔════╝██╔══██╗██╔════╝██╔════╝\n" +
                "██████╔╝███████║██████╔╝█████╔╝ █████╗  ███████║███████╗█████╗  \n" +
                "██╔═══╝ ██╔══██║██╔══██╗██╔═██╗ ██╔══╝  ██╔══██║╚════██║██╔══╝  \n" +
                "██║     ██║  ██║██║  ██║██║  ██╗███████╗██║  ██║███████║███████╗\n" +
                "╚═╝     ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚══════╝╚══════╝\n" +
                "         SMART PARKING LOT MANAGEMENT & BILLING SYSTEM          \n"
        )));
    }

    private void displayStatusBar() {
        SummaryStatistics stats = parkingLot.getSummary();
        System.out.println(AnsiColor.white("+----------------------------------------------------------------------------------------+"));
        System.out.printf("| Total Slots: %-4d | Occupied: %-4d | Vacant: %-4d | Revenue: Rs. %-24.2f |\n",
                stats.getTotalSlots(),
                stats.getOccupiedSlots(),
                stats.getVacantSlots(),
                stats.getTotalRevenueCollected());
        System.out.println(AnsiColor.white("+----------------------------------------------------------------------------------------+"));
    }

    private void displayMenu() {
        System.out.println(AnsiColor.bold("OPERATIONAL MENU:"));
        System.out.println("  " + AnsiColor.yellow("[1]") + " Vehicle Entry (Issue Ticket & Allocate Slot)");
        System.out.println("  " + AnsiColor.yellow("[2]") + " Vehicle Exit & Billing (Checkout & Free Slot)");
        System.out.println("  " + AnsiColor.yellow("[3]") + " View Live Slot Map (2D Visual Grid)");
        System.out.println("  " + AnsiColor.yellow("[4]") + " View Summary Statistics & Revenue Report");
        System.out.println("  " + AnsiColor.yellow("[5]") + " Load Quick Demo Scenario (Seed Sample Vehicles)");
        System.out.println("  " + AnsiColor.yellow("[6]") + " Run Automated Verification Tests (Suite of 10)");
        System.out.println("  " + AnsiColor.yellow("[7]") + " Exit Application");
        System.out.println();
    }

    private void handleVehicleEntry() {
        System.out.println(AnsiColor.bold(AnsiColor.cyan("\n--- [1] VEHICLE ENTRY & TICKET ISSUANCE ---")));

        // 1. Vehicle Type Selection
        System.out.println("Select Vehicle Type:");
        System.out.println("  1) Bike  (1 Slot  | Rs. 20/hr  | Daily Cap Rs. 100)");
        System.out.println("  2) Car   (1 Slot  | Rs. 40/hr  | Daily Cap Rs. 250)");
        System.out.println("  3) Truck (2 Slots | Rs. 100/hr | Daily Cap Rs. 600)");
        System.out.print(AnsiColor.cyan("Enter choice (1-3): "));
        String typeChoice = scanner.nextLine().trim();

        VehicleType type;
        switch (typeChoice) {
            case "1" -> type = VehicleType.BIKE;
            case "2" -> type = VehicleType.CAR;
            case "3" -> type = VehicleType.TRUCK;
            default -> {
                System.out.println(AnsiColor.red("[!] Invalid vehicle type. Entry cancelled.\n"));
                return;
            }
        }

        // 2. Plate Input with Validation
        System.out.print(AnsiColor.cyan("Enter Registration Plate Number (e.g. MH-12-AB-1234, DL01C9999): "));
        String plateInput = scanner.nextLine().trim();

        String normalizedPlate;
        try {
            normalizedPlate = Vehicle.validateAndNormalizePlate(plateInput);
        } catch (IllegalArgumentException e) {
            System.out.println(AnsiColor.red("[!] Validation Error: " + e.getMessage() + "\n"));
            return;
        }

        // 3. Create vehicle instance
        Vehicle vehicle;
        switch (type) {
            case BIKE -> vehicle = new Bike(normalizedPlate);
            case CAR -> vehicle = new Car(normalizedPlate);
            case TRUCK -> vehicle = new Truck(normalizedPlate);
            default -> throw new IllegalStateException();
        }

        // 4. Attempt Parking
        try {
            Ticket ticket = parkingLot.parkVehicle(vehicle);
            System.out.println(AnsiColor.green("\n[OK] Vehicle parked successfully!"));
            System.out.println(ticket.generateEntryReceipt());
            System.out.println();
        } catch (ParkingFullException e) {
            System.out.println(AnsiColor.bold(AnsiColor.red("\n[ERROR] [PARKING FULL EXCEPTION]")));
            System.out.println(AnsiColor.red("Details: " + e.getMessage()));
            if (e.getVehicleType() == VehicleType.TRUCK) {
                System.out.println(AnsiColor.yellow("Note: Trucks require 2 adjacent consecutive slots. Check the live map.\n"));
            } else {
                System.out.println(AnsiColor.yellow("No slots are currently available.\n"));
            }
        } catch (IllegalArgumentException e) {
            System.out.println(AnsiColor.bold(AnsiColor.red("\n[ERROR] [ENTRY REJECTED]")));
            System.out.println(AnsiColor.red(e.getMessage() + "\n"));
        }
    }

    private void handleVehicleExit() {
        System.out.println(AnsiColor.bold(AnsiColor.cyan("\n--- [2] VEHICLE EXIT & BILLING CHECKOUT ---")));
        System.out.print(AnsiColor.cyan("Enter Ticket ID (e.g. TKT-1001) or Plate Number (e.g. MH12AB1234): "));
        String identifier = scanner.nextLine().trim();

        var optTicket = parkingLot.findActiveTicket(identifier);
        if (optTicket.isEmpty()) {
            System.out.println(AnsiColor.red("[!] No active parked vehicle found for: " + identifier + "\n"));
            return;
        }

        Ticket activeTicket = optTicket.get();
        System.out.println(AnsiColor.green("Vehicle Found: " + activeTicket.getVehicle().getVehicleType().getDisplayName() +
                " (" + activeTicket.getVehicle().getPlateNumber() + "), Parked in: " + activeTicket.getFormattedSlots()));
        System.out.println("Entry Timestamp: " + activeTicket.getEntryTime().format(TIME_FMT));

        // Options for exit time
        System.out.println("\nSelect Exit Time Mode:");
        System.out.println("  1) Exit Now (Current Clock Time: " + LocalDateTime.now().format(TIME_FMT) + ")");
        System.out.println("  2) Simulate Elapsed Duration (e.g., enter hours to test rounding & daily cap)");
        System.out.print(AnsiColor.cyan("Enter choice (1 or 2): "));
        String timeChoice = scanner.nextLine().trim();

        LocalDateTime exitTime;
        if ("2".equals(timeChoice)) {
            System.out.print(AnsiColor.cyan("Enter elapsed parking duration in hours (e.g. 1.25, 3.5, 7, 28): "));
            try {
                double hours = Double.parseDouble(scanner.nextLine().trim());
                if (hours < 0) {
                    System.out.println(AnsiColor.red("[!] Duration cannot be negative.\n"));
                    return;
                }
                long minutes = (long) Math.round(hours * 60);
                exitTime = activeTicket.getEntryTime().plusMinutes(minutes);
            } catch (NumberFormatException e) {
                System.out.println(AnsiColor.red("[!] Invalid number format. Exit cancelled.\n"));
                return;
            }
        } else {
            exitTime = LocalDateTime.now();
        }

        try {
            Ticket closedTicket = parkingLot.exitVehicle(identifier, exitTime);
            System.out.println(AnsiColor.green("\n[OK] Vehicle checkout complete! Slots freed: " + closedTicket.getFormattedSlots()));
            System.out.println(closedTicket.generateExitReceipt());
            System.out.println();
        } catch (Exception e) {
            System.out.println(AnsiColor.red("[!] Checkout failed: " + e.getMessage() + "\n"));
        }
    }

    private void handleViewSlotMap() {
        System.out.println();
        System.out.print(parkingLot.renderLiveSlotMap());
        System.out.println();
    }

    private void handleViewSummary() {
        SummaryStatistics stats = parkingLot.getSummary();
        String border = "+" + "-".repeat(88) + "+";
        System.out.println(AnsiColor.bold(AnsiColor.cyan(
                "\n" + border + "\n" +
                "|                         PARKEASE LOT SUMMARY & FINANCIAL REPORT                        |\n" +
                border)));
        System.out.printf("|  %-28s: %-56d |\n", "Total Parking Capacity", stats.getTotalSlots());
        System.out.printf("|  %-28s: %-56d |\n", "Occupied Slots", stats.getOccupiedSlots());
        System.out.printf("|  %-28s: %-56d |\n", "Vacant Slots Available", stats.getVacantSlots());
        System.out.printf("|  %-28s: %-55.1f%% |\n", "Lot Occupancy Rate", stats.getOccupancyRatePercentage());
        System.out.println(border);
        System.out.println("|  CURRENTLY PARKED VEHICLES BREAKDOWN:                                                  |");
        System.out.printf("|    * %-24s: %-56d |\n", "Two-Wheelers (Bikes)", stats.getParkedBikes());
        System.out.printf("|    * %-24s: %-56d |\n", "Four-Wheelers (Cars)", stats.getParkedCars());
        System.out.printf("|    * %-24s: %-56d |\n", "Heavy Vehicles (Trucks)", stats.getParkedTrucks());
        System.out.printf("|    * %-24s: %-56d |\n", "Total Active Vehicles", stats.getTotalParkedVehicles());
        System.out.println(border);
        System.out.println("|  HISTORICAL REVENUE & LIFECYCLE METRICS:                                               |");
        System.out.printf("|    * %-24s: %-56d |\n", "Total Tickets Issued", stats.getTotalTicketsIssued());
        System.out.printf("|    * %-24s: %-56d |\n", "Completed Checkouts", stats.getCompletedExits());
        System.out.printf("|    * %-24s: Rs. %-52.2f |\n", "Bike Collections", stats.getRevenue(VehicleType.BIKE));
        System.out.printf("|    * %-24s: Rs. %-52.2f |\n", "Car Collections", stats.getRevenue(VehicleType.CAR));
        System.out.printf("|    * %-24s: Rs. %-52.2f |\n", "Truck Collections", stats.getRevenue(VehicleType.TRUCK));
        System.out.printf("|    * %-24s: Rs. %-52.2f |\n", "TOTAL REVENUE COLLECTED", stats.getTotalRevenueCollected());
        System.out.println(border + "\n");
    }

    private void handleLoadDemoPresets() {
        System.out.println(AnsiColor.bold(AnsiColor.cyan("\n--- SEEDING QUICK-START DEMO VEHICLES ---")));
        try {
            LocalDateTime baseTime = LocalDateTime.now().minusHours(3);

            parkingLot.parkVehicle(new Bike("KA-01-AB-1234", baseTime));
            parkingLot.parkVehicle(new Car("MH-12-CD-5678", baseTime.plusMinutes(20)));
            parkingLot.parkVehicle(new Truck("DL-01-TR-9000", baseTime.plusMinutes(45)));
            parkingLot.parkVehicle(new Car("TN-09-XY-3344", baseTime.plusMinutes(60)));
            parkingLot.parkVehicle(new Bike("GJ-03-MM-7788", baseTime.plusMinutes(75)));

            System.out.println(AnsiColor.green("[OK] Successfully seeded 5 sample vehicles (2 Bikes, 2 Cars, 1 Truck tandem)!"));
            System.out.println("View the live map ([3]) to see their allocated slots.\n");
        } catch (Exception e) {
            System.out.println(AnsiColor.red("[!] Notice: " + e.getMessage() + "\n"));
        }
    }

    private void handleRunAutomatedTests() {
        System.out.println();
        TestRunner.main(new String[]{});
        System.out.println();
    }
}
