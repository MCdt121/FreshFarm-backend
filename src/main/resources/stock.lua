-- KEYS[1]: 库存的 key (stock:productId)
-- ARGV[1]: 扣减的数量 (quantity)

-- 获取当前库存
local stock = tonumber(redis.call('get', KEYS[1]))

-- 如果库存不存在，查库预热，
if stock == nil then
    return -1
end

-- 判断库存是否充足
local quantity = tonumber(ARGV[1])
if stock >= quantity then
    -- 库存充足，执行扣减
    redis.call('decrby', KEYS[1], quantity)
    return 1 -- 扣减成功
else
    -- 库存不足
    return 0 -- 扣减失败
end