# Redis Leaderboards

A simple yet powerful Java library for creating and managing leaderboards using Redis. Designed for flexibility and ease of use, this library supports standard, periodic (daily, weekly, etc.), and top-N limited leaderboards. It leverages asynchronous operations using `CompletableFuture` for high performance.

## Features

-   **Standard Leaderboards:** Create simple leaderboards with flexible sorting policies.
-   **Periodic Leaderboards:** Automatically manage leaderboards for different time cycles (yearly, monthly, weekly, daily, hourly, minute).
-   **Asynchronous API:** All Redis operations are non-blocking, returning `CompletableFuture` for efficient integration.
-   **Flexible Update Policies:** Choose how scores are updated: `REPLACE`, `AGGREGATE`, or `BEST`.
-   **Top-N Limitation:** Automatically keep the leaderboard trimmed to a specific size (e.g., top 1000).
-   **Efficient Data Handling:** Includes an `ExportStream` for processing large leaderboards in batches.

## Usage

### Initializing the Redis Connection

First, you need to create a `RedisExtension` instance. This class manages the connection to your Redis server and loads necessary Lua scripts.

```java
import redis.clients.jedis.JedisPool;
import pl.krzysiekigry.redisleaderboards.RedisExtension;

// ...

JedisPool jedisPool = new JedisPool("localhost", 6379);
RedisExtension redisExtension = new RedisExtension(jedisPool);
redisExtension.prepare(); // Loads Lua scripts
```

### Standard Leaderboard

Create a `Leaderboard` instance to manage a single, non-periodic leaderboard.

```java
import pl.krzysiekigry.redisleaderboards.*;

// ...

LeaderboardOptions options = new LeaderboardOptions.Builder()
    .sortPolicy(SortPolicy.HIGH_TO_LOW)
    .updatePolicy(UpdatePolicy.REPLACE)
    .limitTopN(1000)
    .build();

Leaderboard<Long> gameScores = new Leaderboard<>(redisExtension, "game:scores", Long.class, options);

// Update a user's score
gameScores.updateOne("player1", 1500L).join();
gameScores.updateOne("player2", 2200L).join();

// Get the top 10 players
List<Entry<Long>> top10 = gameScores.top(10).join();
top10.forEach(entry -> {
    System.out.printf("Rank %d: %s - %d%n", entry.rank(), entry.id(), entry.value());
});

// Get a player's rank
Long player1Rank = gameScores.rank("player1").join();
System.out.println("Player1's rank: " + player1Rank);
```

### Periodic Leaderboard

Create a `PeriodicLeaderboard` to manage leaderboards that reset on a schedule.

```java
import pl.krzysiekigry.redisleaderboards.*;
import java.time.LocalDateTime;

// ...

PeriodicLeaderboardOptions periodicOptions = new PeriodicLeaderboardOptions.Builder()
    .cycle(DefaultCycles.DAILY)
    .leaderboardOptions(new LeaderboardOptions.Builder()
        .sortPolicy(SortPolicy.HIGH_TO_LOW)
        .build())
    .build();

PeriodicLeaderboard<Integer> dailyScores = new PeriodicLeaderboard<>(redisExtension, "daily:scores", Integer.class, periodicOptions);

// Get the leaderboard for the current day
Leaderboard<Integer> todaysLeaderboard = dailyScores.getLeaderboardNow();

// Update a score
todaysLeaderboard.updateOne("player3", 95).join();

// Get the leaderboard for a specific day
Leaderboard<Integer> yesterdaysLeaderboard = dailyScores.getLeaderboardAt(LocalDateTime.now().minusDays(1));
List<Entry<Integer>> top5Yesterday = yesterdaysLeaderboard.top(5).join();
```

## Contributing

Contributions are welcome! Please feel free to submit a pull request or open an issue.
