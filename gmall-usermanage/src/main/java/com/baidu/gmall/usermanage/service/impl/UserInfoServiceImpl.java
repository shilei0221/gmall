package com.baidu.gmall.usermanage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.baidu.gmall.UserInfoService;
import com.baidu.gmall.bean.UserAddress;
import com.baidu.gmall.bean.UserInfo;
import com.baidu.gmall.config.RedisUtil;
import com.baidu.gmall.usermanage.mapper.UserAddressMapper;
import com.baidu.gmall.usermanage.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;
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

    @Autowired
    private RedisUtil redisUtil;




    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24;



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

    /**
     * 根据用户信息查询用户是否存在
     *
     * @param userInfo
     * @return
     */
    @Override
    public UserInfo login(UserInfo userInfo) {

        //因为用户密码数据库是经过加密的  md5  所以在查询之前先获取用户密码 进行加密设置到对象中再去查询数据库
        String passwd = userInfo.getPasswd();

        String newPasswd = DigestUtils.md5DigestAsHex(passwd.getBytes());

        userInfo.setPasswd(newPasswd);

        UserInfo info = userInfoMapper.selectOne(userInfo);

        //判断info 是否为空 放入redis中
        if (info != null) {
            //获取jedis
            Jedis jedis = null;

            try {

                jedis = redisUtil.getJedis();

                //设置到redis中
                jedis.setex(userKey_prefix + info.getId() + userinfoKey_suffix,userKey_timeOut, JSON.toJSONString(info));

                return info;
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                if (jedis != null) {
                    jedis.close();
                }
            }
        }
        return null;
    }

    /**
     * 认证中心 通过用户id 认证用户是否登录 查看redis
     * @param userId
     * @return
     */
    @Override
    public UserInfo verify(String userId) {

        //去缓存中查询使用有用户登录的信息
        Jedis jedis = null;

        try {
            //获取jedis
            jedis = redisUtil.getJedis();

            //定义 key
            String key = userKey_prefix + userId + userinfoKey_suffix;

            String userJson = jedis.get(key);

            System.out.println(userJson);

            //判断用户信息是否存在
            if (userJson != null) {

                //延迟失效时间
                jedis.expire(key,userKey_timeOut);

                UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);

                return userInfo;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }finally {

            if (jedis != null) {
                jedis.close();
            }
        }

        return null;
    }
}
