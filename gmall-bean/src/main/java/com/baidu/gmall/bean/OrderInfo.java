package com.baidu.gmall.bean;

import com.baidu.gmall.bean.enums.OrderStatus;
import com.baidu.gmall.bean.enums.PaymentWay;
import com.baidu.gmall.bean.enums.ProcessStatus;
import lombok.Data;
import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author Alei
 * @create 2019-08-30 18:36
 */
@Data
public class OrderInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column
    private String consignee; //收件人名称

    @Column
    private String consigneeTel; //收件人电话


    @Column
    private BigDecimal totalAmount; //总价格

    @Column
    private OrderStatus orderStatus; //订单状态

    @Column
    private ProcessStatus processStatus; //进程状态


    @Column
    private String userId; //用户id

    @Column
    private PaymentWay paymentWay; //支付方式

    @Column
    private Date expireTime; //过期时间订单

    @Column
    private String deliveryAddress; //收货地址

    @Column
    private String orderComment; //订单状态

    @Column
    private Date createTime; //创建时间

    @Column
    private String parentOrderId; //拆单时的初次的订单id 父id

    @Column
    private String trackingNo; //物流编号


    @Transient
    private List<OrderDetail> orderDetailList; //订单详情集合


    @Transient
    private String wareId; //

    @Column
    private String outTradeNo; //第三方交易编号

    /**
     * 计算总价格
     */
    public void sumTotalAmount(){
        BigDecimal totalAmount=new BigDecimal("0");
        for (OrderDetail orderDetail : orderDetailList) {
            totalAmount= totalAmount.add(orderDetail.getOrderPrice().multiply(new BigDecimal(orderDetail.getSkuNum())));
        }
        this.totalAmount=  totalAmount;
    }

    public String getTradeBody(){
        OrderDetail orderDetail = orderDetailList.get(0);
        String tradeBody=orderDetail.getSkuName()+"等"+orderDetailList.size()+"件商品";
        return tradeBody;
    }

}
