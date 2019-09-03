package com.baidu.gmall.gmallpayment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.baidu.gmall.PaymentService;
import com.baidu.gmall.bean.PaymentInfo;
import com.baidu.gmall.bean.enums.PaymentStatus;
import com.baidu.gmall.config.ActiveMQUtil;
import com.baidu.gmall.config.HttpClient;
import com.baidu.gmall.gmallpayment.mapper.PayMentMapper;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alei
 * @create 2019-08-31 19:26
 */
@Service
public class PayMentServiceImpl implements PaymentService {

    /*
        获取配置文件中的值
     */
    // 服务号Id
    @Value("${appid}")
    private String appid;
    // 商户号Id
    @Value("${partner}")
    private String partner;
    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;

    @Autowired
    private PayMentMapper payMentMapper;


    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private ActiveMQUtil activeMQUtil; //注入activeMq 消息队列 来发送异步通知

    /**
     * 保存支付订单
     * @param paymentInfo
     */
    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {

        payMentMapper.insertSelective(paymentInfo);
    }

    /**
     * 查询支付信息
     * @param paymentInfo
     * @return
     */
    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfo) {
        return payMentMapper.selectOne(paymentInfo);
    }

    /**
     * 通过第三方交易编号修改支付状态
     * @param out_trade_no
     * @param paymentInfoUPD
     */
    @Override
    public void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUPD) {

        //创建修改的条件
        Example example = new Example(PaymentInfo.class);

        example.createCriteria().andEqualTo("outTradeNo",out_trade_no);

        payMentMapper.updateByExampleSelective(paymentInfoUPD,example);
    }

    /**
     * 退款功能实现
     * @param orderId
     * @return
     */
    @Override
    public boolean refund(String orderId) {

//        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key");

        //调用支付宝的退款请求类
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

        //封装 map  设置参数
        Map<String, Object> map = new HashMap<>();

        PaymentInfo paymentInfo = new PaymentInfo();

        paymentInfo.setOrderId(orderId);

        PaymentInfo paymentInfoQuery = getPaymentInfo(paymentInfo);

        map.put("out_trade_no", paymentInfoQuery.getOutTradeNo());

        map.put("refund_amount", paymentInfoQuery.getTotalAmount());

        map.put("refund_reason", "正常退款");

        //更复杂的业务 对号入座 就可以了 在支付宝接口文档 找到对应的参数值 放入就可以
        request.setBizContent(JSON.toJSONString(map));

//        request.setBizContent("{" +
//                "    \"out_trade_no\":\"20150320010101001\"," +
//                "    \"trade_no\":\"2014112611001004680073956707\"," +
//                "    \"refund_amount\":200.12," +
//                "    \"refund_reason\":\"正常退款\"," +
//                "    \"out_request_no\":\"HZ01RF001\"," +
//                "    \"operator_id\":\"OP001\"," +
//                "    \"store_id\":\"NJ_S_001\"," +
//                "    \"terminal_id\":\"NJ_T_001\"" +
//                "  }");
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }


    /**
     * 生成二维码微信支付
     * @param total_fee
     * @param total_fee
     * @return
     */
    @Override
    public Map createNative(String orderId, String total_fee) {
        /*
            1.  传递参数
            2.  map 转成 xml 发送请求
            3.  获取结果
         */
        HashMap<String, String> param = new HashMap<>();

        param.put("appid",appid);
        param.put("mch_id",partner);
        param.put("nonce_str",WXPayUtil.generateNonceStr());
        param.put("body","你只管努力，其他的交给天意");
        param.put("out_trade_no",orderId);
        // 注意单位是分
        param.put("total_fee",total_fee);
        param.put("spbill_create_ip", "127.0.0.1");//IP
        param.put("notify_url", "http://order.gmall.com/trade");//回调地址(随便写)
        param.put("trade_type", "NATIVE");//交易类型

        // 使用工具类
        try {
            String xmlParam  = WXPayUtil.generateSignedXml(param, partnerkey);
            // 调用httpClient
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            // 设置https 发送
            httpClient.setHttps(true);
            // 将xml 发送过去
            httpClient.setXmlParam(xmlParam);
            // 设置post 请求
            httpClient.post();

            // 获取结果
            String result  = httpClient.getContent();
            // 将内容转换为map
            Map<String, String> resultMap  = WXPayUtil.xmlToMap(result);
            // 设置数据
            // 新创建一个map 用来存储数据
            Map<String, String> map=new HashMap<>();
            map.put("code_url",resultMap.get("code_url"));
            map.put("total_fee",total_fee);
            map.put("out_trade_no",orderId);

            return map;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 发送消息队列来通知订单修改状态
     *
     * 创建消息提供者 发送消息给订单 修改订单状态
     *
     * @param paymentInfo
     * @param result
     *
     * 接口：发送消息，给activemq 支付结果！success,fail
     * 发送一个orderId，result 【success,fail】
     */
    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo, String result) {

        //获取连接对象
        Connection connection = activeMQUtil.getConnection();

        Session session = null;
        MessageProducer producer = null;
        try {
            //获取连接
            connection.start();

            //创建session 对象  第一个参数是否使用事务  第二个参数使用事务的方式 有自动签收 事务 手动签收 订阅（一对多，公众号）
            session = connection.createSession(true, Session.SESSION_TRANSACTED);

            //创建队列对象  因为继承了Destination 所以直接可以传入到提供者中
            Queue payment_result_queue = session.createQueue("PAYMENT_RESULT_QUEUE");

            //创建提供者
            producer = session.createProducer(payment_result_queue);

            //创建消息对象 因为数据是map类型的 所以创建 map 类型的消息对象
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            //设置消息
            activeMQMapMessage.setString("orderId",paymentInfo.getOrderId());
            activeMQMapMessage.setString("result",result);

            //发送消息 需要传入一个消息对象 创建消息对象
            producer.send(activeMQMapMessage);

            //因为开启了事务 所以此处必须提交
            session.commit();

        } catch (JMSException e) {
            e.printStackTrace();
        }finally {
            if (producer != null) {
                try {
                    producer.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
            if (session != null) {
                try {
                    session.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     *  检查订单状态是否支付成功
     * @param paymentInfoQuery
     * @return
     */
    @Override
    public boolean checkPayment(PaymentInfo paymentInfoQuery) {

        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        // 什么一个map 集合来存储数据
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",paymentInfoQuery.getOutTradeNo());
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        // 指的在支付宝中有该笔交易 相当于异步回调
        if(response.isSuccess()){
            /*
                	交易状态：WAIT_BUYER_PAY（交易创建，等待买家付款）、
                	TRADE_CLOSED（未付款交易超时关闭，或支付完成后全额退款）、
                	TRADE_SUCCESS（交易支付成功）、TRADE_FINISHED（交易结束，不可退款）
             */
            System.out.println("调用成功");
            if ("TRADE_SUCCESS".equals(response.getTradeStatus()) || "TRADE_FINISHED".equals(response.getTradeStatus())){
                System.out.println("支付成功");
                // 更新交易记录的状态 paymentInfo
                // 改支付状态
                PaymentInfo paymentInfoUpd = new PaymentInfo();
                paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                updatePaymentInfo(paymentInfoQuery.getOutTradeNo(),paymentInfoUpd);
                // paymentInfoQuery 必须有orderId 才能正常发送消息队列给订单！
                sendPaymentResult(paymentInfoQuery,"success");
                return true;
            }

        } else {
            System.out.println("调用失败");
            return  false;
        }

        return false;
    }

    /**
     *  发送延迟队列给订单  定时去检查订单的支付状态  延迟队列反复调用
     * @param outTradeNo 第三方支付编号
     * @param delaySec  每次检查时的间隔时间
     * @param checkCount 检查支付状态的次数是几次
     */
    @Override
    public void sendDelayPaymentResult(String outTradeNo, int delaySec, int checkCount) {

        //调用消息队列工具类 获取连接
        Connection connection = activeMQUtil.getConnection();

        Session session = null;
        MessageProducer producer = null;

        try {
            //获取连接
            connection.start();

            //创建session 启动事务 签收方式 使用事务的签收方式
            session = connection.createSession(true, Session.SESSION_TRANSACTED);

            //创建队列
            Queue payment_result_check_queue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");

            //创建提供者
            producer = session.createProducer(payment_result_check_queue);

            //创建发送的消息
            MapMessage mapMessage = new ActiveMQMapMessage();

            //封装发送的消息
            mapMessage.setString("outTradeNo",outTradeNo);
            mapMessage.setInt("delaySec",delaySec);
            mapMessage.setInt("checkCount",checkCount);

            //设置延迟多少时间  开启延迟队列 意思就是在多少毫秒之后再去发送消息  ScheduledMessage计划发送的消息类型
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,delaySec * 1000);

            //发送消息
            producer.send(mapMessage);

            //因为开启了事务 所以进行提交
            session.commit();

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (producer != null) {
                try {
                    producer.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
            if (session != null) {
                try {
                    session.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     *  根据订单id  关闭过期订单  // 关闭支付信息
     * @param orderId
     */
    @Override
    public void closePayment(String orderId) {
        //构建条件对象
        Example example = new Example(PaymentInfo.class);

        //构建条件
        example.createCriteria().andEqualTo("orderId",orderId);

        //创建支付信息对象
        PaymentInfo paymentInfo = new PaymentInfo();
        //设置订单状态
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED);

        //根据条件去更新订单状态  将其改为关闭状态
        payMentMapper.updateByExampleSelective(paymentInfo,example);
    }
}
