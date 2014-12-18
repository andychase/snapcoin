package TemporaryStorage

import java.net.URI

import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

class RedisStorage(private val url:URI) extends TemporaryStorage {
    private val pool = new JedisPool(new JedisPoolConfig(), url)

    protected def withJedis(func:(Jedis => Any)): Any = {
        var jedis:Jedis = null
        try {
            jedis = pool.getResource
            func(jedis)
        } finally {
            if (jedis != null)
                jedis.close()
        }
    }

    protected def put(key: String, value: String): Unit = {
        withJedis (_.setex(key, expireTimeSeconds, value))
    }

    protected def get(key: String): Option[String] = {
        withJedis ({ jedis =>
            val value = jedis.get(key)
            jedis.del(key)
            value
        }) match {
            case s:String => Some(s)
            case _ => None
        }
    }
}
