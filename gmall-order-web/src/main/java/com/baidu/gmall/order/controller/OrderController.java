package com.baidu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baidu.gmall.UserInfoService;
import com.baidu.gmall.bean.UserAddress;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Alei
 * @create 2019-08-14 17:00
 */
@RestController
public class OrderController {

    @Reference
    private UserInfoService userInfoService;

    @RequestMapping("order")
    public List<UserAddress> order(HttpServletRequest request) {

        String userId = request.getParameter("userId");

        List<UserAddress> addressAll = userInfoService.getAddressAll(userId);

        return addressAll;
    }


}
