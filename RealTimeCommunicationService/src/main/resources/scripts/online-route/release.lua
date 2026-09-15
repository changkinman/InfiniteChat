if redis.call('HGET', KEYS[1], 'connectionId') == ARGV[1] then
  return redis.call('DEL', KEYS[1])
end
return 0
