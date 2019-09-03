package com.baidu.gmall.order.task;

import com.alibaba.dubbo.config.annotation.Reference;
import com.baidu.gmall.OrderService;
import com.baidu.gmall.bean.OrderInfo;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Alei
 * @create 2019-09-02 18:31
 */
@EnableScheduling //开启定时任务
@Component //注入容器中
public class OrderTask {

    @Reference
    private OrderService orderService;

    //5 每分钟的第五秒
    // 0/5 每隔五秒执行一次

    /**
     * 代表每分钟的第五秒执行方法
     */
//    @Scheduled(cron = "5 * * * * ?")
//    public void work() {
//        System.out.println("Thread ==== > " + Thread.currentThread().getName());
//    }

    /**
     * 代表每五秒执行一次方法
     */
//    @Scheduled(cron = "0/5 * * * * ?")
//    public void work1() {
//        System.out.println("Thread ==== > " + Thread.currentThread().getName());
//    }

    /**
     * 代表每隔20秒检查一次订单状态  查到过期订单 进行关闭
     */
    @Scheduled(cron = "0/20 * * * * ?")
    public void checkOrder() {
        System.out.println("开始处理过期订单~");

        long statr = System.currentTimeMillis();

        /*获取所有的过期订单返回一个订单集合*/
        List<OrderInfo> expiredOrderList = orderService.getExpiredOrderList();

        /*遍历所有的过期订单集合*/
        for (OrderInfo orderInfo : expiredOrderList) {

            //处理未完成的订单
            orderService.execExpiredOrder(orderInfo);
        }

        long stop = System.currentTimeMillis();

        System.out.println("一共处理：" + expiredOrderList.size() + " 个订单 共消耗时间： " + (stop - statr) + "毫秒");
    }
}
