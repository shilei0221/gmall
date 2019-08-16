package com.baidu.gmall.usermanage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.baidu.gmall.UserInfoService;
import com.baidu.gmall.bean.UserAddress;
import com.baidu.gmall.bean.UserInfo;
import com.baidu.gmall.usermanage.mapper.UserAddressMapper;
import com.baidu.gmall.usermanage.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-14 16:40
 */
@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;



    /**
     * 查询所有用户
     * @return
     */
    @Override
    public List<UserInfo> getAll() {
        return userInfoMapper.selectAll();
    }

    /**
     * 根据用户id查询用户地址
     * @param userId
     * @return
     */
    @Override
    public List<UserAddress> getAddressAll(String userId) {

        Example example = new Example(UserAddress.class);

        example.createCriteria().andEqualTo("userId",userId);

        return userAddressMapper.selectByExample(example);
    }

    @Override
    public List<UserInfo> getUserByName(UserInfo userInfo) {

//        Example example = new Example(UserInfo.class);
//
//        example.createCriteria().andEqualTo("name",userInfo.getName());

        return userInfoMapper.select(userInfo);
    }

    @Override
    public UserInfo getUserInfo(String userId) {
        return userInfoMapper.selectByPrimaryKey(userId);
    }
}
