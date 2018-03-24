package com.zys.jmeter.protocol.redis.config;


import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jmeter.testbeans.TestBean;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import redis.clients.util.Pool;

/**
 * Created by 01369755 on 2018/3/17.
 */
public class RedisConfig extends ConfigTestElement implements TestBean, TestStateListener {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    private String redisName;
    private String address;
    private String master;
    private String password;
    private Boolean sentinel;

    private static JedisPoolConfig CONFIG = new JedisPoolConfig();

    private static int TIMEOUT = 0;

    public static ConcurrentHashMap<String, Pool<Jedis>> jedisMap = new ConcurrentHashMap<>();

    private JedisPool initJedisPool(){
        String host = address.split(",")[0];
        return new JedisPool(CONFIG, host.split(":")[0], Integer.parseInt(host.split(":")[1]), TIMEOUT, password);
    }

    private JedisSentinelPool initJedisSentinelPool(){
        String[] hosts = address.split(",");
        Set<String> sentinels = new HashSet<>();
        for (String sentinel : hosts) {
            sentinels.add(sentinel);
        }
        return new JedisSentinelPool(master, sentinels, CONFIG, TIMEOUT, password);
    }
    public static Pool<Jedis> getPool(String redisName){
        return jedisMap.get(redisName);
    }

    public void testStarted(String s) {
        testStarted();
    }

    public void testStarted() {
        if (sentinel){
            jedisMap.put(redisName, initJedisSentinelPool());
        }else {
            jedisMap.put(redisName, initJedisPool());
        }

        log.info(redisName + "initialed!");
    }

    public void testEnded(String s) {
        testEnded();
    }

    public void testEnded() {
        jedisMap.get(redisName).destroy();
        jedisMap.remove(redisName);
        log.info(redisName + "removed!");
    }

    public String getRedisName() {
        return redisName;
    }

    public void setRedisName(String redisName) {
        this.redisName = redisName;
    }
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMaster() {
        return master;
    }

    public void setMaster(String master) {
        this.master = master;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getSentinel() {
        return sentinel;
    }

    public void setSentinel(Boolean sentinel) {
        this.sentinel = sentinel;
    }

}