package com.baidu.gmall.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baidu.gmall.OrderService;
import com.baidu.gmall.bean.enums.ProcessStatus;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

/**
 * @author Alei
 * @create 2019-09-01 20:41
 *
 * 消息队列消费者 消费支付成功后发生的修改订单信息 进行修改订单状态信息  修改成功并发送给库存进行减库存
 */
@Component
public class OrderConsumer {

    @Reference
    private OrderService orderService;


    /*
        指定消费的队列名称是哪一个队列中的消息  和监听连接工厂是谁
     */
    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage) {

        try {
            //获取消息对象中的订单id 与 返回结果
            String orderId = mapMessage.getString("orderId");
            String result = mapMessage.getString("result");

            //判断返回结果是否成功
            if ("success".equals(result)) {
                //如果返回成功 则修改订单状态 为已支付
                orderService.updateOrderStatus(orderId,ProcessStatus.PAID);

                //通知库存 进行减库存
                orderService.sendOrderStatus(orderId);

                //减完库存成功 将订单改为代发货 已通知仓库
                orderService.updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);
            } else {
                //将订单改为未支付
                orderService.updateOrderStatus(orderId,ProcessStatus.UNPAID);
            }

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    /**
     * 消费库存返回的结果 修改订单状态
     * @param mapMessage
     */
    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) {

        try {
            //获取提供者放入的消息
            String orderId = mapMessage.getString("orderId");
            String status = mapMessage.getString("status");

            //判断状态是否是DEDUCTED
            if ("DEDUCTED".equals(status)) {
                orderService.updateOrderStatus(orderId,ProcessStatus.DELEVERED);
            }else {
                orderService.updateOrderStatus(orderId,ProcessStatus.STOCK_EXCEPTION);
            }

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

}
