package com.baidu.gmall.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author Alei
 * @create 2019-08-23 10:04
 *
 * redis 工具类
 */
public class RedisUtil {

    /*
        1.创建 redis 连接池 jedisPool
        2. 初始化连接池
        3.创建jedis 对象
     */
    private JedisPool jedisPool;

    /**
     * 初始化 连接池
     * @param host  ip地址
     * @param port 端口号
     * @param timeOut 超时时间
     * @param dataBase 默认使用的库
     */
    public void initJedisPoolConfig(String host,int port,int timeOut,int dataBase) {

        //创建 jedisPool配置类
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();

        //设置连接池参数

        //开启自检功能  就是在获取jedis 时检查是否有效 是否可以使用
        jedisPoolConfig.setTestOnBorrow(true);

        //设置连接池最大连接数
        jedisPoolConfig.setMaxTotal(200);

        //设置获取连接时等待的最大毫秒
        jedisPoolConfig.setMaxWaitMillis(10*1000);

        //设置连接池在高并发下 最少剩余数量
        jedisPoolConfig.setMinIdle(10);

        //如果到最大数 设置等待
        jedisPoolConfig.setBlockWhenExhausted(true);

        jedisPool = new JedisPool(jedisPoolConfig,host,port,timeOut);

    }

    /**
     * 创建 jedis 对象
     * @return
     */
    public Jedis getJedis() {

        return jedisPool.getResource();
    }
}
