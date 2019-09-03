package com.baidu.gmall.order.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * @author Alei
 * @create 2019-09-02 19:02
 *
 * 实现利用多线程实现异步并发操作
 */
@Configuration
@EnableAsync
public class AsyOrderConfig implements AsyncConfigurer {

    /*
        执行者 执行具体的异步操作 创建线程池  初始化
     */
    @Override
    public Executor getAsyncExecutor() {

        //获取线程池  数据库的连接池
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();

        //设置核心线程数
        threadPoolTaskExecutor.setCorePoolSize(10);

        //设置最大连接数  线程池最大数量
        threadPoolTaskExecutor.setMaxPoolSize(100);

        //设置等待队列 如果10个线程不够 可以有100个线程进去等带队列 阻塞队列 缓存池
        threadPoolTaskExecutor.setQueueCapacity(100);

        //初始化操作
        threadPoolTaskExecutor.initialize();

        return threadPoolTaskExecutor;
    }

    /*
        异常处理器 就是在处理运行时异常无法捕获的一些异常会自动执行此方法 可以自定义异常实现
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return null;
    }
}
