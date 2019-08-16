package com.baidu.gmall.usermanage.controller;

import com.baidu.gmall.UserInfoService;
import com.baidu.gmall.bean.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Alei
 * @create 2019-08-14 16:43
 */
@RestController
public class UserInfoController {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * 查询所有
     * @return
     */
    @RequestMapping("getAll")
    public List<UserInfo> getAll() {
        return userInfoService.getAll();
    }

    @RequestMapping("getUserByName")
    public List<UserInfo> getUserByName(UserInfo userInfo) {

       List<UserInfo> userInfoList = userInfoService.getUserByName(userInfo);

       return userInfoList;

    }

    @RequestMapping("id")
    public UserInfo getUserInfo(@RequestParam("userId") String userId) {
        UserInfo userInfo = userInfoService.getUserInfo(userId);
        return userInfo;
    }


}
