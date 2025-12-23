package com.bitirme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BitirmeProjesiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BitirmeProjesiApplication.class, args);
	}

}
