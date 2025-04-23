package com.github.dgaponov99.cs.onlinebot.config;

import com.ibasco.agql.protocols.valve.source.query.SourceQueryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SourceQueryConfig {

    @Bean(destroyMethod = "close")
    public SourceQueryClient sourceQueryClient() {
        return new SourceQueryClient();
    }

}
