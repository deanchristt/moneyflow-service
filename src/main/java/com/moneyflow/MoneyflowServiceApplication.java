package com.moneyflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MoneyflowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoneyflowServiceApplication.class, args);
    }

}
