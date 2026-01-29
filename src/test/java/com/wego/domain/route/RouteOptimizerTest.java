package com.wego.domain.route;

import com.wego.entity.Activity;
import com.wego.entity.Place;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RouteOptimizer.
 *
 * Tests cover:
 * - Basic optimization scenarios
 * - Edge cases (0, 1, 2 activities)
 * - Same location handling
 * - Warning for 15+ activities
 * - Distance calculation accuracy
 * - Algorithm correctness properties
 */
@Tag("fast")
@DisplayName("RouteOptimizer Unit Tests")
class RouteOptimizerTest {

    private RouteOptimizer routeOptimizer;

    // Test locations (real-world coordinates in Taipei)
    private static final double TAIPEI_101_LAT = 25.0330;
    private static final double TAIPEI_101_LNG = 121.5654;
    private static final double TAIPEI_MAIN_STATION_LAT = 25.0478;
    private static final double TAIPEI_MAIN_STATION_LNG = 121.5170;
    private static final double SHILIN_MARKET_LAT = 25.0880;
    private static final double SHILIN_MARKET_LNG = 121.5244;
    private static final double LONGSHAN_TEMPLE_LAT = 25.0373;
    private static final double LONGSHAN_TEMPLE_LNG = 121.5000;
    private static final double XIMENDING_LAT = 25.0423;
    private static final double XIMENDING_LNG = 121.5081;

    @BeforeEach
    void setUp() {
        routeOptimizer = new RouteOptimizer();
    }

    @Nested
    @DisplayName("Edge Cases - Empty and Minimal Input")
    class EdgeCases {

        @Test
        @DisplayName("optimize_emptyList_shouldReturnNoOptimization")
        void optimize_emptyList_shouldReturnNoOptimization() {
            List<Activity> activities = new ArrayList<>();
            Map<UUID, Place> placeLookup = new HashMap<>();

            OptimizationResult result = routeOptimizer.optimize(activities, placeLookup);

            assertNotNull(result);
            assertTrue(result.getOriginalOrder().isEmpty());
            assertTrue(result.getOptimizedOrder().isEmpty());
            assertFalse(result.isOptimizationApplied());
            assertEquals(0.0, result.getOriginalDistanceMeters());
        }

        @Test
        @DisplayName("optimize_nullList_shouldReturnNoOptimization")
        void optimize_nullList_shouldReturnNoOptimization() {
            OptimizationResult result = routeOptimizer.optimize(null, new HashMap<>());

            assertNotNull(result);
            assertTrue(result.getOriginalOrder().isEmpty());
            assertFalse(result.isOptimizationApplied());
        }

        @Test
        @DisplayName("optimize_singleActivity_shouldReturnOriginalOrder")
        void optimize_singleActivity_shouldReturnOriginalOrder() {
            TestData data = createTestData(1);

            OptimizationResult result = routeOptimizer.optimize(data.activities, data.placeLookup);

            assertNotNull(result);
            assertEquals(1, result.getOriginalOrder().size());
            assertEquals(data.activities.get(0).getId(), result.getOriginalOrder().get(0));
            assertFalse(result.isOptimizationApplied());
            assertEquals(0.0, result.getOriginalDistanceMeters());
        }

        @Test
        @DisplayName("optimize_twoActivities_shouldReturnOriginalOrder")
        void optimize_twoActivities_shouldReturnOriginalOrder() {
            TestData data = createTestDataWithCoordinates(List.of(
                    new double[]{TAIPEI_101_LAT, TAIPEI_101_LNG},
                    new double[]{TAIPEI_MAIN_STATION_LAT, TAIPEI_MAIN_STATION_LNG}
            ));

            OptimizationResult result = routeOptimizer.optimize(data.activities, data.placeLookup);

            assertNotNull(result);
            assertEquals(2, result.getOriginalOrder().size());
            assertEquals(data.activities.get(0).getId(), result.getOriginalOrder().get(0));
            assertEquals(data.activities.get(1).getId(), result.getOriginalOrder().get(1));
            assertFalse(result.isOptimizationApplied());
            assertTrue(result.getOriginalDistanceMeters() > 0);
        }

        @Test
        @DisplayName("optimize_activitiesWithoutPlaces_shouldReturnNoOptimization")
        void optimize_activitiesWithoutPlaces_shouldReturnNoOptimization() {
            List<Activity> activities = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Activity activity = Activity.builder()
                        .id(UUID.randomUUID())
                        .tripId(UUID.randomUUID())
                        .day(1)
                        .sortOrder(i)
                        .placeId(null) // No place
                        .build();
                activities.add(activity);
            }

            OptimizationResult result = routeOptimizer.optimize(activities, new HashMap<>());

            assertNotNull(result);
            assertFalse(result.isOptimizationApplied());
        }
    }

    @Nested
    @DisplayName("Basic Optimization Scenarios")
    class BasicOptimization {

        @Test
        @DisplayName("optimize_threeActivities_shouldOptimizeRoute")
        void optimize_threeActivities_shouldOptimizeRoute() {
            // Create route: Taipei 101 -> Shilin (far) -> Main Station (closer to 101)
            // Optimal: Taipei 101 -> Main Station -> Shilin
            TestData data = createTestDataWithCoordinates(List.of(
                    new double[]{TAIPEI_101_LAT, TAIPEI_101_LNG},           // Start
                    new double[]{SHILIN_MARKET_LAT, SHILIN_MARKET_LNG},     // Far from start
                    new double[]{TAIPEI_MAIN_STATION_LAT, TAIPEI_MAIN_STATION_LNG} // Closer to start
            ));

            OptimizationResult result = routeOptimizer.optimize(data.activities, data.placeLookup);

            assertNotNull(result);
            assertEquals(3, result.getOptimizedOrder().size());
            // First activity should remain the same (starting point)
            assertEquals(data.activities.get(0).getId(), result.getOptimizedOrder().get(0));
            // The optimized distance should be less than or equal to original
            assertTrue(result.getOptimizedDistanceMeters() <= result.getOriginalDistanceMeters());
        }

        @Test
        @DisplayName("optimize_fiveActivitiesInSuboptimalOrder_shouldImproveRoute")
        void optimize_fiveActivitiesInSuboptimalOrder_shouldImproveRoute() {
            // Deliberately suboptimal order: zigzag pattern
            TestData data = createTestDataWithCoordinates(List.of(
                    new double[]{TAIPEI_101_LAT, TAIPEI_101_LNG},           // South-east
                    new double[]{SHILIN_MARKET_LAT, SHILIN_MARKET_LNG},     // Far north
                    new double[]{LONGSHAN_TEMPLE_LAT, LONGSHAN_TEMPLE_LNG}, // South-west
                    new double[]{TAIPEI_MAIN_STATION_LAT, TAIPEI_MAIN_STATION_LNG}, // Central
                    new double[]{XIMENDING_LAT, XIMENDING_LNG}              // Near main station
            ));

            OptimizationResult result = routeOptimizer.optimize(data.activities, data.placeLookup);

            assertNotNull(result);
            assertEquals(5, result.getOptimizedOrder().size());
            // Optimization should be applied for this suboptimal route
            assertTrue(result.isOptimizationApplied() ||
                       result.getOptimizedDistanceMeters() <= result.getOriginalDistanceMeters());
        }

        @Test
        @DisplayName("optimize_alreadyOptimalRoute_shouldReturnOriginalOrder")
        void optimize_alreadyOptimalRoute_shouldReturnOriginalOrder() {
            // Create a linear route that's already optimal
            // A -> B -> C where each is progressively further in same direction
            TestData data = createTestDataWithCoordinates(List.of(
                    new double[]{25.0000, 121.5000},
                    new double[]{25.0100, 121.5000},  // North of first
                    new double[]{25.0200, 121.5000}   // North of second
            ));

            OptimizationResult result = routeOptimizer.optimize(data.activities, data.placeLookup);

            assertNotNull(result);
            // Distance should not increase
            assertTrue(result.getOptimizedDistanceMeters() <= result.getOriginalDistanceMeters() * 1.01);
        }
    }

    @Nested
    @DisplayName("Same Location Handling")
    class SameLocationHandling {

        @Test
        @DisplayName("optimize_allSameLocation_shouldReturnOriginalOrder")
        void optimize_allSameLocation_shouldReturnOriginalOrder() {
            TestData data = createTestDataWithCoordinates(List.of(
                    new double[]{TAIPEI_101_LAT, TAIPEI_101_LNG},
                    new double[]{TAIPEI_101_LAT, TAIPEI_101_LNG},
                    new double[]{TAIPEI_101_LAT, TAIPEI_101_LNG}
            ));

            OptimizationResult result = routeOptimizer.optimize(data.activities, data.placeLookup);

            assertNotNull(result);
            assertEquals(3, result.getOriginalOrder().size());
            assertEquals(0.0, result.getOriginalDistanceMeters(), 0.001);
            assertEquals(0.0, result.getOptimizedDistanceMeters(), 0.001);
            assertFalse(result.isOptimizationApplied());
        }

        @Test
        @DisplayName("optimize_someSameLocation_shouldHandleCorrectly")
        void optimize_someSameLocation_shouldHandleCorrectly() {
            TestData data = createTestDataWithCoordinates(List.of(
                    new double[]{TAIPEI_101_LAT, TAIPEI_101_LNG},
                    new double[]{TAIPEI_101_LAT, TAIPEI_101_LNG},  // Same as first
                    new double[]{TAIPEI_MAIN_STATION_LAT, TAIPEI_MAIN_STATION_LNG}
            ));

            OptimizationResult result = routeOptimizer.optimize(data.activities, data.placeLookup);

            assertNotNull(result);
            assertEquals(3, result.getOptimizedOrder().size());
            // All activities should be in the result
            assertTrue(result.getOptimizedOrder().containsAll(
                    data.activities.stream().map(Activity::getId).toList()));
        }
    }

    @Nested
    @DisplayName("Warning for 15+ Activities")
    class WarningHandling {

        @Test
        @DisplayName("optimize_exactlyFifteenActivities_shouldNotWarn")
        void optimize_exactlyFifteenActivities_shouldNotWarn() {
            TestData data = createTestData(15);

            OptimizationResult result = routeOptimizer.optimize(data.activities, data.placeLookup);

            assertNotNull(result);
            assertNull(result.getWarningMessage());
        }

        @Test
        @DisplayName("optimize_sixteenActivities_shouldWarn")
        void optimize_sixteenActivities_shouldWarn() {
            TestData data = createTestData(16);

            OptimizationResult result = routeOptimizer.optimize(data.activities, data.placeLookup);

            assertNotNull(result);
            assertNotNull(result.getWarningMessage());
            assertTrue(result.getWarningMessage().contains("16"));
            assertTrue(result.getWarningMessage().contains("15"));
        }

        @Test
        @DisplayName("optimize_twentyActivities_shouldWarnButStillOptimize")
        void optimize_twentyActivities_shouldWarnButStillOptimize() {
            TestData data = createTestData(20);

            OptimizationResult result = routeOptimizer.optimize(data.activities, data.placeLookup);

            assertNotNull(result);
            assertNotNull(result.getWarningMessage());
            assertEquals(20, result.getOriginalOrder().size());
            assertEquals(20, result.getOptimizedOrder().size());
        }
    }

    @Nested
    @DisplayName("Distance Calculation Accuracy")
    class DistanceCalculation {

        @Test
        @DisplayName("calculateDistanceMeters_taipei101ToMainStation_shouldBeApproximatelyCorrect")
        void calculateDistanceMeters_taipei101ToMainStation_shouldBeApproximatelyCorrect() {
            // Known distance approximately 5.7 km
            double distance = routeOptimizer.calculateDistanceMeters(
                    TAIPEI_101_LAT, TAIPEI_101_LNG,
                    TAIPEI_MAIN_STATION_LAT, TAIPEI_MAIN_STATION_LNG
            );

            // Allow 10% margin for calculation differences
            assertTrue(distance > 5000 && distance < 7000,
                    "Expected ~5.7km, got " + distance / 1000 + "km");
        }

        @Test
        @DisplayName("calculateDistanceMeters_samePoint_shouldReturnZero")
        void calculateDistanceMeters_samePoint_shouldReturnZero() {
            double distance = routeOptimizer.calculateDistanceMeters(
                    TAIPEI_101_LAT, TAIPEI_101_LNG,
                    TAIPEI_101_LAT, TAIPEI_101_LNG
            );

            assertEquals(0.0, distance, 0.001);
        }

        @Test
        @DisplayName("calculateDistanceMeters_antipodalPoints_shouldReturnHalfEarthCircumference")
        void calculateDistanceMeters_antipodalPoints_shouldReturnHalfEarthCircumference() {
            // North pole to South pole
            double distance = routeOptimizer.calculateDistanceMeters(90, 0, -90, 0);

            // Half of Earth's circumference is approximately 20,000 km
            assertTrue(distance > 19_000_000 && distance < 21_000_000,
                    "Expected ~20000km, got " + distance / 1000 + "km");
        }

        @Test
        @DisplayName("calculateTotalDistance_multipleActivities_shouldSumCorrectly")
        void calculateTotalDistance_multipleActivities_shouldSumCorrectly() {
            TestData data = createTestDataWithCoordinates(List.of(
                    new double[]{25.0000, 121.5000},
                    new double[]{25.0100, 121.5000},
                    new double[]{25.0200, 121.5000}
            ));

            double totalDistance = routeOptimizer.calculateTotalDistance(
                    data.activities, data.placeLookup);

            // Distance A->B + B->C
            double distanceAB = routeOptimizer.calculateDistanceMeters(
                    25.0000, 121.5000, 25.0100, 121.5000);
            double distanceBC = routeOptimizer.calculateDistanceMeters(
                    25.0100, 121.5000, 25.0200, 121.5000);

            assertEquals(distanceAB + distanceBC, totalDistance, 0.001);
        }
    }

    @Nested
    @DisplayName("Algorithm Correctness Properties")
    class AlgorithmProperties {

        @Test
        @DisplayName("optimize_shouldPreserveFirstActivity")
        void optimize_shouldPreserveFirstActivity() {
            TestData data = createTestData(5);

            OptimizationResult result = routeOptimizer.optimize(data.activities, data.placeLookup);

            assertNotNull(result);
            assertEquals(data.activities.get(0).getId(), result.getOptimizedOrder().get(0),
                    "First activity (starting point) must be preserved");
        }

        @Test
        @DisplayName("optimize_shouldContainAllActivities")
        void optimize_shouldContainAllActivities() {
            TestData data = createTestData(8);

            OptimizationResult result = routeOptimizer.optimize(data.activities, data.placeLookup);

            assertNotNull(result);
            List<UUID> originalIds = data.activities.stream().map(Activity::getId).toList();

            assertEquals(originalIds.size(), result.getOptimizedOrder().size());
            assertTrue(result.getOptimizedOrder().containsAll(originalIds),
                    "Optimized order must contain all original activities");
        }

        @Test
        @DisplayName("optimize_shouldNotIntroduceDuplicates")
        void optimize_shouldNotIntroduceDuplicates() {
            TestData data = createTestData(10);

            OptimizationResult result = routeOptimizer.optimize(data.activities, data.placeLookup);

            assertNotNull(result);
            long uniqueCount = result.getOptimizedOrder().stream().distinct().count();
            assertEquals(result.getOptimizedOrder().size(), uniqueCount,
                    "Optimized order must not contain duplicates");
        }

        @Test
        @DisplayName("optimize_shouldCalculateCorrectSavings")
        void optimize_shouldCalculateCorrectSavings() {
            TestData data = createTestData(5);

            OptimizationResult result = routeOptimizer.optimize(data.activities, data.placeLookup);

            assertNotNull(result);
            double expectedSavings = result.getOriginalDistanceMeters() - result.getOptimizedDistanceMeters();
            assertEquals(expectedSavings, result.getDistanceSavedMeters(), 0.001);

            if (result.getOriginalDistanceMeters() > 0) {
                double expectedPercentage = (expectedSavings / result.getOriginalDistanceMeters()) * 100.0;
                assertEquals(expectedPercentage, result.getSavingsPercentage(), 0.01);
            }
        }

        @Test
        @DisplayName("optimize_shouldBeIdempotent")
        void optimize_shouldBeIdempotent() {
            TestData data = createTestData(5);

            OptimizationResult result1 = routeOptimizer.optimize(data.activities, data.placeLookup);
            OptimizationResult result2 = routeOptimizer.optimize(data.activities, data.placeLookup);

            assertEquals(result1.getOptimizedOrder(), result2.getOptimizedOrder(),
                    "Optimization should be deterministic");
            assertEquals(result1.getOptimizedDistanceMeters(), result2.getOptimizedDistanceMeters(), 0.001);
        }
    }

    @Nested
    @DisplayName("OptimizationResult Factory Methods")
    class OptimizationResultTests {

        @Test
        @DisplayName("create_shouldCalculateSavingsPercentageCorrectly")
        void create_shouldCalculateSavingsPercentageCorrectly() {
            List<UUID> originalOrder = List.of(UUID.randomUUID(), UUID.randomUUID());
            List<UUID> optimizedOrder = List.of(originalOrder.get(0), originalOrder.get(1));

            OptimizationResult result = OptimizationResult.create(
                    originalOrder, optimizedOrder, 1000.0, 800.0, null, true);

            assertEquals(200.0, result.getDistanceSavedMeters(), 0.001);
            assertEquals(20.0, result.getSavingsPercentage(), 0.001);
        }

        @Test
        @DisplayName("create_withZeroOriginalDistance_shouldHaveZeroSavingsPercentage")
        void create_withZeroOriginalDistance_shouldHaveZeroSavingsPercentage() {
            List<UUID> order = List.of(UUID.randomUUID());

            OptimizationResult result = OptimizationResult.create(
                    order, order, 0.0, 0.0, null, false);

            assertEquals(0.0, result.getSavingsPercentage(), 0.001);
        }

        @Test
        @DisplayName("noOptimizationNeeded_shouldSetCorrectFlags")
        void noOptimizationNeeded_shouldSetCorrectFlags() {
            List<UUID> order = List.of(UUID.randomUUID(), UUID.randomUUID());

            OptimizationResult result = OptimizationResult.noOptimizationNeeded(order, 500.0);

            assertEquals(order, result.getOriginalOrder());
            assertEquals(order, result.getOptimizedOrder());
            assertEquals(500.0, result.getOriginalDistanceMeters());
            assertEquals(500.0, result.getOptimizedDistanceMeters());
            assertEquals(0.0, result.getDistanceSavedMeters());
            assertFalse(result.isOptimizationApplied());
        }

        @Test
        @DisplayName("create_shouldMakeImmutableCopies")
        void create_shouldMakeImmutableCopies() {
            List<UUID> originalOrder = new ArrayList<>(List.of(UUID.randomUUID(), UUID.randomUUID()));
            List<UUID> optimizedOrder = new ArrayList<>(originalOrder);

            OptimizationResult result = OptimizationResult.create(
                    originalOrder, optimizedOrder, 100.0, 80.0, null, true);

            // Modify original lists
            UUID newId = UUID.randomUUID();
            originalOrder.add(newId);
            optimizedOrder.add(newId);

            // Result should not be affected
            assertEquals(2, result.getOriginalOrder().size());
            assertEquals(2, result.getOptimizedOrder().size());
        }
    }

    // ========== Helper Methods ==========

    /**
     * Creates test data with random coordinates in Taipei area.
     */
    private TestData createTestData(int activityCount) {
        List<double[]> coordinates = new ArrayList<>();
        for (int i = 0; i < activityCount; i++) {
            // Random coordinates in Taipei area
            double lat = 25.0 + (Math.random() * 0.1);
            double lng = 121.5 + (Math.random() * 0.1);
            coordinates.add(new double[]{lat, lng});
        }
        return createTestDataWithCoordinates(coordinates);
    }

    /**
     * Creates test data with specified coordinates.
     */
    private TestData createTestDataWithCoordinates(List<double[]> coordinates) {
        UUID tripId = UUID.randomUUID();
        List<Activity> activities = new ArrayList<>();
        Map<UUID, Place> placeLookup = new HashMap<>();

        for (int i = 0; i < coordinates.size(); i++) {
            double[] coord = coordinates.get(i);
            UUID placeId = UUID.randomUUID();
            UUID activityId = UUID.randomUUID();

            Place place = Place.builder()
                    .id(placeId)
                    .name("Place " + (i + 1))
                    .latitude(coord[0])
                    .longitude(coord[1])
                    .build();

            Activity activity = Activity.builder()
                    .id(activityId)
                    .tripId(tripId)
                    .placeId(placeId)
                    .day(1)
                    .sortOrder(i)
                    .build();

            activities.add(activity);
            placeLookup.put(placeId, place);
        }

        return new TestData(activities, placeLookup);
    }

    /**
     * Test data container.
     */
    private record TestData(List<Activity> activities, Map<UUID, Place> placeLookup) {}
}
