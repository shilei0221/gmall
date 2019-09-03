package com.baidu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.baidu.gmall.CartInfoService;
import com.baidu.gmall.ManageService;
import com.baidu.gmall.OrderService;
import com.baidu.gmall.UserInfoService;
import com.baidu.gmall.bean.*;
import com.baidu.gmall.bean.enums.OrderStatus;
import com.baidu.gmall.bean.enums.ProcessStatus;
import com.baidu.gmall.config.LoginRequire;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Alei
 * @create 2019-08-14 17:00
 */
@Controller
public class OrderController {

    @Reference
    private UserInfoService userInfoService;

    @Reference
    private CartInfoService cartInfoService;

    @Reference
    private OrderService orderService;

    @Reference
    private ManageService manageService;

    /**
     * 创建订单控制器  进行显示订单的详细信息
     * @param request
     * @return
     */
    @RequestMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request) {

        //获取用户id
        String userId = (String) request.getAttribute("userId");

        //获取选中的购物车集合
        List<CartInfo> cartCheckedList = cartInfoService.getcartCheckedList(userId);

        //获取收件人地址
        List<UserAddress> userAddressList = userInfoService.getAddressAll(userId);

        //将地址信息放入域中
        request.setAttribute("userAddressList",userAddressList);

        //创建一个集合来封装最后返回的数据
        List<OrderDetail> orderDetailList = new ArrayList<>();

        //遍历获取到的选中的商品集合
        if (cartCheckedList != null && cartCheckedList.size() > 0) {

            //遍历
            for (CartInfo cartInfo : cartCheckedList) {

                //创建一个订单详情对象 将获取到的购物车对象 赋值给订单详情对象
                OrderDetail orderDetail = new OrderDetail();

                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setOrderPrice(cartInfo.getCartPrice());
                orderDetailList.add(orderDetail);
            }
        }

        //获取第三方交易编号 调用业务层方法
        String tradeNo = orderService.getTradeNo(userId);

        //将第三方交易编号放入域中  判断进行避免表单重复提交 页面隐藏域 在页面表单提交的时候，将TradeNo进行保存
        request.setAttribute("tradeNo",tradeNo);

        //将封装好的订单详情集合放入域中
        request.setAttribute("orderDetailList",orderDetailList);

        //创建订单对象 将订单详情集合设置到订单中
        OrderInfo orderInfo = new OrderInfo();

        orderInfo.setOrderDetailList(orderDetailList);

        //调用总价格方法 设置
        orderInfo.sumTotalAmount();

        //将计算后的总价格放入域中 获取
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());

        return "trade";
    }

    /**
     * 下订单 保存订单信息
     * 1、保存单据前要做交易 验库存
     * 2、保存单据：orderInfo、orderDetail 两张表
     * 3、保存以后把购物车中的商品删除
     * 4、重定向到支付页面
     *
     * http://order.gmall.com/submitOrder
     *
     * 前台页面中传递的数据不是 json 格式的形式 所以在后台封装获取的时候 就不需要使用 @RequestBody 注解获取封装前端传递的数据了
     *  如果是json 格式的数据 则后台需要使用 @RequestBody 注解封装
     */
    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder (OrderInfo orderInfo, HttpServletRequest request) {

        /*
            在点击结算的时候会有表单重复提交问题，我们可以在进入结算页面时，生成一个结算流水号，然后保存到结算页面的隐藏元素中，
            每次用户提交都检查该流水号与页面提交的是否相符，订单保存以后把后台的流水号删除掉。
            那么第二次用户用同一个页面提交的话流水号就会匹配失败，无法重复保存订单。
         */
        //获取用户id
        String userId = (String) request.getAttribute("userId");

        String tradeNo = request.getParameter("tradeNo");

        //检查隐藏域中的第三方交易编号是否与缓存中一致 判断是否为第一次提交订单
        boolean checkTradeCode = orderService.checkTradeCode(userId, tradeNo);

        //返回false 说明失败 是重复下单
        if (!checkTradeCode) {
            request.setAttribute("errMsg","该订单已提交,请勿重复提交订单~");
            return "tradeFail";
        }

        //因为前端传递的数据只是订单中的一部分 所以我们手动进行赋值给其他数据
        orderInfo.setOrderStatus(OrderStatus.UNPAID); //设置订单状态为未支付

        orderInfo.setProcessStatus(ProcessStatus.UNPAID); //设置进程状态为未支付状态

        orderInfo.sumTotalAmount(); //调用方法 计算总价格

        orderInfo.setUserId(userId); //将用户id 设置到对象中

        //获取订单详情信息
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        //判断是否为空
        if (orderDetailList != null && orderDetailList.size() > 0) {

            //进行遍历
            for (OrderDetail orderDetail : orderDetailList) {

                //从订单中取看skuId 数量
                boolean flag = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());

                //说明库存不足
                if (! flag) {

                    request.setAttribute("errMsg","库存不足,请您重新下单~");

                    return "tradeFail";
                }

                //校验价格是否一致
                SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());

                //调用其中价格进行比较 如果不一致 说明价格不一致 返回错误页面
                int result = orderDetail.getOrderPrice().compareTo(skuInfo.getPrice());

                if (result != 0) {
                    //说明价格有变动
                    request.setAttribute("errMsg",orderDetail.getSkuName() + "价格有变动,请重新下单~");
                    return "tradeFail";
                }
                //如果价格有变动的话 在调用购物车中的方法 重新根据用户id 获取一下最新的购物车数据
                cartInfoService.loadCartCache(userId);
            }

        }

        //保存订单 下订单 orderInfo orderDetail
        String orderId = orderService.saveOrder(orderInfo);

        //保存订单完成之后 将缓存中的 第三方交易编号删除
        orderService.delTradeCode(userId);

        //从定向到支付页面
        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }


    /**
     * 当支付完成之后 判断商品是否为不同的库存中 进行拆单
     * http://order.gmall.com/orderSplit?orderId=xxx&wareSkuMap=xxx
     *
     * @param request
     * @return
     *
     *
     */
    @RequestMapping("orderSplit")
    @ResponseBody
    public String orderSplit(HttpServletRequest request) {

        //从请求路径获取订单id
        String orderId = request.getParameter("orderId");

        String wareSkuMap = request.getParameter("wareSkuMap");

        //定义订单集合对象 用来封装子订单集合中的订单详情
        List<Map> wareMapList = new ArrayList<>();

        //调用业务层获取子订单集合
        /*根据库存接口文档 中的参数 获取到拆单后的子订单集合*/
        List<OrderInfo> subOrderInfoList = orderService.splitOrder(orderId,wareSkuMap);

        //循环子订单集合
        for (OrderInfo orderInfo : subOrderInfoList) {

            //调用业务成方法 将每一个订单信息转换为一个 map
            Map map = orderService.initWareOrder(orderInfo);

            //将每一个子订单信息添加到集合中   因为可能有多个子订单
            wareMapList.add(map);
        }

        //将封装的多个子订单集合转换为json字符串返回
        return JSON.toJSONString(wareMapList);
    }
}
