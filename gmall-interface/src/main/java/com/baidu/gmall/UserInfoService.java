package com.baidu.gmall;

import com.baidu.gmall.bean.UserAddress;
import com.baidu.gmall.bean.UserInfo;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-14 16:39
 */
public interface UserInfoService {

    /**
     * 查询全部
     * @return
     */
    List<UserInfo> getAll();

    /**
     * 通过用户id查询用户的地址
     * @return
     */
    List<UserAddress> getAddressAll(String userId);


    /**
     * 根据名字查询
     * @param
     * @return
     */
    List<UserInfo> getUserByName(UserInfo userInfo);

    UserInfo getUserInfo(String userId);

    /**
     * 根据用户信息查询用户是否存在
     *
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

    /**
     * 认证中心 通过用户id 认证用户是否登录 查看redis
     * @param userId
     * @return
     */
    UserInfo verify(String userId);
}
