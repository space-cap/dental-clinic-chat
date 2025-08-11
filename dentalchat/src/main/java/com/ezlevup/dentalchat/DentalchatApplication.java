package com.ezlevup.dentalchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DentalchatApplication {

	public static void main(String[] args) {
		SpringApplication.run(DentalchatApplication.class, args);
	}

}
