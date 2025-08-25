package io.github.nokasegu.post_here;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PostHereApplication {

	public static void main(String[] args) {
		SpringApplication.run(PostHereApplication.class, args);
	}

}
