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
}
