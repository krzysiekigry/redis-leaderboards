return {
    redis.call('zscore', KEYS[1], ARGV[1]),
    redis.call('zrank', KEYS[1], ARGV[1])
}