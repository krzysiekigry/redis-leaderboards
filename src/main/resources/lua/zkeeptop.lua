local c = redis.call('zcard', KEYS[1]);
local n = tonumber(ARGV[1])
local dif = c - n
if dif > 0 then
    redis.call('zremrangebyrank', KEYS[1], 0, dif - 1)
end