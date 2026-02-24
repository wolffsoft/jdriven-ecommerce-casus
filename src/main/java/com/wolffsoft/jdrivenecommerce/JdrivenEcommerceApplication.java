package com.wolffsoft.jdrivenecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class JdrivenEcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JdrivenEcommerceApplication.class, args);
    }

}
