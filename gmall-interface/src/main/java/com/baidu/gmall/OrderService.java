package com.baidu.gmall;

import com.baidu.gmall.bean.OrderInfo;
import com.baidu.gmall.bean.enums.ProcessStatus;

/**
 * @author Alei
 * @create 2019-08-30 19:25
 *
 * 订单接口
 */
public interface OrderService {

    /**
     * 保存订单信息 以及 订单明细 下订单
     *
     * 保存完成去调用支付 所以返回orderId
     * @param orderInfo
     * @return
     */
    String saveOrder(OrderInfo orderInfo);

    /**
     * 生成一个第三方交易编号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 判断页面与缓存中的第三方交易编号是否一致 一致则提交 不一致则提示信息
     * @param userId
     * @param tradeCodeNo
     * @return
     */
    boolean checkTradeCode(String userId,String tradeCodeNo);

    /**
     * 删除某个用户的第三方交易编号 使其避免表单重复提交
     * @param userId
     */
    void delTradeCode(String userId);

    /**
     * 验证库存中商品是否足够
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(String skuId,Integer skuNum);

    /**
     * 根据订单id 查询订单信息
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(String orderId);

    /**
     * 修改订单状态为已支付
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(String orderId, ProcessStatus processStatus);

    /**
     * 发送消息给库存 修改订单状态
     * @param orderId
     */
    void sendOrderStatus(String orderId);
}
