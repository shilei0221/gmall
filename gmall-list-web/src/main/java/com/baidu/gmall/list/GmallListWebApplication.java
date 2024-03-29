package com.baidu.gmall.list;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.baidu.gmall")
public class GmallListWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallListWebApplication.class, args);
    }

}
