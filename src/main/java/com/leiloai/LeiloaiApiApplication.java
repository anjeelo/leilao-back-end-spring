package com.leiloai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LeiloaiApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeiloaiApiApplication.class, args);
	}

}
