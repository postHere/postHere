// src/main/java/io/github/nokasegu/post_here/common/config/CryptoProviderConfig.java
package io.github.nokasegu.post_here.common.config;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Configuration;

import java.security.Security;

@Configuration
public class CryptoProviderConfig {

    @PostConstruct
    public void registerBouncyCastleOnce() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
}
