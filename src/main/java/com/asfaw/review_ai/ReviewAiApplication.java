package com.asfaw.review_ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ReviewAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReviewAiApplication.class, args);
	}

}
