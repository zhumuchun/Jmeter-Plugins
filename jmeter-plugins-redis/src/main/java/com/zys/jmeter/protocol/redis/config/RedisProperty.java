package com.zys.jmeter.protocol.redis.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by zhuyongsheng on 2019/1/7.
 */
public class RedisProperty {

    private static final JedisPoolConfig CONFIG = new JedisPoolConfig();
    private static final int TIMEOUT = 6000;
    private static final int SO_TIMEOUT = 2000;
    private static final int MAX_ATTEMPTS = 3;
    private static final String BAD_MODE = "unsupported mode.";
    private static final String NOT_FOUND = "no key found.";
    private static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();


    private JedisCluster jedisCluster;
    private JedisPool jedisPool;
    private JedisSentinelPool jedisSentinelPool;

    public RedisProperty(String address, String password) {
        this.jedisPool = new JedisPool(CONFIG, StringUtils.substringBefore(address, ":"), Integer.parseInt(StringUtils.substringAfter(address, ":")), TIMEOUT, password);
    }

    public RedisProperty(String masterName, Set<String> sentinels, String password) {
        this.jedisSentinelPool = new JedisSentinelPool(masterName, sentinels, CONFIG, TIMEOUT, password);
    }

    public RedisProperty(String[] addrs, String password) {
        Set<HostAndPort> nodes = new HashSet<>();
        for (String addr : addrs) {
            nodes.add(new HostAndPort(StringUtils.substringBefore(addr, ":"), Integer.parseInt(StringUtils.substringAfter(addr, ":"))));
        }
        this.jedisCluster = new JedisCluster(nodes, TIMEOUT, SO_TIMEOUT, MAX_ATTEMPTS, password, new GenericObjectPoolConfig());
    }

    public String create(String key, String value) {
        String result;
        if (null != jedisPool) {
            Jedis jedis = jedisPool.getResource();
            result = jedis.setnx(key, value).toString();
            jedis.close();
            return result;
        } else if (null != jedisSentinelPool) {
            Jedis jedis = jedisSentinelPool.getResource();
            result = jedis.setnx(key, value).toString();
            jedis.close();
            return result;
        } else if (null != jedisCluster) {
            result = jedisCluster.setnx(key, value).toString();
            return result;
        }
        return BAD_MODE;
    }

    public String delete(String key) {
        String result;
        if (null != jedisPool) {
            Jedis jedis = jedisPool.getResource();
            result = jedis.del(key).toString();
            jedis.close();
            return result;
        } else if (null != jedisSentinelPool) {
            Jedis jedis = jedisSentinelPool.getResource();
            result = jedis.del(key).toString();
            jedis.close();
            return result;
        } else if (null != jedisCluster) {
            result = jedisCluster.del(key).toString();
            return result;
        }
        return BAD_MODE;
    }

    public String update(String key, String value) {
        String result;
        if (null != jedisPool) {
            Jedis jedis = jedisPool.getResource();
            result = jedis.set(key, value);
            jedis.close();
            return result;
        } else if (null != jedisSentinelPool) {
            Jedis jedis = jedisSentinelPool.getResource();
            result = jedis.set(key, value);
            jedis.close();
            return result;
        } else if (null != jedisCluster) {
            result = jedisCluster.set(key, value);
            return result;
        }
        return BAD_MODE;
    }

    public String ttl(String key) {
        String result;
        if (null != jedisPool) {
            Jedis jedis = jedisPool.getResource();
            result = jedis.ttl(key).toString();
            jedis.close();
            return result;
        } else if (null != jedisSentinelPool) {
            Jedis jedis = jedisSentinelPool.getResource();
            result = jedis.ttl(key).toString();
            jedis.close();
            return result;
        } else if (null != jedisCluster) {
            result = jedisCluster.ttl(key).toString();
            return result;
        }
        return BAD_MODE;
    }

    public String read(String key) throws Exception {
        String result;
        if (null != jedisPool) {
            Jedis jedis = jedisPool.getResource();
            result = read(jedis, key);
            jedis.close();
            return result;
        } else if (null != jedisSentinelPool) {
            Jedis jedis = jedisSentinelPool.getResource();
            result = read(jedis, key);
            jedis.close();
            return result;
        } else if (null != jedisCluster) {
            if (key.contains("*")) {
                result = "Redis Cluster does not support 'keys'.";
            } else {
                result = jedisCluster.get(key);
                if (StringUtils.isEmpty(result)) {
                    result = NOT_FOUND;
                }
            }
            return result;
        }
        return BAD_MODE;

    }

    private String get(Jedis jedis, String key) {
        byte[] bytes = null;
        try {
            bytes = jedis.get(key.getBytes());
            ObjectInputStream oi = new ObjectInputStream(new ByteArrayInputStream(bytes));
            return GSON.toJson(oi.readObject());
        } catch (Exception e) {
            if (bytes != null) {
                return new String(bytes, Charsets.UTF_8);
            }
        }
        return null;
    }

    private String read(Jedis jedis, String key) {
        StringBuilder sb = new StringBuilder();
        jedis.keys(key).forEach(k -> sb.append(k).append(" : ").append(get(jedis, k)).append("\n"));
        if (sb.length() > 0) {
            return sb.deleteCharAt(sb.length() - 1).toString();
        }
        return NOT_FOUND;
    }

    public void destroy() throws Exception {
        if (null != jedisPool) {
            jedisPool.destroy();
        } else if (null != jedisSentinelPool) {
            jedisSentinelPool.destroy();
        } else if (null != jedisCluster) {
            jedisCluster.close();
        }
    }
}

