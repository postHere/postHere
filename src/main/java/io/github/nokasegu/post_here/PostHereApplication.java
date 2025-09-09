package io.github.nokasegu.post_here;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@ConfigurationPropertiesScan //  @ConfigurationProperties 클래스들을 자동 스캔해서 빈으로 등록하기 위함
@SpringBootApplication
@EnableJpaAuditing
public class PostHereApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostHereApplication.class, args);
    }

}
