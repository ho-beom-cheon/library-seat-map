package com.libraryseatmap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LibrarySeatMapBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibrarySeatMapBackendApplication.class, args);
	}

}
