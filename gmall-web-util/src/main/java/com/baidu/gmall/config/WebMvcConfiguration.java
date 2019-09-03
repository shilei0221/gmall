package com.baidu.gmall.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author Alei
 * @create 2019-08-27 18:24
 *
 * 配置类 使我们写的 拦截器生效
 */
@Configuration //声明一个配置类
public class WebMvcConfiguration extends WebMvcConfigurerAdapter {

    @Autowired
    private AuthInterceptor authInterceptor; //注入我们定义的拦截器

    public void addInterceptors(InterceptorRegistry registry) {

        //添加拦截器 添加拦截的路径 为所有请求
        registry.addInterceptor(authInterceptor).addPathPatterns("/**");

        //将赋值好的拦截器传入父类生效
        super.addInterceptors(registry);
    }
}
