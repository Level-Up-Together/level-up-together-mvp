package io.pinkspider.leveluptogethermvp.noticeservice.core.config;

import feign.Logger;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
