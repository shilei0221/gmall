package com.baidu.gmall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Alei
 * @create 2019-08-23 10:15
 *
 * redis 配置类
 */
@Configuration
public class RedisConfig {

    //读取配置文件中的redis的ip地址 如果没有使用默认的
    @Value("${spring.redis.host:disabled}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    @Value("${spring.redis.database}")
    private int database;

    @Value("${spring.redis.timeout}")
    private int timeOut;

    @Bean
    public RedisUtil getRedisUtil() {
        if ("disabled".equals(host)) {
            return null;
        }

        RedisUtil redisUtil = new RedisUtil();

        redisUtil.initJedisPoolConfig(host,port,timeOut,database);

        return redisUtil;
    }

}
