package com.baidu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.baidu.gmall.OrderService;
import com.baidu.gmall.bean.OrderDetail;
import com.baidu.gmall.bean.OrderInfo;
import com.baidu.gmall.bean.enums.ProcessStatus;
import com.baidu.gmall.config.ActiveMQUtil;
import com.baidu.gmall.config.HttpClientUtil;
import com.baidu.gmall.config.RedisUtil;
import com.baidu.gmall.order.mapper.OrderDetailMapper;
import com.baidu.gmall.order.mapper.OrderInfoMapper;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import java.util.*;

/**
 * @author Alei
 * @create 2019-08-30 19:26
 */
@Service
public class OrderServiceImpl implements OrderService {


    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ActiveMQUtil activeMQUtil;//注入消息队列

    /**
     * 保存订单信息 以及 订单明细 下订单
     *
     * 保存完成去调用支付 所以返回orderId
     * @param orderInfo
     * @return
     */
    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {

        //设置创建时间
        orderInfo.setCreateTime(new Date());

        //设置失效时间  先获取日历时间
        Calendar calendar = Calendar.getInstance();

        //设置时间为一天
        calendar.add(Calendar.DATE,1);

        //设置过期时间为第二天
        orderInfo.setExpireTime(calendar.getTime());

        //生成第三方支付编号  使用时间戳加随机数
        String outTradeNo = "ALEI" + System.currentTimeMillis() + "" + new Random().nextInt(1000);

        //设置第三方交易编号
        orderInfo.setOutTradeNo(outTradeNo);

        //将订单添加到数据库中
        orderInfoMapper.insertSelective(orderInfo);

        //插入订单详细信息
        //从订单中获取订单详情信息
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        //判断订单详情是否为空
        if (orderDetailList != null && orderDetailList.size() > 0) {

            //遍历订单详情
            for (OrderDetail orderDetail : orderDetailList) {

                //将订单id 设置到订单详情中 注意：订单bean中添加注解 获取自增主键id
                orderDetail.setOrderId( orderInfo.getId());

                //添加到数据库中
                orderDetailMapper.insertSelective(orderDetail);
            }
        }

        // 为了跳转到支付页面使用。支付会根据订单id进行支付。
        return orderInfo.getId();
    }


    /**
     * 生成一个第三方交易编号
     * @param userId
     * @return
     */
    @Override
    public String getTradeNo(String userId) {
        //获取jedis
        Jedis jedis = null;

        try {
            jedis = redisUtil.getJedis();

            //定义 key
            String tradeNoKey =  "user:"+userId+":tradeCode";

            //生成第三方交易编号 使用 uuid
            String tradeCode = UUID.randomUUID().toString().replace("-","");

            //设置到redis中 将第三方交易编号
            jedis.set(tradeNoKey,tradeCode);

            return tradeCode;

        }catch (Exception e){
            e.printStackTrace();
        }finally {

            if (jedis != null) {
                jedis.close();
            }
        }
        return null;
    }

    /**
     * 判断页面与缓存中的第三方交易编号是否一致 一致则提交 不一致则提示信息
     * @param userId
     * @param tradeCodeNo
     * @return
     */
    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {

        //获取jedis
        Jedis jedis = null;

        try {
            jedis = redisUtil.getJedis();

            //定义 key
            String tradeNoKey =  "user:"+userId+":tradeCode";

            String tradeCode = jedis.get(tradeNoKey);

            return tradeCodeNo.equals(tradeCode);

        }catch (Exception e){
            e.printStackTrace();
        }finally {

            if (jedis != null) {
                jedis.close();
            }
        }
        return false;
    }


    /**
     * 删除某个用户的第三方交易编号 使其避免表单重复提交
     * @param userId
     */
    @Override
    public void delTradeCode(String userId) {

        //获取jedis
        Jedis jedis = null;

        try {
            jedis = redisUtil.getJedis();

            //定义 key
            String tradeNoKey =  "user:"+userId+":tradeCode";

            //删除key
            jedis.del(tradeNoKey);

        }catch (Exception e){
            e.printStackTrace();
        }finally {

            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * 验证库存中商品是否足够
     * @param skuId
     * @param skuNum
     * @return
     */
    @Override
    public boolean checkStock(String skuId, Integer skuNum) {

        //调用 HttpClient 工具类 访问控制器  控制器与控制器访问 涉及到跨域 所以使用 httpClient 调用
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);


        return "1".equals(result);
    }

    /**
     * 根据订单id 查询订单信息
     * @param orderId
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(String orderId) {

        //根据主键获取订单对象
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);

        OrderDetail orderDetail = new OrderDetail();

        //将订单id 设置到订单详情中
        orderDetail.setOrderId(orderId);

        //根据订单id 查询订单详情表 返回集合数据 放入订单对象中
        orderInfo.setOrderDetailList(orderDetailMapper.select(orderDetail));

        return orderInfo;
    }


    /**
     * 修改订单状态为已支付
     * @param orderId
     * @param processStatus
     */
    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus) {

        //根据订单 id  获取订单对象
        OrderInfo orderInfo = new OrderInfo();

        //设置订单状态
        orderInfo.setId(orderId);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
        orderInfo.setProcessStatus(processStatus);

        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);

    }

    /**
     * 发送消息给库存 修改订单状态
     * @param orderId
     */
    @Override
    public void sendOrderStatus(String orderId) {

        Connection connection = activeMQUtil.getConnection();

        //通过订单id获取订单信息 封装成一个map 最后返回json字符串
        String orderJson = initWareOrder(orderId);

        Session session = null;
        MessageProducer producer = null;

        try {
            //获取连接
            connection.start();

            //创建session
            session = connection.createSession(true,Session.SESSION_TRANSACTED);

            //创建队列
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");

            //创建提供zhe
            producer = session.createProducer(order_result_queue);

            //创建消息对象
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();

            //设置消息
            activeMQTextMessage.setText(orderJson);

            //发送消息
            producer.send(activeMQTextMessage);

            //提交
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
     * 使用订单id 获取对象封装为map 转换为字符串返回
     * @param orderId
     * @return
     */
    private String initWareOrder(String orderId) {

        //根据订单id 获取订单对象
        OrderInfo orderInfo = getOrderInfo(orderId);

        //将订单对象封装为一个map
        Map map = initWareOrder(orderInfo);

        //以json字符串的形式返回
        return JSON.toJSONString(map);
    }

    /**
     * 将订单信息封装为一个map
     *
     * 设置初始化仓库信息方法
     * @param orderInfo
     * @return
     */
    private Map initWareOrder(OrderInfo orderInfo) {
        //创建map对象封装数据
        Map<String,Object> map = new HashMap<>();

        /*
            根据库存系统的文档 封装对应的信息
            因为订单中有订单详情集合 订单详情集合中又对应着属性名属性值 多个 k-v 形式的参数
            所以使用一个map存储订单信息与详情集合，详情集合使用map封装一个详情对象在使用list集合封装多个详情对象
         */
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody",orderInfo.getTradeBody());
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");

        //拆单的时候使用
//        map.put("wareId",orderInfo.getWareId());

        //组合json
        List detailList = new ArrayList();

        //获取订单详情集合
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        //判断是否为空
        if (orderDetailList != null && orderDetailList.size() > 0) {

            //进行遍历
            for (OrderDetail orderDetail : orderDetailList) {
                //定义一个map 封装订单详情中的信息
                Map detailMap  = new HashMap();

                //根据库存系统文档封装参数
                detailMap.put("skuId",orderDetail.getSkuId());
                detailMap.put("skuName",orderDetail.getSkuName());
                detailMap.put("skuNum",orderDetail.getSkuNum());

                //将封装后的多个订单详情放入订单集合中
                detailList.add(detailMap);
            }
        }
        map.put("details",detailList);

        return map;

    }
}
