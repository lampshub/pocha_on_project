package com.beyond.pochaon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;



@SpringBootApplication
@EnableJpaAuditing  //@MappedSuperclass 사용시 필요
public class pochaonApplication {

    public static void main(String[] args) {
        SpringApplication.run(pochaonApplication.class, args);
    }

}
