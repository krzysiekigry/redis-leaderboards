local ps = redis.call('zscore', KEYS[1], ARGV[2]);
if not ps or tonumber(ARGV[1]) > tonumber(ps) then
    redis.call('zadd', KEYS[1], ARGV[1], ARGV[2])
    return tonumber(ARGV[1])
end
return tonumber(ps)