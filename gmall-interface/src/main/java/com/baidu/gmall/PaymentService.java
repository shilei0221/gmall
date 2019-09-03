package com.baidu.gmall;

import com.baidu.gmall.bean.PaymentInfo;

import java.util.Map;

/**
 * @author Alei
 * @create 2019-08-31 19:27
 *
 * 支付接口
 */
public interface PaymentService {


    /**
     * 保存支付订单
     * @param paymentInfo
     */
    void savePaymentInfo(PaymentInfo paymentInfo);

    /**
     * 查询支付信息
     * @param paymentInfo
     * @return
     */
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    /**
     * 通过第三方交易编号修改支付状态
     * @param out_trade_no
     * @param paymentInfoUPD
     */
    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUPD);

    /**
     * 退款功能实现
     * @param orderId
     * @return
     */
    boolean refund(String orderId);

    /**
     * 生成二维码微信支付
     * @param orderId
     * @param total_fee
     * @return
     */
    Map createNative(String orderId, String total_fee);

    /**
     * 发送消息队列来通知订单修改状态
     * @param paymentInfo
     * @param result
     *
     * 接口：发送消息，给activemq 支付结果！success,fail
     * 发送一个orderId，result 【success,fail】
     */
    void sendPaymentResult(PaymentInfo paymentInfo, String result);


    /**
     *  检查订单状态是否支付成功
     * @param paymentInfoQuery
     * @return
     */
    boolean checkPayment(PaymentInfo paymentInfoQuery);

    /**
     *  发送延迟队列给订单  定时去检查订单的支付状态   延迟队列反复调用
     * @param outTradeNo 第三方支付编号
     * @param delaySec  每次检查时的间隔时间
     * @param checkCount 检查支付状态的次数是几次
     */
    void sendDelayPaymentResult(String outTradeNo,int delaySec ,int checkCount);

    /**
     *  根据订单id  关闭过期订单
     * @param orderId
     */
    void closePayment(String orderId);
}
