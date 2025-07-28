local function aroundRange(path, id, distance, fill_borders, sort_dir)
    local r = redis.call((sort_dir == 'low-to-high') and 'zrank' or 'zrevrank', path, id)
    if not r then return { -1, -1, -1, -1 } end

    local c = redis.call('zcard', path)
    local l = math.max(0, r - distance)
    local h = (fill_borders == 'true') and math.min(c, r + distance) or math.max(0, h - 2 * distance - 1)
    return { l, h, c, r }
end

local range = aroundRange(KEYS[1], ARGV[1], ARGV[2], ARGV[3], ARGV[4])
if range[1] == -1 then return { 0, {} } end
return { range[1], redis.call((ARGV[4] == 'low-to-high') and 'zrange' or 'zrevrange', KEYS[1], range[1], range[2], 'WITHSCORES') }