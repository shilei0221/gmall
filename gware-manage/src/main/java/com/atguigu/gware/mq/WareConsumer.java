package com.atguigu.gware.mq;

import com.alibaba.fastjson.JSON;
import com.atguigu.gware.bean.WareOrderTask;
import com.atguigu.gware.config.ActiveMQUtil;
import com.atguigu.gware.enums.TaskStatus;
import com.atguigu.gware.mapper.WareOrderTaskDetailMapper;
import com.atguigu.gware.mapper.WareOrderTaskMapper;
import com.atguigu.gware.mapper.WareSkuMapper;
import com.atguigu.gware.service.GwareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.util.List;

/**
 * @param
 * @return
 */
@Component
public class WareConsumer {


    @Autowired
    WareOrderTaskMapper wareOrderTaskMapper;

    @Autowired
    WareOrderTaskDetailMapper wareOrderTaskDetailMapper;

    @Autowired
    WareSkuMapper wareSkuMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    JmsTemplate jmsTemplate;


    @Autowired
    GwareService gwareService;


    /**
     * 消费者进行消费消息 该订单状态
     * @param textMessage
     * @throws JMSException
     */
    @JmsListener(destination = "ORDER_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void receiveOrder(TextMessage textMessage) throws JMSException {
        //获取提供者发送的消息
        String orderTaskJson = textMessage.getText();
        //将消息转换为对象
        WareOrderTask wareOrderTask = JSON.parseObject(orderTaskJson, WareOrderTask.class);
        //设置订单状态为已支付
        wareOrderTask.setTaskStatus(TaskStatus.PAID);
        //
        gwareService.saveWareOrderTask(wareOrderTask);
        textMessage.acknowledge();


        List<WareOrderTask> wareSubOrderTaskList = gwareService.checkOrderSplit(wareOrderTask);
        if (wareSubOrderTaskList != null && wareSubOrderTaskList.size() >= 2) {
            for (WareOrderTask orderTask : wareSubOrderTaskList) {
                gwareService.lockStock(orderTask);
            }
        } else {
            gwareService.lockStock(wareOrderTask);
        }


    }





}
