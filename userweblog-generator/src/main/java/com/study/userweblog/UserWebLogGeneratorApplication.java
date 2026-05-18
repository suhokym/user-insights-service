package com.study.userweblog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UserWebLogGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserWebLogGeneratorApplication.class, args);
    }
}
