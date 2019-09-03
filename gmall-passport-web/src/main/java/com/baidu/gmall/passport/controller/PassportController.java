package com.baidu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baidu.gmall.UserInfoService;
import com.baidu.gmall.bean.UserInfo;
import com.baidu.gmall.config.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alei
 * @create 2019-08-27 11:03
 *
 * 实现登录控制器
 */
@Controller
public class PassportController {

    @Reference
    private UserInfoService userInfoService;

    @Value("${token.key}")
    private String key;

    /**
     * 跳转登录页面
     * @return
     */
    @RequestMapping("index")
    public String index(HttpServletRequest request) {

        //从请求路径中获取 originUrl
        String originUrl = request.getParameter("originUrl");

        //将获取到的url 放入域中 传递给前端显示
        request.setAttribute("originUrl",originUrl);

        return "index";
    }


    /**
     * 实现登录功能
     * @param userInfo
     * @return
     */
    @RequestMapping("login")
    @ResponseBody
    public String login (UserInfo userInfo, HttpServletRequest request) {

        //获取 ip 地址 这里只能使用域名访问 如果使用localhost 访问则获取不到 ip地址 因为 nginx 中没有相关配置
        String salt = request.getHeader("X-forwarded-for");

        //判断传入的用户信息是否为空
        if (userInfo != null) {

            //从数据库中查询看是否存在该用户
            UserInfo info = userInfoService.login(userInfo);

            //如果存在该用户
            if (info != null) {

                //根据用户信息生成token
                Map<String,Object> map = new HashMap<>();

                //将用户相关信息放入map中
                map.put("userId",info.getId());
                map.put("nickName",info.getNickName());

                //调用jwt工具类获取token值
                String token = JwtUtil.encode(key, map, salt);

                //返回token值
                return token;
            } else {

                //不存在返回失败
                return "fail";
            }
        }
        return "fail";
    }

    /**
     * 认证中心 检查用户是否登录 登录的话延长过期时间
     * @param request
     * @return
     */
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request) {

        //获取url中的token值
        String token = request.getParameter("token");

        //获取ip
        String salt = request.getHeader("X-forwarded-for");

        //检查token  进行解密token 看是否存在用户信息
        Map<String, Object> map = JwtUtil.decode(token, key, salt);

        //判断用户信息是否为空
        if (map != null && map.size() > 0) {

            //从map中获取用户id
            String userId = (String)map.get("userId");

            //调用业务层方法查询redis中是否存在该用户 存在则登录 不存在 则未登录
            UserInfo userInfo = userInfoService.verify(userId);

            if (userInfo != null) {
                //说明存在该用户 已登录
                return "success";
            }
        }


        return "fail";
    }
}
