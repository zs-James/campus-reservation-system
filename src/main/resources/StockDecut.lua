--KEYS[1] 是库存的key，比如"stock:venue:1:date:2026-11-15:slot:1"
--ARGV[1] 是扣减的数量，通常是1

local stock = tonumber(redis.call("get", KEYS[1]))
if stock == nil then
    -- 库存不存在(没初始化)
    return -1
end

if stock >=tonumber(ARGV[1]) then
    -- 扣减成功
    redis.call("decrby", KEYS[1], ARGV[1])
    return 1
else
    -- 库存不足
    return 0
end
