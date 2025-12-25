package io.pinkspider.leveluptogethermvp.noticeservice.core.config;

import feign.Client;
import feign.Logger;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

@Configuration
public class AdminFeignConfig {

    @Bean
    public Logger.Level adminFeignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public Retryer adminFeignRetryer() {
        return new Retryer.Default(1000, 2000, 3);
    }

    /**
     * 자체 서명 SSL 인증서를 허용하는 Feign Client 설정
     * 로컬/개발 환경에서 HTTPS 호출을 위해 필요
     */
    @Bean
    public Client feignClient() {
        try {
            TrustManager[] trustManagers = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new java.security.SecureRandom());

            return new Client.Default(
                sslContext.getSocketFactory(),
                (hostname, session) -> true
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Feign client with SSL bypass", e);
        }
    }
}
