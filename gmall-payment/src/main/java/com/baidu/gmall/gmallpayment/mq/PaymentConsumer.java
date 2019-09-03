package com.baidu.gmall.gmallpayment.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baidu.gmall.PaymentService;
import com.baidu.gmall.bean.PaymentInfo;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

/**
 * @author Alei
 * @create 2019-09-02 16:55
 *
 * 接收延迟队列的消费端
 */
@Component
public class PaymentConsumer {

    @Reference
    private PaymentService paymentService; //注入业务层  因为要调用方法

    /**
     * 消费支付时候发送的检查订单状态的消息
     * @param mapMessage
     */
    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage) throws JMSException {

        //获取消息队列中的数据 获取第三方订单编号
        String outTradeNo = mapMessage.getString("outTradeNo");
        //获取每次发送消息时的间隔时间
        int delaySec = mapMessage.getInt("delaySec");
        //获取一共发送的次数
        int checkCount = mapMessage.getInt("checkCount");

        //创建一个paymentInfo
        PaymentInfo paymentInfo = new PaymentInfo();
        //设置第三方订单编号
        paymentInfo.setOutTradeNo(outTradeNo);
        //调用业务层方法获取支付订单信息
        PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(paymentInfo);

        //调用业务层检查支付状态的次数方法
        boolean result = paymentService.checkPayment(paymentInfoQuery);

        System.out.println("result: " + result);

        //如果结果返回false 说明未支付 次数大于零 说明检查次数还有
        if (!result && checkCount != 0) {
            //还需要进行检查支付状态  因为这样只执行了一次检查状态的方法 所以如果未支付 返回结果false的话 还需要继续检查，所以我们就继续调用
            //发送消息队列来发送检查消息 消费者继续消费 去看订单状态是否支付成功
            paymentService.sendDelayPaymentResult(outTradeNo,delaySec,checkCount - 1);
        }
    }
}
