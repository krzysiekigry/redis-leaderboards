package pl.krzysiekigry.redisleaderboards;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class EnumsAndUtilitiesTest {

    @Test
    public void testUpdatePolicyValues() {
        UpdatePolicy[] values = UpdatePolicy.values();
        assertEquals(3, values.length);
        
        assertTrue(contains(values, UpdatePolicy.REPLACE));
        assertTrue(contains(values, UpdatePolicy.AGGREGATE));
        assertTrue(contains(values, UpdatePolicy.BEST));
    }

    @Test
    public void testUpdatePolicyValueOf() {
        assertEquals(UpdatePolicy.REPLACE, UpdatePolicy.valueOf("REPLACE"));
        assertEquals(UpdatePolicy.AGGREGATE, UpdatePolicy.valueOf("AGGREGATE"));
        assertEquals(UpdatePolicy.BEST, UpdatePolicy.valueOf("BEST"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            UpdatePolicy.valueOf("INVALID");
        });
    }

    @Test
    public void testUpdatePolicyToString() {
        assertEquals("REPLACE", UpdatePolicy.REPLACE.toString());
        assertEquals("AGGREGATE", UpdatePolicy.AGGREGATE.toString());
        assertEquals("BEST", UpdatePolicy.BEST.toString());
    }

    @Test
    public void testSortPolicyValues() {
        SortPolicy[] values = SortPolicy.values();
        assertEquals(2, values.length);
        
        assertTrue(contains(values, SortPolicy.HIGH_TO_LOW));
        assertTrue(contains(values, SortPolicy.LOW_TO_HIGH));
    }

    @Test
    public void testSortPolicyValueOf() {
        assertEquals(SortPolicy.HIGH_TO_LOW, SortPolicy.valueOf("HIGH_TO_LOW"));
        assertEquals(SortPolicy.LOW_TO_HIGH, SortPolicy.valueOf("LOW_TO_HIGH"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            SortPolicy.valueOf("INVALID");
        });
    }

    @Test
    public void testSortPolicyGetValue() {
        assertEquals("high-to-low", SortPolicy.HIGH_TO_LOW.getValue());
        assertEquals("low-to-high", SortPolicy.LOW_TO_HIGH.getValue());
    }

    @Test
    public void testSortPolicyToString() {
        assertEquals("HIGH_TO_LOW", SortPolicy.HIGH_TO_LOW.toString());
        assertEquals("LOW_TO_HIGH", SortPolicy.LOW_TO_HIGH.toString());
    }

    @Test
    public void testDefaultCyclesValues() {
        DefaultCycles[] values = DefaultCycles.values();
        assertEquals(6, values.length);
        
        assertTrue(contains(values, DefaultCycles.MINUTE));
        assertTrue(contains(values, DefaultCycles.HOURLY));
        assertTrue(contains(values, DefaultCycles.DAILY));
        assertTrue(contains(values, DefaultCycles.WEEKLY));
        assertTrue(contains(values, DefaultCycles.MONTHLY));
        assertTrue(contains(values, DefaultCycles.YEARLY));
    }

    @Test
    public void testDefaultCyclesValueOf() {
        assertEquals(DefaultCycles.MINUTE, DefaultCycles.valueOf("MINUTE"));
        assertEquals(DefaultCycles.HOURLY, DefaultCycles.valueOf("HOURLY"));
        assertEquals(DefaultCycles.DAILY, DefaultCycles.valueOf("DAILY"));
        assertEquals(DefaultCycles.WEEKLY, DefaultCycles.valueOf("WEEKLY"));
        assertEquals(DefaultCycles.MONTHLY, DefaultCycles.valueOf("MONTHLY"));
        assertEquals(DefaultCycles.YEARLY, DefaultCycles.valueOf("YEARLY"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            DefaultCycles.valueOf("INVALID");
        });
    }

    @Test
    public void testDefaultCyclesToString() {
        assertEquals("MINUTE", DefaultCycles.MINUTE.toString());
        assertEquals("HOURLY", DefaultCycles.HOURLY.toString());
        assertEquals("DAILY", DefaultCycles.DAILY.toString());
        assertEquals("WEEKLY", DefaultCycles.WEEKLY.toString());
        assertEquals("MONTHLY", DefaultCycles.MONTHLY.toString());
        assertEquals("YEARLY", DefaultCycles.YEARLY.toString());
    }

    @Test
    public void testDefaultCyclesOrdinal() {
        // Test that ordinals are consistent
        assertEquals(0, DefaultCycles.MINUTE.ordinal());
        assertEquals(1, DefaultCycles.HOURLY.ordinal());
        assertEquals(2, DefaultCycles.DAILY.ordinal());
        assertEquals(3, DefaultCycles.WEEKLY.ordinal());
        assertEquals(4, DefaultCycles.MONTHLY.ordinal());
        assertEquals(5, DefaultCycles.YEARLY.ordinal());
    }

    @Test
    public void testCycleFunctionInterface() {
        // Test custom CycleFunction implementation
        CycleFunction customCycle = time -> "test-" + time.getYear() + "-" + time.getMonthValue();
        
        LocalDateTime testTime = LocalDateTime.of(2024, 12, 25, 14, 30, 45);
        String result = customCycle.apply(testTime);
        assertEquals("test-2024-12", result);
    }

    @Test
    public void testCycleFunctionWithDifferentFormats() {
        // Test various custom cycle functions
        CycleFunction yearOnly = time -> String.valueOf(time.getYear());
        CycleFunction monthDay = time -> time.getMonthValue() + "-" + time.getDayOfMonth();
        CycleFunction hourMinute = time -> time.getHour() + ":" + time.getMinute();
        
        LocalDateTime testTime = LocalDateTime.of(2024, 6, 15, 14, 30, 45);
        
        assertEquals("2024", yearOnly.apply(testTime));
        assertEquals("6-15", monthDay.apply(testTime));
        assertEquals("14:30", hourMinute.apply(testTime));
    }

    @Test
    public void testNowFunctionInterface() {
        // Test custom NowFunction implementation
        LocalDateTime fixedTime = LocalDateTime.of(2024, 6, 15, 10, 30, 0);
        NowFunction fixedNow = () -> fixedTime;
        
        assertEquals(fixedTime, fixedNow.get());
    }

    @Test
    public void testNowFunctionWithCurrentTime() {
        // Test that default now function returns current time
        NowFunction defaultNow = LocalDateTime::now;
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime result = defaultNow.get();
        LocalDateTime after = LocalDateTime.now();
        
        assertTrue(result.isAfter(before.minusSeconds(1)));
        assertTrue(result.isBefore(after.plusSeconds(1)));
    }

    @Test
    public void testEnumEquality() {
        // Test enum equality
        assertEquals(UpdatePolicy.REPLACE, UpdatePolicy.REPLACE);
        assertNotEquals(UpdatePolicy.REPLACE, UpdatePolicy.BEST);
        
        assertEquals(SortPolicy.HIGH_TO_LOW, SortPolicy.HIGH_TO_LOW);
        assertNotEquals(SortPolicy.HIGH_TO_LOW, SortPolicy.LOW_TO_HIGH);
        
        assertEquals(DefaultCycles.DAILY, DefaultCycles.DAILY);
        assertNotEquals(DefaultCycles.DAILY, DefaultCycles.HOURLY);
    }

    @Test
    public void testEnumHashCode() {
        // Test that same enum values have same hash codes
        assertEquals(UpdatePolicy.REPLACE.hashCode(), UpdatePolicy.REPLACE.hashCode());
        assertEquals(SortPolicy.HIGH_TO_LOW.hashCode(), SortPolicy.HIGH_TO_LOW.hashCode());
        assertEquals(DefaultCycles.DAILY.hashCode(), DefaultCycles.DAILY.hashCode());
    }

    @Test
    public void testEnumCompareTo() {
        // Test enum natural ordering (by ordinal)
        assertTrue(DefaultCycles.MINUTE.compareTo(DefaultCycles.HOURLY) < 0);
        assertTrue(DefaultCycles.HOURLY.compareTo(DefaultCycles.DAILY) < 0);
        assertTrue(DefaultCycles.DAILY.compareTo(DefaultCycles.WEEKLY) < 0);
        assertTrue(DefaultCycles.WEEKLY.compareTo(DefaultCycles.MONTHLY) < 0);
        assertTrue(DefaultCycles.MONTHLY.compareTo(DefaultCycles.YEARLY) < 0);
        
        assertEquals(0, DefaultCycles.DAILY.compareTo(DefaultCycles.DAILY));
        
        assertTrue(UpdatePolicy.REPLACE.compareTo(UpdatePolicy.AGGREGATE) < 0);
        assertTrue(UpdatePolicy.AGGREGATE.compareTo(UpdatePolicy.BEST) < 0);
        
        assertTrue(SortPolicy.HIGH_TO_LOW.compareTo(SortPolicy.LOW_TO_HIGH) < 0);
    }

    @Test
    public void testEnumInSwitch() {
        // Test that enums work properly in switch statements
        String updateResult = switch (UpdatePolicy.REPLACE) {
            case REPLACE -> "replace";
            case AGGREGATE -> "aggregate";
            case BEST -> "best";
        };
        assertEquals("replace", updateResult);
        
        String sortResult = switch (SortPolicy.HIGH_TO_LOW) {
            case HIGH_TO_LOW -> "high-to-low";
            case LOW_TO_HIGH -> "low-to-high";
        };
        assertEquals("high-to-low", sortResult);
        
        String cycleResult = switch (DefaultCycles.DAILY) {
            case MINUTE -> "minute";
            case HOURLY -> "hourly";
            case DAILY -> "daily";
            case WEEKLY -> "weekly";
            case MONTHLY -> "monthly";
            case YEARLY -> "yearly";
        };
        assertEquals("daily", cycleResult);
    }

    @Test
    public void testFunctionalInterfaceCompatibility() {
        // Test that functional interfaces can be used with lambdas
        CycleFunction lambda1 = time -> "lambda-" + time.getYear();
        NowFunction lambda2 = () -> LocalDateTime.of(2024, 1, 1, 0, 0);
        
        LocalDateTime testTime = LocalDateTime.of(2024, 12, 25, 14, 30, 45);
        assertEquals("lambda-2024", lambda1.apply(testTime));
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0), lambda2.get());
    }

    @Test
    public void testFunctionalInterfaceWithMethodReferences() {
        // Test that functional interfaces work with method references
        NowFunction methodRef = LocalDateTime::now;
        assertNotNull(methodRef.get());
        
        // Custom method reference
        CycleFunction yearExtractor = time -> String.valueOf(time.getYear());
        LocalDateTime testTime = LocalDateTime.of(2024, 12, 25, 14, 30, 45);
        assertEquals("2024", yearExtractor.apply(testTime));
    }

    // Helper method to check if array contains element
    private <T> boolean contains(T[] array, T element) {
        for (T item : array) {
            if (item.equals(element)) {
                return true;
            }
        }
        return false;
    }
}