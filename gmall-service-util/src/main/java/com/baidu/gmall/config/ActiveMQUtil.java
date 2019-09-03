package com.baidu.gmall.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;

/**
 * @author Alei
 * @create 2019-09-01 20:06
 *
 * activeMQ 消息队列 工具类
 */
public class ActiveMQUtil {

    //定义 连接工厂池
    PooledConnectionFactory pooledConnectionFactory = null;

    /**
     * 初始化连接工厂
     * @param brokerUrl
     */
    public void init(String brokerUrl) {
        //创建activeMq连接工厂
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(brokerUrl);

        //给连接池赋值 创建
        pooledConnectionFactory = new PooledConnectionFactory(activeMQConnectionFactory);

        //设置超时时间
        pooledConnectionFactory.setExpiryTimeout(2000);
        //设置出现异常的时候,继续重试连接
        pooledConnectionFactory.setReconnectOnException(true);
        //设置最大连接数
        pooledConnectionFactory.setMaxConnections(5);
    }

    /**
     * 获取连接
     * @return
     */
    public Connection getConnection() {
        Connection connection = null;

        try {
            connection = pooledConnectionFactory.createConnection();
        } catch (JMSException e) {
            e.printStackTrace();
        }
        return connection;
    }
}
