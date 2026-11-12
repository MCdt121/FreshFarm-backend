package com.briup.famerdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FamerdemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(FamerdemoApplication.class, args);
    }

}
