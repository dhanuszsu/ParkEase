package com.parkease.test;

import com.parkease.exception.ParkingFullException;
import com.parkease.model.*;
import com.parkease.service.ParkingLot;
import com.parkease.util.AnsiColor;

import java.time.LocalDateTime;

/**
 * Comprehensive standalone automated verification test runner for ParkEase.
 * Tests OOP models, plate validation, duplicate prevention, consecutive slot allocation,
 * exception handling, billing calculation, daily caps, and revenue tracking.
 */
public class TestRunner {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println(AnsiColor.bold(AnsiColor.cyan(
                "========================================================================\n" +
                "               PARKEASE AUTOMATED TEST SUITE EXECUTION                  \n" +
                "========================================================================")));

        runTest("1. Registration Plate Validation & Normalization", TestRunner::testPlateValidation);
        runTest("2. Duplicate Entry Prevention", TestRunner::testDuplicateEntryPrevention);
        runTest("3. Single Slot Allocation for Bike & Car", TestRunner::testSingleSlotAllocation);
        runTest("4. Truck 2-Consecutive Slot Allocation", TestRunner::testTruckConsecutiveAllocation);
        runTest("5. Truck Fragmentation throws ParkingFullException", TestRunner::testTruckFragmentationException);
        runTest("6. Entire Lot Full throws ParkingFullException", TestRunner::testLotFullException);
        runTest("7. Hourly Billing with Partial Hour Rounding (Math.ceil)", TestRunner::testPartialHourBillingRounding);
        runTest("8. Vehicle-Specific Daily Charge Caps", TestRunner::testDailyChargeCaps);
        runTest("9. Multi-Day Stay with Periodic Caps Calculation", TestRunner::testMultiDayCaps);
        runTest("10. Full Vehicle Departure Lifecycle & Revenue Tracking", TestRunner::testExitLifecycleAndRevenue);

        System.out.println(AnsiColor.bold(AnsiColor.cyan(
                "\n========================================================================")));
        System.out.printf("TEST SUITE RESULTS: %s Passed, %s Failed\n",
                AnsiColor.green(passed + " PASSED"),
                failed == 0 ? AnsiColor.green("0 FAILED") : AnsiColor.red(failed + " FAILED"));
        System.out.println(AnsiColor.bold(AnsiColor.cyan(
                "========================================================================\n")));

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void runTest(String testName, Runnable test) {
        try {
            test.run();
            passed++;
            System.out.printf("  %s %s\n", AnsiColor.green("[PASS]"), testName);
        } catch (Throwable t) {
            failed++;
            System.out.printf("  %s %s\n", AnsiColor.red("[FAIL]"), testName);
            System.out.printf("         %s: %s\n", AnsiColor.red("Error"), t.getMessage());
            t.printStackTrace(System.out);
        }
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError(String.format("%s => Expected: %s, Actual: %s", msg, expected, actual));
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) {
            throw new AssertionError("Assertion Failed: " + msg);
        }
    }

    // 1. Plate validation
    private static void testPlateValidation() {
        assertEquals("MH12AB1234", Vehicle.validateAndNormalizePlate("MH-12-AB-1234"), "Hyphenated plate");
        assertEquals("DL01C9999", Vehicle.validateAndNormalizePlate("DL 01 C 9999"), "Spaced plate");
        assertEquals("KA05MB9876", Vehicle.validateAndNormalizePlate("ka05mb9876"), "Lowercase plate");

        boolean threw = false;
        try {
            Vehicle.validateAndNormalizePlate("INVALID123");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue(threw, "Malformed plate should throw IllegalArgumentException");
    }

    // 2. Duplicate entry prevention
    private static void testDuplicateEntryPrevention() {
        ParkingLot lot = new ParkingLot(10);
        Bike bike1 = new Bike("KA-05-MB-1111");
        try {
            lot.parkVehicle(bike1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        boolean caughtDuplicate = false;
        try {
            Bike bike2 = new Bike("KA05MB1111"); // Same normalized plate
            lot.parkVehicle(bike2);
        } catch (IllegalArgumentException e) {
            caughtDuplicate = true;
            assertTrue(e.getMessage().contains("Duplicate entry rejected"), "Must indicate duplicate entry");
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception: " + e);
        }
        assertTrue(caughtDuplicate, "Duplicate vehicle plate must be rejected");
    }

    // 3. Single slot allocation
    private static void testSingleSlotAllocation() {
        ParkingLot lot = new ParkingLot(5);
        try {
            Ticket t1 = lot.parkVehicle(new Bike("KA01AA1111"));
            assertEquals(1, t1.getAllocatedSlots().size(), "Bike gets 1 slot");
            assertEquals(1, (int) t1.getAllocatedSlots().get(0), "Slot S-01");

            Ticket t2 = lot.parkVehicle(new Car("KA01BB2222"));
            assertEquals(1, t2.getAllocatedSlots().size(), "Car gets 1 slot");
            assertEquals(2, (int) t2.getAllocatedSlots().get(0), "Slot S-02");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 4. Truck consecutive slot allocation
    private static void testTruckConsecutiveAllocation() {
        ParkingLot lot = new ParkingLot(6);
        try {
            Ticket t1 = lot.parkVehicle(new Truck("MH12TR1234"));
            assertEquals(2, t1.getAllocatedSlots().size(), "Truck requires 2 slots");
            assertEquals(1, (int) t1.getAllocatedSlots().get(0), "First slot");
            assertEquals(2, (int) t1.getAllocatedSlots().get(1), "Second slot (consecutive)");

            // Check slots
            assertTrue(lot.getSlots().get(0).isOccupied(), "Slot 1 occupied");
            assertTrue(lot.getSlots().get(1).isOccupied(), "Slot 2 occupied");
            assertTrue(lot.getSlots().get(0).isLinkedTandem(), "Slot 1 tandem linked");
            assertEquals(2, lot.getSlots().get(0).getLinkedSlotNumber(), "Linked to slot 2");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 5. Truck fragmentation throws ParkingFullException
    private static void testTruckFragmentationException() {
        // Create 3 slots: [S1: CAR, S2: VACANT, S3: CAR]
        ParkingLot lot = new ParkingLot(3);
        try {
            lot.parkVehicle(new Car("KA01AA1111")); // S1 occupied
            Ticket temp = lot.parkVehicle(new Car("KA01BB2222")); // S2 occupied
            lot.parkVehicle(new Car("KA01CC3333")); // S3 occupied

            // Vacate S2, so S1 is occupied, S2 is vacant, S3 is occupied. Free slots = 1
            // Let's test with a 4 slot lot to have 2 vacant slots that are NOT consecutive:
            // S1 (Car), S2 (Free), S3 (Car), S4 (Free)
            ParkingLot lot4 = new ParkingLot(4);
            lot4.parkVehicle(new Car("KA01AA1111")); // S1
            Ticket s2Car = lot4.parkVehicle(new Car("KA01BB2222")); // S2
            lot4.parkVehicle(new Car("KA01CC3333")); // S3
            Ticket s4Car = lot4.parkVehicle(new Car("KA01DD4444")); // S4

            // Exit S2 and S4 -> Free slots are S2 and S4 (count = 2, but NOT consecutive!)
            lot4.exitVehicle(s2Car.getTicketId(), LocalDateTime.now());
            lot4.exitVehicle(s4Car.getTicketId(), LocalDateTime.now());

            assertEquals(2, lot4.getVacantSlotsCount(), "Exactly 2 slots free");

            boolean threwParkingFull = false;
            try {
                lot4.parkVehicle(new Truck("MH12ZZ9999"));
            } catch (ParkingFullException pfe) {
                threwParkingFull = true;
                assertEquals(VehicleType.TRUCK, pfe.getVehicleType(), "VehicleType is TRUCK");
                assertTrue(pfe.getMessage().contains("CONSECUTIVE"), "Message mentions consecutive requirement");
            }
            assertTrue(threwParkingFull, "Must throw ParkingFullException when vacant slots are not consecutive");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 6. Lot full throws ParkingFullException
    private static void testLotFullException() {
        ParkingLot lot = new ParkingLot(2);
        try {
            lot.parkVehicle(new Car("KA01AA1111"));
            lot.parkVehicle(new Car("KA01BB2222"));

            boolean threw = false;
            try {
                lot.parkVehicle(new Bike("KA01CC3333"));
            } catch (ParkingFullException e) {
                threw = true;
                assertEquals(VehicleType.BIKE, e.getVehicleType(), "VehicleType is BIKE");
            }
            assertTrue(threw, "Must throw ParkingFullException when lot is 100% full");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 7. Partial hour billing rounding (Math.ceil)
    private static void testPartialHourBillingRounding() {
        LocalDateTime now = LocalDateTime.now();
        Bike bike = new Bike("KA01AA1111", now);
        Car car = new Car("KA01BB2222", now);

        // 10 mins -> 1 hour
        assertEquals(1L, bike.calculateBilledHours(10), "10 mins -> 1 hour");
        assertEquals(20.0, bike.calculateFee(10), "10 mins Bike fee = Rs. 20");

        // 60 mins -> 1 hour
        assertEquals(1L, bike.calculateBilledHours(60), "60 mins -> 1 hour");
        assertEquals(20.0, bike.calculateFee(60), "60 mins Bike fee = Rs. 20");

        // 61 mins -> 2 hours
        assertEquals(2L, bike.calculateBilledHours(61), "61 mins -> 2 hours");
        assertEquals(40.0, bike.calculateFee(61), "61 mins Bike fee = Rs. 40");

        // Car: 125 mins -> 3 hours
        assertEquals(3L, car.calculateBilledHours(125), "125 mins -> 3 hours");
        assertEquals(120.0, car.calculateFee(125), "125 mins Car fee = 3 * Rs. 40 = Rs. 120");
    }

    // 8. Vehicle-specific daily charge caps
    private static void testDailyChargeCaps() {
        LocalDateTime now = LocalDateTime.now();
        Bike bike = new Bike("KA01AA1111", now);
        Car car = new Car("KA01BB2222", now);
        Truck truck = new Truck("MH12TR1234", now);

        // Bike: Rs. 20/hr, Cap = Rs. 100. At 6 hrs uncapped = 120 -> capped to 100
        assertEquals(100.0, bike.calculateFee(6 * 60), "Bike 6 hrs capped at Rs. 100");
        assertEquals(100.0, bike.calculateFee(24 * 60), "Bike 24 hrs capped at Rs. 100");

        // Car: Rs. 40/hr, Cap = Rs. 250. At 7 hrs uncapped = 280 -> capped to 250
        assertEquals(250.0, car.calculateFee(7 * 60), "Car 7 hrs capped at Rs. 250");
        assertEquals(250.0, car.calculateFee(24 * 60), "Car 24 hrs capped at Rs. 250");

        // Truck: Rs. 100/hr, Cap = Rs. 600. At 8 hrs uncapped = 800 -> capped to 600
        assertEquals(600.0, truck.calculateFee(8 * 60), "Truck 8 hrs capped at Rs. 600");
        assertEquals(600.0, truck.calculateFee(24 * 60), "Truck 24 hrs capped at Rs. 600");
    }

    // 9. Multi-day stay with cap calculation
    private static void testMultiDayCaps() {
        LocalDateTime now = LocalDateTime.now();
        Car car = new Car("KA01BB2222", now);

        // 26 hours stay: 1 full day (Rs. 250) + 2 remainder hours (2 * Rs. 40 = Rs. 80) = Rs. 330
        assertEquals(330.0, car.calculateFee(26 * 60), "Car 26 hours = 250 + 80 = Rs. 330");

        // 32 hours stay: 1 full day (Rs. 250) + 8 remainder hours (8 * 40 = 320 -> capped to 250) = Rs. 500
        assertEquals(500.0, car.calculateFee(32 * 60), "Car 32 hours = 250 + 250 = Rs. 500");

        // 48 hours stay: 2 full days = Rs. 500
        assertEquals(500.0, car.calculateFee(48 * 60), "Car 48 hours = 2 * 250 = Rs. 500");
    }

    // 10. Departure lifecycle and revenue tracking
    private static void testExitLifecycleAndRevenue() {
        ParkingLot lot = new ParkingLot(5);
        try {
            LocalDateTime entryTime = LocalDateTime.of(2026, 10, 16, 10, 0);
            Bike bike = new Bike("KA01AA1111", entryTime);
            Ticket t1 = lot.parkVehicle(bike);

            Car car = new Car("KA01BB2222", entryTime);
            lot.parkVehicle(car);

            assertEquals(2, lot.getOccupiedSlotsCount(), "2 slots occupied");

            // Exit bike after 2 hours (12:00)
            LocalDateTime exitTime = LocalDateTime.of(2026, 10, 16, 12, 0);
            Ticket closedTicket = lot.exitVehicle(t1.getTicketId(), exitTime);

            assertEquals(40.0, closedTicket.getFeeCharged(), "Bike 2 hours = Rs. 40");
            assertTrue(!closedTicket.isActive(), "Ticket is inactive");
            assertEquals(1, lot.getOccupiedSlotsCount(), "1 slot now occupied (Bike slot freed)");

            SummaryStatistics summary = lot.getSummary();
            assertEquals(1, summary.getTotalParkedVehicles(), "1 parked vehicle remaining");
            assertEquals(1, summary.getCompletedExits(), "1 completed exit");
            assertEquals(40.0, summary.getTotalRevenueCollected(), "Revenue is Rs. 40");
            assertEquals(40.0, summary.getRevenue(VehicleType.BIKE), "Bike revenue is Rs. 40");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
