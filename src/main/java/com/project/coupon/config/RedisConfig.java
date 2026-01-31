package com.project.coupon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories
public class RedisConfig {
    // stringRedisTemplate은 RedisAutoConfiguration에서 자동 등록됨 (RedisTemplate<String, String>)
}
