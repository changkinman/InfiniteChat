if redis.call('HGET', KEYS[1], 'connectionId') == ARGV[1] then
  return redis.call('PEXPIRE', KEYS[1], ARGV[2])
end
return 0
