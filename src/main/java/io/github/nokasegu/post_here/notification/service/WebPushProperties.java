package io.github.nokasegu.post_here.notification.service;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "webpush")
public class WebPushProperties {
    private String subject;
    private String publicKey;
    private String privateKey;

    // ✅ @ConfigurationProperties는 setter 필요
    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}

