package com.baidu.gmall.gmallpayment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.baidu.gmall.CartInfoService;
import com.baidu.gmall.OrderService;
import com.baidu.gmall.PaymentService;
import com.baidu.gmall.bean.OrderInfo;
import com.baidu.gmall.bean.PaymentInfo;
import com.baidu.gmall.bean.enums.PaymentStatus;
import com.baidu.gmall.config.LoginRequire;
import com.baidu.gmall.gmallpayment.config.AlipayConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alei
 * @create 2019-08-31 16:55
 * <p>
 * 支付模块控制器
 */
@Controller
public class PayMentController {


    @Reference
    private OrderService orderService;

    @Autowired
    private AlipayClient alipayClient;

    @Reference
    private PaymentService paymentService;

    @Reference
    private CartInfoService cartInfoService;

    /**
     * 显示支付页面 选择支付渠道 跳转至支付页面
     *
     * @param request
     * @return
     */
    @RequestMapping("index")
    public String index(HttpServletRequest request) {

        //获取订单id
        String orderId = request.getParameter("orderId");

        //调用业务层方法 获取订单信息
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);

        //将订单id  与总价格放入域中  进行显示信息
        request.setAttribute("orderId", orderId);

        request.setAttribute("totalAmount", orderInfo.getTotalAmount());

        return "index";
    }

    /**
     * 点击提交订单生成二维码  与 保存订单
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/alipay/submit")
    @ResponseBody
    public String alipaySubmit(HttpServletRequest request, HttpServletResponse response) {

        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE); //获得初始化的AlipayClient

        // 获取订单Id
        String orderId = request.getParameter("orderId");

        //获取订单信息 赋值给支付对象
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);

        //创建支付对象
        PaymentInfo paymentInfo = new PaymentInfo();

        //进行赋值操作
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject("向来缘浅,奈何情深~");
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID); //设置订单状态为未支付
        paymentInfo.setCreateTime(new Date());

        //保存支付信息
        paymentService.savePaymentInfo(paymentInfo);

        //设置支付宝参数  从支付宝 api 拷贝的代码
        //https://docs.open.alipay.com/270/105899/#%E6%94%AF%E4%BB%98
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request

        //同步通知回调  通知用户成功失败
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);

        //异步回调通知 通知商家是否成功
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址

        //因为需要的是 json 串 所以我们封装map 来封装json对应的字符串 比拼接简单且效率高
        Map<String, Object> map = new HashMap<>();

        //设置第三方交易编号
        map.put("out_trade_no", paymentInfo.getOutTradeNo());

        //	销售产品码，与支付宝签约的产品码名称。  固定的
        map.put("product_code", "FAST_INSTANT_TRADE_PAY");

        //总金额
        map.put("total_amount", paymentInfo.getTotalAmount());

        //分类 购买商品的种类
        map.put("subject", paymentInfo.getSubject());

        //将 map 转换为 json
        alipayRequest.setBizContent(JSON.toJSONString(map));


        /*alipayRequest.setBizContent("{" +
                "    \"out_trade_no\":\"20150320010101001\"," +
                "    \"product_code\":\"FAST_INSTANT_TRADE_PAY\"," +
                "    \"total_amount\":88.88," +
                "    \"subject\":\"Iphone6 16G\"," +
                "    \"body\":\"Iphone6 16G\"," +
                "    \"passback_params\":\"merchantBizType%3d3C%26merchantBizNo%3d2016010101111\"," +
                "    \"extend_params\":{" +
                "    \"sys_service_provider_id\":\"2088511833207846\"" +
                "    }"+
                "  }");//填充业务参数*/
        String form = "";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8");
//        response.getWriter().write(form);//直接将完整的表单html输出到页面
//        response.getWriter().flush();
//        response.getWriter().close();

        //这里是延迟队列 在生成二维码的时候 延迟队列检查是否支付成功  意思就是15秒执行一次 总共执行3次
        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),15,3);

        //直接使用 @ResponseBody 返回页面
        return form;
    }

    /**
     * 同步回调通知  顺便删除购物车中已勾选的商品 分为勾选购物车与 购物车中的勾选商品 两个购物车
     *
     * @return
     */
    //http://payment.gmall.com/alipay/callback/return?charset=utf-8&out_trade_no=ALEI15672538555908&method=alipay.trade.page.pay.return&total_amount=0.01&sign=fUyjgpGEhUdaSlJBM0rP1EQh3OG1QaI%2FTTxSzSa5nh3hLvzzPTpL%2BYzHsrD0kFuug3MDfww6OqKUNlCACbWM%2FB9%2BA6gYVlvfYOKZOgPVSUNiWu3LG5oRKKhVQnyEqEJ41Mk%2F5qwXD3B0Mh%2B81j0hheQYo9QnmpEObCXS6drgLZtNJoSQNLrv4aD1OvwsnwV4Uitn2SGJUzd7RLWBEGdqYF%2Fg8ZBj6v%2BJ1KqFqjU1GnSl6pETKAntMkGDulU8X6jzV3Wh%2By0zSR4%2FsiuIuMuKRjhIzuM4S91UqtaXMcK8bik9R0r8Nxw85Kpf1apEtbXmulOs4Fvf9hG9qSTy8cgGnw%3D%3D&trade_no=2019083122001453350508298324&auth_app_id=2018020102122556&version=1.0&app_id=2018020102122556&sign_type=RSA2&seller_id=2088921750292524&timestamp=2019-08-31+20%3A20%3A02
    @RequestMapping("/alipay/callback/return")
    @LoginRequire
    public String callbackReturn(HttpServletRequest request) {

        //获取用户id
        String userId = (String) request.getAttribute("userId");

        //返回订单页面  清空购物车
        cartInfoService.deleteCheckedCart(userId);

        return "redirect:" + AlipayConfig.return_order_url;
    }

    /**
     * 异步回调通知  通知商家是否支付成功！
     * 因为路径中传的map形式的参数 所以封装
     */
    @RequestMapping("alipay/callback/notify")
    public String callbackNotify(@RequestParam Map<String, String> paramMap, HttpServletRequest request) {

//        Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中

        //将剩下参数进行 url_decode 编码，然后进行字典排序 组成字符串 得到待签名字符串
        boolean flag = false; //调用 SDK验证签名

        try {

            flag = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名

        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //在支付宝中有该笔交易
        if (flag) {
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            // 只有交易通知状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时，支付宝才会认定为买家付款成功。
            String trade_status = paramMap.get("trade_status");

            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {

                //付款成功之后 修改交易状态
                //如果交易状态为 PAID 或者 CLOSED
                //查询当前交易状态 获取第三方交易编号 通过第三方交易编号 查询交易记录对象 获取状态值进行判断
                String out_trade_no = paramMap.get("out_trade_no");

                PaymentInfo paymentInfoQuery = new PaymentInfo();

                paymentInfoQuery.setOutTradeNo(out_trade_no);

                //select * from paymentInfo where out_trade_no = ?
//                通过第三方交易编号 查询交易记录对象
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(paymentInfoQuery);

                //判断状态
                PaymentStatus paymentStatus = paymentInfo.getPaymentStatus();

                if (paymentStatus == PaymentStatus.ClOSED || paymentStatus == PaymentStatus.PAID) {

                    //说明交易当中关闭订单等 出现异常
                    return "failure";
                }

                //修改交易状态
                //调用更新方法
                PaymentInfo paymentInfoUPD = new PaymentInfo();
                paymentInfoUPD.setPaymentStatus(PaymentStatus.PAID);
                paymentInfoUPD.setCreateTime(new Date());

                //更新
                paymentService.updatePaymentInfo(out_trade_no, paymentInfoUPD);


                //TODO 通知订单支付成功  ActiveMQ 发送异步消息队列  下面两个都可以
                paymentService.sendPaymentResult(paymentInfo,"success");

                //如果状态等于这两个值 说明成功了
                return "success";
            }

        } else {
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }


        return "failure";
    }

    /**
     * 退款功能
     * payment.gmall.com/refund?orderId=100
     */
    @RequestMapping("refund")
    @ResponseBody
    public String refund(String orderId) {

        //调用服务层接口
        boolean result = paymentService.refund(orderId);

        return "" + result;
    }

    /**
     * 问题1：
     * 		微信设置回调地址？
     * 		商户支付回调URL设置指引：进入商户平台-->产品中心-->开发配置，进行配置和修改.
     * 问题2：
     * 		微信支付？支付宝支付？
     * 		如果用了微信支付，如何防止用户再次使用支付宝支付？
     * 		解决方案：
     * 1.支付日志记录每个订单支付的状态
     * a)无论微信支付，支付宝支付，都会有同步，异步回调
     * b)重点判断异步回调{给商家看，根据异步回调结果修改日志记录中的支付状态。}
     * c)在每种支付方式的控制器中，要做一个判断{订单的支付状态如果订单支付状态为已支付，则不生成二维码，给一个提示即可！}
     * d)如果支付状态只保存在数据库中，则会在高并发情况下会对数据库产生压力。 优化方案：将支付的状态放入缓存中！
     * i.Set(key,value)  key=user:orderId:info value=PaymentStatus.PAID
     * ii.状态 如果是PaymentStatus.PAID 或者 PaymentStatus.CLOSE 都不能生成二维码
     * iii.什么时候可以将支付状态删除：减库存完成，客户确认收获以后。
     *
     *
     * 微信支付功能
     * @param orderId
     * @return
     */
    @RequestMapping("wx/submit")
    @ResponseBody
    public Map createNative(String orderId){

        //做一个判断 支付日志中的订单支付状态  如果是已支付  则不生成二维码直接重定向到消息提示页面

        // 调用服务层数据
        // 第一个参数是订单Id ，第二个参数是多少钱，单位是分
        Map map = paymentService.createNative(orderId +"", "1");
        System.out.println(map.get("code_url"));
        // data = map
        return map;

    }

    /**
     * 模拟发送消息修改订单状态
     * @param paymentInfo
     * @param result
     * @return
     */
    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo,String result) {
        paymentService.sendPaymentResult(paymentInfo,result);

        return "ok";
    }

    /**
     * 查询订单信息 看是否支付成功
     * @param request
     * @return
     */
    @RequestMapping("queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(HttpServletRequest request) {

        //获取订单id
        String orderId = request.getParameter("orderId");

        //创建支付对象来调用检查支付状态
        PaymentInfo paymentInfoQuery = new PaymentInfo();

        //将订单id 设置到对象中
        paymentInfoQuery.setOrderId(orderId);

        //调用业务层方法 获取返回结果 是否支付成功 true  成功 false 失败
        boolean result = paymentService.checkPayment(paymentInfoQuery);

        return result+"";
    }


}
