package com.baidu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.baidu.gmall.ManageService;
import com.baidu.gmall.bean.CartInfo;
import com.baidu.gmall.bean.SkuInfo;
import com.baidu.gmall.config.CookieUtil;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alei
 * @create 2019-08-28 16:40
 */
@Component
public class CartCookieHandler {

    // 定义购物车名称
    private String cookieCartName = "CART";
    // 设置cookie 过期时间
    private int COOKIE_CART_MAXAGE = 7 * 24 * 3600;
    @Reference
    private ManageService manageService;

    /**
     * 将未登录的数据放入 cookie 中
     * <p>
     * 1、先查询出来在cookie中的购物车 反序列化成列表
     * 2、通过循环比较有没有该商品
     * 3、如果存在该商品，数量相加
     * 4、如果不存在该商品，增加商品到cookie中
     * 5、然后将对象转换为json字符串，利用之前做好的 cookieUtil 工具类 保存到cookie中
     *
     * @param request
     * @param response
     * @param skuId
     * @param userId
     * @param skuNum
     */
    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, int skuNum) {

        //判断 cookie中是否存在购物车 有可能商品名字是中文 所以进行编码
        String cartJson = CookieUtil.getCookieValue(request, cookieCartName, true);

        //定义一个集合 用来存储最终的购物车中的多个商品数据
        List<CartInfo> cartInfoList = new ArrayList<>();

        //定义一个标志符  代替 else 来进行判断cookie中是否存在购物车商品数据
        /*
            默认标志符为false 如果cookie中存在购物车数据 就将标志符改为 true 不执行下面的代码
            如果cookie中不存在对应的商品 则执行后面 标志符代码 将商品放入cookie中
         */
        boolean ifExist = false;

        //判断 cartJson 是否为空
        if (cartJson != null) {

            //说明有对应的商品 进行数量相加
            //将获取到的购物车字符串转换为集合对象 泛型指定为购物车对象
            cartInfoList = JSON.parseArray(cartJson, CartInfo.class);

            //将获取到的购物车集合 进行遍历
            for (CartInfo cartInfo : cartInfoList) {
                //判断当前添加的商品id 是否等于获取到的cookie中的商品id
                if (skuId.equals(cartInfo.getSkuId())) {

                    //如果相等进行商品数量上的累加
                    cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);

                    //设置商品的实时价格  因为实时价格初始值为空 所以设置为当前商品的价格
                    cartInfo.setSkuPrice(cartInfo.getCartPrice());

                    //相加之后将标志符改为 true 不执行后续代码
                    ifExist = true;

                    break;
                }
            }
        }

        //判断标志符如果取反为真 说明cookie中没有对应的商品 进行添加商品
        if (!ifExist) {
            //调用后台管理接口方法 获取商品信息
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);

            //创建购物车对象 将商品信息 设置到 购物车对象中
            CartInfo cartInfo = new CartInfo();

            cartInfo.setSkuId(skuId);

            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());

            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);

            //将设置好的购物车对象添加到购物车集合中
            cartInfoList.add(cartInfo);
        }

        //最后不管是cookie中是否有商品 进行数量累加  进行添加对应商品 最后将最终的购物车对象 写入 cookie 中
        CookieUtil.setCookie(request, response, cookieCartName, JSON.toJSONString(cartInfoList), COOKIE_CART_MAXAGE, true);
    }

    /**
     * 说明用户未登录 从cookie中获取数据返回
     * @param request
     * @return
     */
    public List<CartInfo> getCartList(HttpServletRequest request) {

        //调用cookieUtil 工具类 获取 cookie 中的值
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);

        //将获取到的字符串值转换为集合 返回
        List<CartInfo> cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);

        return  cartInfoList;
    }

    /**
     * 合并之后将cookie中的数据 删除
     * @param request
     * @param response
     */
    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request,response,cookieCartName);
    }


    /**
     * 更新cookie中的商品选中状态
     * @param request
     * @param response
     * @param skuId
     * @param isChecked
     */
    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId, String isChecked) {

        //获取cookie中的数据
        List<CartInfo> cartList = getCartList(request);
        
        //判断cookie中是否存在数据
        if (cartList != null && cartList.size() > 0) {
            
            //遍历集合商品数据
            for (CartInfo cartInfo : cartList) {
                
                //判断勾选的与cookie中是否为同一件商品
                if (skuId.equals(cartInfo.getSkuId())) {
                    //如果是同一件商品 则更改勾选状态
                    cartInfo.setIsChecked(isChecked);
                    
                }
            }
            //最后在写会到cookie中  更新 cookie
            String newCartJson  = JSON.toJSONString(cartList);

            CookieUtil.setCookie(request,response,cookieCartName,newCartJson,COOKIE_CART_MAXAGE,true);
        }

    }
}