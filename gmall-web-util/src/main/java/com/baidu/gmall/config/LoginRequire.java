package com.baidu.gmall.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Alei
 * @create 2019-08-27 18:39
 *
 * 自定义注解 用来实现在访问某些页面的时候进行登录
 */
@Target({ElementType.METHOD}) //定义注解只能使用在方法上
@Retention(RetentionPolicy.RUNTIME) //定义注解的生命周期在什么时候起作用  CLASS 在编译成.class 文件的时候起作用 SOURCE 在JVM运行的时候生效
//RUNTIME 在编译期与运行期都起作用
public @interface LoginRequire {

    //标注为当前注解在使用的时候默认指定为true  必须的 或者false 不是必须的
    boolean autoRedirect() default true;
}
