package com.baidu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baidu.gmall.CartInfoService;
import com.baidu.gmall.ManageService;
import com.baidu.gmall.bean.CartInfo;
import com.baidu.gmall.bean.SkuInfo;
import com.baidu.gmall.config.LoginRequire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Alei
 * @create 2019-08-28 10:06
 */
@Controller
public class CartController {

    @Reference
    private CartInfoService cartInfoService;

    @Autowired
    private CartCookieHandler cartCookieHandler;

    @Reference
    private ManageService manageService;

    private String userKey; //记录未登录用户的id 给 uuid

    /**
     * 将商品添加到购物车
     *  1、根据 skuId 查询出商品详情 skuInfo
     *  2、把 skuInfo 信息对应保持到购物车 【购物车的实体类】
     *  3、返回成功页面
     * @param request
     * @return
     */
    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response) {

        /*
            1、获取参数 skuId skuNum
            2、判断userId是否进行登录
            3、如果登录则调用后台的service 的业务方法实现
            4、如果未登录，要把购物车数据暂存到 cookie 中，待合并后移除cookie中的数据
            5、实现利用 cookie 保存购物车的方法
         */
        //从域中获取对应的数据
        String skuNum = request.getParameter("skuNum");
        String skuId = request.getParameter("skuId");

        //因为拦截器中在用户登录的时候将 userId 存入域中，所以如果用户登录了 在这里直接获取即可
        String userId = (String) request.getAttribute("userId");

        //判断获取到的用户id 是否为空 判断是否登录
        if (userId != null) {
            //说明用户已登录 将购物车添加进redis中
            cartInfoService.addToCart(userId,skuId,Integer.parseInt(skuNum));

        } else {
            //说明未登录  将购物车添加到 cookie中 将未登录数据放入cookie中
           // cartCookieHandler.addToCart(request,response,skuId,userId,Integer.parseInt(skuNum));

            /*
            以下将未登录数据放入redis中
             */
            userKey = getUUID(request, response);

            //将其放入cookie  因为不同用户发送不同的请求 携带不同的cookie 所以放入cookie中 在redis中获取的时候可以区别出不同的用户对应的未登录时的购物车
            Cookie cookie = new Cookie("user-key",userKey);

            //将cookie 写给浏览器 客户端

            //此处必须执行 否则cookie 中 不存在该值

            response.addCookie(cookie);

            //未登录下将商品放入redis中
            cartInfoService.addToCartRedis(skuId,userKey,Integer.parseInt(skuNum));
        }

        //获取商品信息 放入域中 前端获取显示
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        request.setAttribute("skuNum",skuNum);
        request.setAttribute("skuInfo",skuInfo);

        return "success";
    }


    /*
     获取 uuid 存入 cookie 中

     因为每一个用户在添加购物车时候 都会发生一个请求 携带一个 cookie 这样 uuid 值就会不一样 代表不同用户的标志
     */
    private String  getUUID(HttpServletRequest request, HttpServletResponse response) {
        //获取cookie中的uuid  如果没有进行下一步添加
        Cookie[] cookies = request.getCookies();

        //判断cookie中是否有数据
        if (cookies != null && cookies.length > 0) {

            //进行遍历获取其中的值
            for (Cookie cks : cookies) {

                //判断获取到的cookie中的数据名称是否等于我们放进去的key
                if (cks.getName().equals("user-key")){

                    //如果相等 取值进行赋值 说明是同一个人使用多次【如果不同的人应该不同的key】
                    userKey = cks.getValue();
                } else {
                    //记录未登录的 userId 给UUID
                    userKey = UUID.randomUUID().toString().replace("-","");
                }
            }
        }
        return userKey;
    }

    /**
     * 展示购物车列表
     *  1、展示购物车中的信息
     *  2、如果用户未登录从cookie中获取值
     *  3、如果用户已登录从缓存中获取值，如果缓存没有，加载数据库获取
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request,HttpServletResponse response) {

        /*
            判断用户是否登录  进行登录操作 未登录操作

            合并购物车
                由于加入购物车时，用户可能存在登录和未登录两个状态，登录前在cookie 中保存了一部分购物车信息，如果用户登录了，那么对应的要把cookie中的购物车
                合并到数据库中，并且刷新缓存

              思路：
                1、用数据库中的购物车列表与传递过来的cookie里的购物车列表循环匹配
                2、能够匹配上的数量相加
                3、匹配不上的插入到数据库中
                4、最后重新加载缓存
                5、合并按照登录为主的购物车进行合并
         */

        //将未登录购物车数据存入redis中
        return getRedis(request,response);

        //获取cookie 与 redis 未登录 与登录 状态下的数据 进行合并数据
        //return getCart(request, response);
    }

    /**
     * 将未登录购物车数据存入redis中
     * @param request
     * @return
     */
    private String getRedis(HttpServletRequest request,HttpServletResponse response) {
        String userId = (String) request.getAttribute("userId");


        /*
         * 从 cookie 中获取 未登录时用户的 id  【uuid】 赋值给 userKey  最后定义key
         */
        userKey = getUUID(request,response);

        System.out.println("cooki中e: **  " + userKey);

        //定义一个集合接收查询的返回值
        List<CartInfo> cartInfoList = new ArrayList<>();

        if (userId != null) {
            //说明用户已经登录

            //查询redis中未登录的数据  合并使用
            List<CartInfo> cartListRD  = cartInfoService.getCartListRedis(userKey);

            //判断 redis 是否存在数据
            if (cartListRD != null && cartListRD.size() > 0) {
                // 进行合并 如果存在购物车 进行合并
                cartInfoList = cartInfoService.mergeToCartList(cartListRD,userId);

                //合并之后 将 redis 中的数据进行清空
                cartInfoService.deleteCartRedis(userKey);
            }else {
                //说明用户已经登录 从缓存中获取 缓存没有从数据库中获取
                cartInfoList = cartInfoService.getCartList(userId);
            }
        } else {
            //说明用户未登录
            //说明未登录 从 redis 中获取数据显示
            cartInfoList = cartInfoService.getCartListRedis(userKey);

        }
        //将接收后的结果集合放入域中 前台获取显示数据
        request.setAttribute("cartInfoList",cartInfoList);

        return "cartList";
    }

    /**
     * 显示用户登录与未登录下的信息  redis 与 cookie
     * @param request
     * @param response
     * @return
     */
    private String getCart(HttpServletRequest request, HttpServletResponse response) {

        String userId = (String) request.getAttribute("userId");

        //定义一个集合接收查询的返回值
        List<CartInfo> cartInfoList = new ArrayList<>();

        if (userId != null) {

            //查询cookie中是否存在数据
            List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);

            //判断cookie中是否存在数据
            if (cartListCK != null && cartListCK.size() > 0) {
                // 进行合并 如果存在购物车 进行合并
                cartInfoList = cartInfoService.mergeToCartList(cartListCK,userId);

                //合并之后 将 cookie中的数据进行清空
                cartCookieHandler.deleteCartCookie(request,response);

            } else {
                //说明用户已经登录 从缓存中获取 缓存没有从数据库中获取
                cartInfoList = cartInfoService.getCartList(userId);
            }

        } else {
            //说明未登录 从 cookie 中获取数据显示  因为cookie工具类获取cookie值需要一个请求对象
            cartInfoList = cartCookieHandler.getCartList(request);
        }

        //将接收后的结果集合放入域中 前台获取显示数据
        request.setAttribute("cartInfoList",cartInfoList);

        return "cartList";
    }

    /**
     * http://cart.gmall.com/checkCart
     *
     * 当用户勾选中商品的时候 锁定该勾选的商品 并更新缓存中的商品勾选状态
     *  区分 用户登录与用户未登录状态，分别更新对应的商品勾选状态
     */
    @RequestMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false) //因为勾选不需要登录 并且需要获取userId 所以加自定义注解 使程序走拦截器 获取用户id
    public void checkCart(HttpServletRequest request,HttpServletResponse response) {

        //未登录的时候将数据放入redis缓存中
        checkCartRedis(request, response);

        //未登录时使用cookie存储的商品数据
        //checkCartCookie(request, response);
    }

    /**
     * 未登录的时候将数据放入redis缓存中
     * @param request
     * @param response
     */
    private void checkCartRedis(HttpServletRequest request, HttpServletResponse response) {
        //TODO 这里使用redis存储未登录数据   需完善
        //从页面url 获取 skuId  isChecked 是否选中 以及用户id从域中获取 拦截器的时候存入
        String skuId = request.getParameter("skuId");

        String isChecked = request.getParameter("isChecked");

        String userId = (String) request.getAttribute("userId");

        //获取cookie中的uuid
        userKey = getUUID(request, response);

        //判断用户id是否为空
        if (userId != null) {

            //说明用户已经登录 从redis中获取数据 改变其中的勾选状态
            cartInfoService.checkCart(skuId,isChecked,userId);
        } else {
            //说明用户未登录 从redis中获取商品数据 改变其勾选状态
            cartInfoService.checkCartRedis(skuId,isChecked,userKey);
        }
    }

    /**
     * 未登录时使用cookie存储的商品数据
     * @param request
     * @param response
     */
    private void checkCartCookie(HttpServletRequest request, HttpServletResponse response) {
        //从页面url 获取 skuId  isChecked 是否选中 以及用户id从域中获取 拦截器的时候存入
        String skuId = request.getParameter("skuId");

        String isChecked = request.getParameter("isChecked");

        String userId = (String) request.getAttribute("userId");

        //判断用户id是否为空
        if (userId != null) {

            //说明用户已经登录 从redis中获取数据 改变其中的勾选状态
            cartInfoService.checkCart(skuId,isChecked,userId);
        } else {
            //说明用户未登录 从cookie中获取商品数据 改变其勾选状态
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }
    }

    /**
     * http://cart.gmall.com/toTrade
     *
     * 点击结算页面
     * 当用户点击结算的时候需要用户必须登录才能结算
     * 在未登录的时候点击结算提示用户登录并合并未登录时选中的购物车数据到登录购物车中
     * 并且将未登录与登录时的商品数量相加
     * 我们是以用户未登录时选中的商品为主 直接合并到登录购物车中
     * 可以有两种方案 从商家多卖角度 合并的时候 将未登录与登录的商品数量相加
     *              从用户的角度考虑  合并的时候 将未登录与登录的商品以未登录时选中的数量为主 直接覆盖登录时的数量
     */
    @RequestMapping("toTrade")
    @LoginRequire
    public String toTrade(HttpServletRequest request, HttpServletResponse response) {

        //未登录时使用redis存储商品数据
        return toTradeRedis(request, response);


        //未登录时使用cookie存储商品数据
       // return toTradeCookie(request, response);
    }

    /**
     * 未登录时使用cookie存储商品数据
     * @param request
     * @param response
     * @return
     */
    private String toTradeRedis(HttpServletRequest request, HttpServletResponse response) {
        //TODO 这里使用redis存储未登录数据  需完善
        //登录后获取用户id
        String userId = (String) request.getAttribute("userId");

        //从cookie中获取 uuid
        userKey = getUUID(request,response);

        //获取未登录时的购物车数据  因为在选中的时候已经更新到redis中了将选中状态 所以直接调用此方法就可以获取勾选时的商品
        List<CartInfo> cartListRedis = cartInfoService.getCartListRedis(userKey);

        //判断未登录时数据是否为空
        if (cartListRedis != null && cartListRedis.size() > 0) {
            //进行合并登录与未登录时的勾选数据
            cartInfoService.mergeToCartList(cartListRedis,userId);
            //合并之后调用方法 删除未登录时的cookie中的方法
            cartInfoService.deleteCartRedis(userKey);
        }
        //重定向到页面
        return "redirect://order.gmall.com/trade";
    }

    /**
     * 未登录时使用cookie存储商品数据
     * @param request
     * @param response
     * @return
     */
    private String toTradeCookie(HttpServletRequest request, HttpServletResponse response) {

        //登录后获取用户id
        String userId = (String) request.getAttribute("userId");

        //获取未登录时的购物车数据  因为在选中的时候已经更新到cookie中了将选中状态 所以直接调用此方法就可以获取勾选时的商品
        List<CartInfo> cartInfoList = cartCookieHandler.getCartList(request);

        //判断未登录时的数据是否为空
        if (cartInfoList != null && cartInfoList.size() > 0) {

            //进行合并登录与未登录时的勾选数据
            cartInfoService.mergeToCartList(cartInfoList,userId);

            //合并之后调用方法 删除未登录时的cookie中的方法
            cartCookieHandler.deleteCartCookie(request,response);
        }
        return "redirect://order.gmall.com/trade";
    }
}
