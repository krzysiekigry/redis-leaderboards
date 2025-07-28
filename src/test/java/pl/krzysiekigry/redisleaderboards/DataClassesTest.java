package pl.krzysiekigry.redisleaderboards;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class DataClassesTest {

    @Test
    public void testEntryCreation() {
        Entry<Integer> entry = new Entry<>("player1", 100, 5L);
        
        assertEquals("player1", entry.id());
        assertEquals(100, entry.score());
        assertEquals(5L, entry.rank());
    }

    @Test
    public void testEntryEquality() {
        Entry<Integer> entry1 = new Entry<>("player1", 100, 5L);
        Entry<Integer> entry2 = new Entry<>("player1", 100, 5L);
        Entry<Integer> entry3 = new Entry<>("player2", 100, 5L);
        Entry<Integer> entry4 = new Entry<>("player1", 200, 5L);
        Entry<Integer> entry5 = new Entry<>("player1", 100, 3L);

        assertEquals(entry1, entry2);
        assertNotEquals(entry1, entry3);
        assertNotEquals(entry1, entry4);
        assertNotEquals(entry1, entry5);
        
        assertEquals(entry1.hashCode(), entry2.hashCode());
    }

    @Test
    public void testEntryToString() {
        Entry<Integer> entry = new Entry<>("player1", 100, 5L);
        String toString = entry.toString();
        
        assertTrue(toString.contains("player1"));
        assertTrue(toString.contains("100"));
        assertTrue(toString.contains("5"));
    }

    @Test
    public void testEntryWithDifferentTypes() {
        Entry<Double> doubleEntry = new Entry<>("player1", 99.99, 1L);
        Entry<Long> longEntry = new Entry<>("player2", 1000000000L, 2L);
        
        assertEquals(99.99, doubleEntry.score());
        assertEquals(1000000000L, longEntry.score());
    }

    @Test
    public void testEntryUpdateQueryCreation() {
        EntryUpdateQuery<Integer> query = new EntryUpdateQuery<>("player1", 100);
        
        assertEquals("player1", query.id());
        assertEquals(100, query.value());
    }

    @Test
    public void testEntryUpdateQueryEquality() {
        EntryUpdateQuery<Integer> query1 = new EntryUpdateQuery<>("player1", 100);
        EntryUpdateQuery<Integer> query2 = new EntryUpdateQuery<>("player1", 100);
        EntryUpdateQuery<Integer> query3 = new EntryUpdateQuery<>("player2", 100);
        EntryUpdateQuery<Integer> query4 = new EntryUpdateQuery<>("player1", 200);

        assertEquals(query1, query2);
        assertNotEquals(query1, query3);
        assertNotEquals(query1, query4);
        
        assertEquals(query1.hashCode(), query2.hashCode());
    }

    @Test
    public void testEntryUpdateQueryToString() {
        EntryUpdateQuery<Integer> query = new EntryUpdateQuery<>("player1", 100);
        String toString = query.toString();
        
        assertTrue(toString.contains("player1"));
        assertTrue(toString.contains("100"));
    }

    @Test
    public void testEntryUpdateQueryWithDifferentTypes() {
        EntryUpdateQuery<Double> doubleQuery = new EntryUpdateQuery<>("player1", 99.99);
        EntryUpdateQuery<Long> longQuery = new EntryUpdateQuery<>("player2", 1000000000L);
        
        assertEquals(99.99, doubleQuery.value());
        assertEquals(1000000000L, longQuery.value());
    }

    @Test
    public void testLeaderboardOptionsCreation() {
        LeaderboardOptions options = new LeaderboardOptions(
                SortPolicy.HIGH_TO_LOW, 
                UpdatePolicy.REPLACE, 
                100
        );
        
        assertEquals(SortPolicy.HIGH_TO_LOW, options.sortPolicy());
        assertEquals(UpdatePolicy.REPLACE, options.updatePolicy());
        assertEquals(100, options.limitTopN());
    }

    @Test
    public void testLeaderboardOptionsEquality() {
        LeaderboardOptions options1 = new LeaderboardOptions(
                SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100);
        LeaderboardOptions options2 = new LeaderboardOptions(
                SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100);
        LeaderboardOptions options3 = new LeaderboardOptions(
                SortPolicy.LOW_TO_HIGH, UpdatePolicy.REPLACE, 100);
        LeaderboardOptions options4 = new LeaderboardOptions(
                SortPolicy.HIGH_TO_LOW, UpdatePolicy.BEST, 100);
        LeaderboardOptions options5 = new LeaderboardOptions(
                SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 50);

        assertEquals(options1, options2);
        assertNotEquals(options1, options3);
        assertNotEquals(options1, options4);
        assertNotEquals(options1, options5);
        
        assertEquals(options1.hashCode(), options2.hashCode());
    }

    @Test
    public void testLeaderboardOptionsToString() {
        LeaderboardOptions options = new LeaderboardOptions(
                SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100);
        String toString = options.toString();
        
        assertTrue(toString.contains("HIGH_TO_LOW"));
        assertTrue(toString.contains("REPLACE"));
        assertTrue(toString.contains("100"));
    }

    @Test
    public void testPeriodicLeaderboardOptionsCreationWithDefaultNow() {
        LeaderboardOptions lbOptions = new LeaderboardOptions(
                SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100);
        
        PeriodicLeaderboardOptions periodicOptions = new PeriodicLeaderboardOptions(
                lbOptions, DefaultCycles.DAILY);
        
        assertEquals(lbOptions, periodicOptions.leaderboardOptions());
        assertEquals(DefaultCycles.DAILY, periodicOptions.cycle());
        assertNotNull(periodicOptions.now());
        
        // Test that default now function works
        LocalDateTime now = periodicOptions.now().get();
        assertNotNull(now);
    }

    @Test
    public void testPeriodicLeaderboardOptionsCreationWithCustomNow() {
        LeaderboardOptions lbOptions = new LeaderboardOptions(
                SortPolicy.LOW_TO_HIGH, UpdatePolicy.BEST, 50);
        LocalDateTime fixedTime = LocalDateTime.of(2024, 6, 15, 10, 30, 0);
        NowFunction customNow = () -> fixedTime;
        
        PeriodicLeaderboardOptions periodicOptions = new PeriodicLeaderboardOptions(
                lbOptions, DefaultCycles.HOURLY, customNow);
        
        assertEquals(lbOptions, periodicOptions.leaderboardOptions());
        assertEquals(DefaultCycles.HOURLY, periodicOptions.cycle());
        assertEquals(customNow, periodicOptions.now());
        assertEquals(fixedTime, periodicOptions.now().get());
    }

    @Test
    public void testPeriodicLeaderboardOptionsWithCustomCycle() {
        LeaderboardOptions lbOptions = new LeaderboardOptions(
                SortPolicy.HIGH_TO_LOW, UpdatePolicy.AGGREGATE, 200);
        CycleFunction customCycle = time -> "custom-" + time.getYear();
        
        PeriodicLeaderboardOptions periodicOptions = new PeriodicLeaderboardOptions(
                lbOptions, customCycle);
        
        assertEquals(lbOptions, periodicOptions.leaderboardOptions());
        assertEquals(customCycle, periodicOptions.cycle());
        
        // Test the custom cycle function
        LocalDateTime testTime = LocalDateTime.of(2024, 12, 25, 14, 30, 45);
        assertEquals("custom-2024", ((CycleFunction) periodicOptions.cycle()).apply(testTime));
    }

    @Test
    public void testPeriodicLeaderboardOptionsEquality() {
        LeaderboardOptions lbOptions = new LeaderboardOptions(
                SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100);
        NowFunction nowFunc = LocalDateTime::now;
        
        PeriodicLeaderboardOptions options1 = new PeriodicLeaderboardOptions(
                lbOptions, DefaultCycles.DAILY, nowFunc);
        PeriodicLeaderboardOptions options2 = new PeriodicLeaderboardOptions(
                lbOptions, DefaultCycles.DAILY, nowFunc);
        PeriodicLeaderboardOptions options3 = new PeriodicLeaderboardOptions(
                lbOptions, DefaultCycles.HOURLY, nowFunc);

        assertEquals(options1, options2);
        assertNotEquals(options1, options3);
        
        assertEquals(options1.hashCode(), options2.hashCode());
    }

    @Test
    public void testPeriodicLeaderboardOptionsToString() {
        LeaderboardOptions lbOptions = new LeaderboardOptions(
                SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, 100);
        PeriodicLeaderboardOptions periodicOptions = new PeriodicLeaderboardOptions(
                lbOptions, DefaultCycles.DAILY);
        
        String toString = periodicOptions.toString();
        assertTrue(toString.contains("DAILY"));
    }

    @Test
    public void testNullValues() {
        // Test that records can be created with null values (records don't enforce non-null by default)
        Entry<Integer> nullIdEntry = new Entry<>(null, 100, 1L);
        assertNull(nullIdEntry.id());
        assertEquals(100, nullIdEntry.score());
        
        EntryUpdateQuery<Integer> nullIdQuery = new EntryUpdateQuery<>(null, 100);
        assertNull(nullIdQuery.id());
        assertEquals(100, nullIdQuery.value());
        
        // LeaderboardOptions with null sortPolicy should be allowed at construction
        // but may cause issues when used
        LeaderboardOptions nullSortOptions = new LeaderboardOptions(null, UpdatePolicy.REPLACE, 100);
        assertNull(nullSortOptions.sortPolicy());
    }

    @Test
    public void testEdgeCaseValues() {
        // Test with edge case values
        Entry<Integer> maxEntry = new Entry<>("player", Integer.MAX_VALUE, Long.MAX_VALUE);
        Entry<Integer> minEntry = new Entry<>("player", Integer.MIN_VALUE, Long.MIN_VALUE);
        
        assertEquals(Integer.MAX_VALUE, maxEntry.score());
        assertEquals(Long.MAX_VALUE, maxEntry.rank());
        assertEquals(Integer.MIN_VALUE, minEntry.score());
        assertEquals(Long.MIN_VALUE, minEntry.rank());
        
        EntryUpdateQuery<Double> doubleQuery = new EntryUpdateQuery<>("player", Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, doubleQuery.value());
        
        LeaderboardOptions options = new LeaderboardOptions(
                SortPolicy.HIGH_TO_LOW, UpdatePolicy.REPLACE, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, options.limitTopN());
    }

    @Test
    public void testEmptyStrings() {
        // Test with empty strings
        Entry<Integer> emptyIdEntry = new Entry<>("", 100, 1L);
        assertEquals("", emptyIdEntry.id());
        
        EntryUpdateQuery<Integer> emptyIdQuery = new EntryUpdateQuery<>("", 100);
        assertEquals("", emptyIdQuery.id());
    }

    @Test
    public void testSpecialCharacters() {
        // Test with special characters in IDs
        String specialId = "player@#$%^&*()_+-=[]{}|;':\",./<>?`~";
        Entry<Integer> specialEntry = new Entry<>(specialId, 100, 1L);
        assertEquals(specialId, specialEntry.id());
        
        EntryUpdateQuery<Integer> specialQuery = new EntryUpdateQuery<>(specialId, 100);
        assertEquals(specialId, specialQuery.id());
    }

    @Test
    public void testUnicodeCharacters() {
        // Test with Unicode characters
        String unicodeId = "玩家1_🎮_测试";
        Entry<Integer> unicodeEntry = new Entry<>(unicodeId, 100, 1L);
        assertEquals(unicodeId, unicodeEntry.id());
        
        EntryUpdateQuery<Integer> unicodeQuery = new EntryUpdateQuery<>(unicodeId, 100);
        assertEquals(unicodeId, unicodeQuery.id());
    }
}