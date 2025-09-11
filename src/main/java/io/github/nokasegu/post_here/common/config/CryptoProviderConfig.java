// src/main/java/io/github/nokasegu/post_here/common/config/CryptoProviderConfig.java
package io.github.nokasegu.post_here.common.config;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Configuration;

import java.security.Security;

/**
 * CryptoProviderConfig
 * <p>
 * 역할(정확 명칭)
 * - 애플리케이션 기동 시 한 번만 BouncyCastle 보안 프로바이더를 등록합니다.
 * - Web Push 전송(webpush-java) 및 JOSE(jose4j) 처리 과정에서 사용하는
 * ECDH-ES, ECDSA(P-256, SHA-256) 등 EC 기반 알고리즘 구현을 안정적으로 제공하기 위함입니다.
 * <p>
 * 동작
 * - @PostConstruct 시점에 Security.getProvider("BC") 존재 여부를 확인하고,
 * 없으면 Security.addProvider(new BouncyCastleProvider())로 등록합니다.
 * - 이미 등록되어 있으면 재등록하지 않습니다(멱등).
 * <p>
 * 사용/의존 맥락
 * - 서버: WebPushService에서 nl.martijndwars.webpush.PushService 사용 시
 * 내부적으로 jose4j/EC 암호화가 수행됩니다. 일부 JDK/환경에서 기본 프로바이더만으로는
 * 필요한 구현이 부족하거나 상호운용성 이슈가 있어 BouncyCastle 사용을 권장합니다.
 * <p>
 * 배포/보안 메모
 * - FIPS 환경이거나 정책상 BC 대신 BCFIPS를 써야 하는 경우, BouncyCastleProvider 대신
 * BouncyCastleFipsProvider로 교체하고 관련 알고리즘/키 사이즈 정책을 점검해야 합니다.
 * - 프로바이더 등록 순서가 중요해지는 환경에서는 addProvider 대신 insertProviderAt 사용을 고려하세요.
 */
@Configuration
public class CryptoProviderConfig {

    @PostConstruct
    public void registerBouncyCastleOnce() {
        // 이미 등록되어 있으면 재등록하지 않음(멱등 보장)
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
}
