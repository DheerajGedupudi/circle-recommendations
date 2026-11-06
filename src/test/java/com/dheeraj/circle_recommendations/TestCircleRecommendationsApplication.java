package com.dheeraj.circle_recommendations;

import org.springframework.boot.SpringApplication;

public class TestCircleRecommendationsApplication {

	public static void main(String[] args) {
		SpringApplication.from(CircleRecommendationsApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
