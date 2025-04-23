package com.github.dgaponov99.cs.onlinebot.scheduler;

import freemarker.template.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.github.dgaponov99.cs.onlinebot.CsOnlineBot;
import com.github.dgaponov99.cs.onlinebot.message.OnlineMessage;
import com.github.dgaponov99.cs.onlinebot.message.ServerMessage;
import com.github.dgaponov99.cs.onlinebot.service.SourceQueryService;

import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshScheduler {

    private final SourceQueryService sourceQueryService;
    private final CsOnlineBot csOnlineBot;
    private final Configuration freemarkerConfig;

    @Scheduled(cron = "${refresh.cron}")
    public void refresh() {
        log.info("Refreshing bot...");
        Map<Long, Map<Integer, String>> refreshChats = csOnlineBot.getDb().getMap("REFRESH");
        for (Map.Entry<Long, Map<Integer, String>> chatRefresh : refreshChats.entrySet()) {
            var chatId = chatRefresh.getKey();
            var messages = chatRefresh.getValue();
            for (int messageId : messages.keySet()) {
                var messageType = messages.get(messageId);
                var model = new HashMap<String, Object>();
                String templateName;
                if (messageType.equals("online")) {
                    templateName = "online";
                    Map<Long, Set<String>> chatServers = csOnlineBot.getDb().getMap("CHAT_SERVERS");
                    var servers = Optional.ofNullable(chatServers.get(chatId)).orElseGet(HashSet::new)
                            .stream()
                            .map(server -> sourceQueryService.getServerInfo(server.split(":")[0], Integer.parseInt(server.split(":")[1])))
                            .toList();
                    var onlineMessage = new OnlineMessage(servers, ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm z")));
                    model.put("online", onlineMessage);
                } else {
                    templateName = "server";
                    var serverInfo = sourceQueryService.getServerInfo(messageType.split(":")[0], Integer.parseInt(messageType.split(":")[1]));
                    var serverPlayers = sourceQueryService.getServerPlayers(messageType.split(":")[0], Integer.parseInt(messageType.split(":")[1]));
                    var serverMessage = new ServerMessage(serverInfo, serverPlayers, ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm z")));
                    model.put("server", serverMessage);
                }
                templateName = "message/" + templateName + ".ftl";
                var message = "Ошибка генерации сообщения";
                try {
                    var template = freemarkerConfig.getTemplate(templateName);
                    var out = new StringWriter();
                    template.process(model, out);
                    message = out.toString();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                var editMessage = new EditMessageText(message);
                editMessage.setChatId(chatId);
                editMessage.setMessageId(messageId);
                editMessage.enableHtml(true);
                editMessage.disableWebPagePreview();
                try {
                    csOnlineBot.getTelegramClient().execute(editMessage);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
