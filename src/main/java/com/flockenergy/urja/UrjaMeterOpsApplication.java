package com.flockenergy.urja;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class UrjaMeterOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrjaMeterOpsApplication.class, args);
    }
}
