package com.github.dgaponov99.cs.onlinebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CsOnlineBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(CsOnlineBotApplication.class, args);
    }

}
