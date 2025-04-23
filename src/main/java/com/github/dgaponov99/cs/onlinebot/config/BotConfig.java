package com.github.dgaponov99.cs.onlinebot.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Getter
@Configuration
public class BotConfig {

    @Value("${bot.token}")
    private String token;
    @Value("${bot.username}")
    private String username;
    @Value("${bot.creatorId}")
    private long creatorId;

    @Bean
    protected TelegramClient telegramClient() {
        return new OkHttpTelegramClient(getToken());
    }

}
