package com.baidu.gmall;

import com.baidu.gmall.bean.CartInfo;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-28 16:31
 *
 *  购物车接口
 */
public interface CartInfoService {

    /**
     *  添加数据到购物车 分为登录状态与未登录状态
     * @param userId
     * @param skuId
     * @param skuNum
     */
    void addToCart(String userId,String skuId,Integer skuNum);

    /**
     * 用户登录状态从redis 中获取数据 没有的话 从数据库获取数据
     * @param userId
     * @return
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 进行合并购物车
     * @param cartListCK
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId);

    /**
     * 添加数据到redis中 未登录状态下
     * @param skuId
     * @param userKey
     * @param i
     */
    void addToCartRedis(String skuId, String userKey, int i);

    /**
     * 根据key 删除redis中数据
     * @param userKey
     */
    void deleteCartRedis(String userKey);

    /**
     * 从redis中获取数据显示
     * @param userKey
     * @return
     */
    List<CartInfo> getCartListRedis(String userKey);

    /**
     * 更新redis中的数据的勾选状态
     * @param skuId
     * @param isChecked
     * @param userId
     */
    void checkCart(String skuId, String isChecked, String userId);

    /**
     *  获取购物车中的选中商品
     * @param userId
     * @return
     */
    List<CartInfo> getcartCheckedList(String userId);

    /**
     * 根据用户id 查询该用户对应的购物车数据
     *
     * @param userId
     * @return
     */
    List<CartInfo> loadCartCache(String userId) ;

    /**
     * 用户未登录下存入redis中 改变其勾选状态
     * @param skuId
     * @param isChecked
     * @param userKey
     */
    void checkCartRedis(String skuId, String isChecked, String userKey);

    /**
     * 在支付成功之后 同步回调通知中删除勾选时的购物车商品  两个购物车 勾选购物车 与 用户购物车
     * @param userId
     */
    void deleteCheckedCart(String userId);
}
