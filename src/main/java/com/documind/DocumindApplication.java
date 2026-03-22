package com.documind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.documind.repository")
@EntityScan(basePackages = "com.documind.model")
public class DocumindApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumindApplication.class, args);
	}
}