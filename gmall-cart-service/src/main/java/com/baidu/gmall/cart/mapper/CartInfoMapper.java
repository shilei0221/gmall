package com.baidu.gmall.cart.mapper;

import com.baidu.gmall.bean.CartInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-28 16:31
 *
 *
 */
public interface CartInfoMapper extends Mapper<CartInfo> {

    /**
     * 根据用户id 查询数据库中该用户对应的购物车
     * @param userId
     * @return
     */
    List<CartInfo> selectCartListWithCurPrice(String userId);
}
