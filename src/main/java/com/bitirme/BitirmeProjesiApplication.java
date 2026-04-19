package com.bitirme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BitirmeProjesiApplication {

	static {
		// Spark/Hadoop Java 21+ ortamlarda Subject.getSubject çağrısı için JVM açılışında da gerekli.
		System.setProperty("java.security.manager", "allow");
	}

	public static void main(String[] args) {
		System.setProperty("java.security.manager", "allow");
		SpringApplication.run(BitirmeProjesiApplication.class, args);
	}

}
