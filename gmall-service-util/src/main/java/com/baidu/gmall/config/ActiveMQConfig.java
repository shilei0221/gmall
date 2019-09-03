package com.baidu.gmall.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

import javax.jms.Session;

/**
 * @author Alei
 * @create 2019-09-01 20:06
 */
@Configuration
public class ActiveMQConfig {

    @Value("${spring.activemq.broker-url:disabled}")
    String brokerURL;

    @Value("${activemq.listener.enable:disabled}")
    String listenerEnable;

    /**
     * 发送队列 初始化队列
     * @return
     */
    @Bean
    public ActiveMQUtil getActiveMQUtil() {
        //判断如果等于默认值 返回空
        if ("disabled".equals(brokerURL)) {
            return null;
        }

        //进行初始化 返回结果
        ActiveMQUtil activeMQUtil = new ActiveMQUtil();
        activeMQUtil.init(brokerURL);
        return activeMQUtil;
    }

    /**
     * 创建监听的工厂  监听哪个连接工厂
     * @param activeMQConnectionFactory 连接工厂对象 需要注入到容器中
     * @return
     */
    @Bean(name = "jmsQueueListener")
    public DefaultJmsListenerContainerFactory jmsQueueListenerContainerFactory(ActiveMQConnectionFactory activeMQConnectionFactory){

        if ("disabled".equals(listenerEnable)) {
            return  null;
        }

        //创建对象 默认的监听器工厂
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();

        //指定使用哪个连接工厂 此工厂必须注入容器中 才能获取到
        factory.setConnectionFactory(activeMQConnectionFactory);

        //设置事务
        factory.setSessionTransacted(false);
        //自动签收
        factory.setSessionAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
        //设置并发数
        factory.setConcurrency("5");
        //设置获取不到消息时重连间隔时间
        factory.setRecoveryInterval(5000L);

        return factory;
    }

    /**
     * 接收消息  将连接工厂注入到容器中 可以让上面监听方法 作为参数获取到连接工厂
     * @return
     */
    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactory() {

        //创建连接工厂对象
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(brokerURL);

        return activeMQConnectionFactory;
    }
}
