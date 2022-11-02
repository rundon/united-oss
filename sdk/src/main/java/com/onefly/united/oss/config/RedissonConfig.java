package com.onefly.united.oss.config;

import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.commons.lang.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

@Configuration
@EnableConfigurationProperties({RedisProperties.class})
public class RedissonConfig {

    @Bean
    public Config config(RedisProperties redisProperties) throws Exception {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort())
                .setDatabase(redisProperties.getDatabase())
                .setTimeout((int) redisProperties.getTimeout().toMillis())
                .setConnectionMinimumIdleSize(10)
                .setConnectionPoolSize(64)
                .setDnsMonitoringInterval(5000)
                .setSubscriptionConnectionMinimumIdleSize(1)
                .setSubscriptionConnectionPoolSize(50)
                .setSubscriptionsPerConnection(5)
                .setRetryAttempts(3)
                .setRetryInterval(1500)
                .setTimeout(3000)
                .setConnectTimeout(10000)
                .setIdleConnectionTimeout(10000);
        if (StringUtils.isNotBlank(redisProperties.getPassword())) {
            config.useSingleServer().setPassword(redisProperties.getPassword());
        }
        Codec codec = (Codec) ClassUtils.forName("org.redisson.codec.JsonJacksonCodec", ClassUtils.getDefaultClassLoader()).newInstance();
        config.setCodec(codec);
        config.setThreads(4);
        config.setEventLoopGroup(new NioEventLoopGroup());
        return config;
    }

    @Bean
    RedissonClient redissonClient(Config config) {
        return Redisson.create(config);
    }
}
