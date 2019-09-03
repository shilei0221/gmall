package com.baidu.gmall.config;

import com.alibaba.fastjson.JSON;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.Map;

/**
 * @author Alei
 * @create 2019-08-27 18:01
 *
 * 拦截器 拦截控制器请求 对其进行操作 判断请求是否需要登录 进行登录
 */
@Component //注入到容器中 可以自动装配使用
public class AuthInterceptor extends HandlerInterceptorAdapter {

    /**
     * 页面渲染之前执行
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //获取 url 中的 token  判断是否为空 进行操作
        String token = request.getParameter("newToken");

        //如果 token 不为空 将token 放入 cookie 中 方便下次请求时候获取token值进行判断是否登录状态
        if (token != null) {

            //将token放入cookie中 设置过期时间 一周  不进行编码
            CookieUtil.setCookie(request,response,"token",token,WebConst.COOKIE_MAXAGE,false);
        }

        //如果token 为空 说明 cookie 中可能存在该token值了
        if (token == null) {
            //从cookie中获取 token值  从请求域中获取 token 默认不变吗
            token = CookieUtil.getCookieValue(request,"token",false);
        }

        //如果token不为空
        if (token != null) {
            //去获取token中的值 进行解密 得出用户信息 最终将用户信息放入域中
            Map map = getUserMapByToken(token);

            if (map != null && map.size() > 0) {
                //获取 map 中的 用户信息
                String nickName = (String) map.get("nickName");
                //放入域中 在页面获取 显示用户信息
                request.setAttribute("nickName", nickName);
            }
        }

        /*
            以下方法实现自定义注解来判断用户是否登录 在请求标注有LoginRequire 注解的方法 进行拦截
            判断此方法是否处于登录状态 如果是 放行 如果不是 则进行登录

            检查业务方法是否需要用户登录，如果需要就把cookie中的token和当前登录人的ip地址发给远程服务器进行登录验证，

            返回的result是验证结果 success 或者 fail。如果验证未登录，直接重定向到登录页面。
         */
        //将object 的handler 对象 强转为 handlerMethod 对象 获取其中的注解信息 进行判断
        HandlerMethod handlerMethod = (HandlerMethod) handler;

        //获取标注为 LoginRequire 注解的方法
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);

        //判断此方法是否为空  进行拦截 放行
        if (methodAnnotation != null) {

            //获取ip地址 盐值 做路径跳转
            String salt = request.getHeader("X-forwarded-for");

            //因为@RequestMapping 注解 默认使用的是 get 请求 所以使用get请求 访问该路径(此注解可以访问get与post请求)
            //最终认证返回的结果  判断认证之后是否成功
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&salt=" + salt);

            if ("success".equals(result)) {
                //说明认证成功 从 调用解密token的方法 获取用户信息
                Map map = getUserMapByToken(token);

                //获取map 中的 用户id
                String userId = (String) map.get("userId");

                request.setAttribute("userId",userId);


                return true;
            } else {
                //说明认证失败了
                //判断该注解中的默认值 是否为true  默认为true
                if (methodAnnotation.autoRedirect()) {

                    //获取url 将其转换为字符串
                    String requestURL = request.getRequestURL().toString();
                    System.out.println("requestURL : 编码前  " + requestURL);

                    //因为请求路径的时候需要将url 进行编码
                    String encodeURL = URLEncoder.encode(requestURL,"utf-8");
                    System.out.println("encodeURL : 编码后  " + encodeURL);

                    //重定向到请求路径中  说明认证失败 没有登录 进行重定向到登录页面
                    response.sendRedirect(WebConst.LOGIN_ADDRESS + "?originUrl=" + encodeURL);

                    /*
                    此处return false  是因为如果后期维护的时候需要添加多个拦截器的时候
                    此处return false就不会继续往下执行后续的拦截器了 如果是单个拦截器的话 false 与 true 没什么区别
                    */
                    return  false;
                }
            }
        }

        return true;
    }

    /**
     * 解析token值  得出用户信息 
     * @param token
     * @return
     */
    private Map getUserMapByToken(String token) {
        
            //因为token有三部分 所以进行分割 获取中间的私有部分 用户信息
            String tokenUserInfo  = StringUtils.substringBetween(token, ".");

            //因为获取到的用户信息为编码后的字符串 所以进行解码
            Base64UrlCodec base64UrlCodec = new Base64UrlCodec();

            byte[] decode = base64UrlCodec.decode(tokenUserInfo);

            //定义字符串 进行将byte数组转换成字符串 在转换map 返回
            String tokenJson = null;

            try {
                //转换为字符串 utf-8格式
                tokenJson = new String(decode,"utf-8");
            } catch (Exception e) {
                e.printStackTrace();
            }

            //将字符串转换为map 返回
            Map map = JSON.parseObject(tokenJson, Map.class);

            return map;
    }

    /**
     * 页面渲染之后  视图解析之前执行
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }


    /**
     * 视图解析之后执行
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }

}
