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

    public void setSubject(String v) {
        this.subject = v;
    }

    public void setPublicKey(String v) {
        this.publicKey = v;
    }

    public void setPrivateKey(String v) {
        this.privateKey = v;
    }
}
