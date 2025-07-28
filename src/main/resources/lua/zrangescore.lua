local c = redis.call('zrangebyscore', KEYS[1], ARGV[1], ARGV[2], 'WITHSCORES');
if #c > 0 then
    local r = redis.call('zrank', KEYS[1], c[1]);
    return { r, c }
else
    return { -1, {} }
end