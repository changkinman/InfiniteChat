local oldNodeId = redis.call('HGET', KEYS[1], 'nodeId') or ''
local oldEndpoint = redis.call('HGET', KEYS[1], 'endpoint') or ''
local oldConnectionId = redis.call('HGET', KEYS[1], 'connectionId') or ''
redis.call('HSET', KEYS[1],
  'nodeId', ARGV[1],
  'endpoint', ARGV[2],
  'connectionId', ARGV[3])
redis.call('PEXPIRE', KEYS[1], ARGV[4])
return {oldNodeId, oldEndpoint, oldConnectionId}
