package com.everypoll.pollService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableJpaAuditing
@EnableKafka
@EnableFeignClients
@SpringBootApplication(scanBasePackages = { "com.everypoll.pollService", "com.everypoll.common" })
public class PollServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PollServiceApplication.class, args);
	}

}
