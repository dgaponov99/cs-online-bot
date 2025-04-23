package com.github.dgaponov99.cs.onlinebot;

import freemarker.template.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.abilitybots.api.objects.Reply;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import com.github.dgaponov99.cs.onlinebot.config.BotConfig;
import com.github.dgaponov99.cs.onlinebot.message.OnlineMessage;
import com.github.dgaponov99.cs.onlinebot.message.ServerMessage;
import com.github.dgaponov99.cs.onlinebot.service.SourceQueryService;

import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

import static org.telegram.telegrambots.abilitybots.api.objects.Locality.ALL;
import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.GROUP_ADMIN;
import static org.telegram.telegrambots.abilitybots.api.objects.Privacy.PUBLIC;
import static org.telegram.telegrambots.abilitybots.api.util.AbilityUtils.getChatId;

@Slf4j
@Component
public class CsOnlineBot extends AbilityBot implements SpringLongPollingBot {

    private final BotConfig botConfig;
    private final SourceQueryService sourceQueryService;
    private final Configuration freemarkerConfig;

    protected CsOnlineBot(BotConfig botConfig,
                          TelegramClient telegramClient,
                          SourceQueryService sourceQueryService,
                          Configuration freemarkerConfig) {
        super(telegramClient, botConfig.getUsername());
        this.botConfig = botConfig;
        this.sourceQueryService = sourceQueryService;
        this.freemarkerConfig = freemarkerConfig;
        this.onRegister();
    }

    @Override
    public long creatorId() {
        return botConfig.getCreatorId();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    private Map<Long, Set<String>> chatServers;

    public Ability addNewCsServer() {
        return Ability
                .builder()
                .name("add")
                .info("add new server info")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    if (chatServers == null) {
                        chatServers = getDb().getMap("CHAT_SERVERS");
                    }
                    var servers = Optional.ofNullable(chatServers.get(ctx.chatId())).orElseGet(HashSet::new);
                    servers.add(ctx.firstArg());
                    chatServers.put(ctx.chatId(), servers);
                    silent.send("Success add server: " + ctx.firstArg(), ctx.chatId());
                })
                .build();
    }

    public Ability removeCsServer() {
        return Ability
                .builder()
                .name("remove")
                .info("add new server info")
                .locality(ALL)
                .privacy(GROUP_ADMIN)
                .action(ctx -> {
                    if (chatServers == null) {
                        chatServers = getDb().getMap("CHAT_SERVERS");
                    }
                    var servers = Optional.ofNullable(chatServers.get(ctx.chatId())).orElseGet(HashSet::new);
                    servers.remove(ctx.firstArg());
                    chatServers.put(ctx.chatId(), servers);
                    silent.send("Success remove server: " + ctx.firstArg(), ctx.chatId());
                })
                .build();
    }

    public Ability getCsServers() {
        return Ability
                .builder()
                .name("servers")
                .info("Просмотреть все добавленные сервера")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    if (chatServers == null) {
                        chatServers = getDb().getMap("CHAT_SERVERS");
                    }
                    var servers = Optional.ofNullable(chatServers.get(ctx.chatId())).orElseGet(HashSet::new);
                    silent.send("Servers: " + servers, ctx.chatId());
                })
                .build();
    }

    public Ability showOnline() {
        return Ability
                .builder()
                .name("online")
                .info("Просмотреть онлайн на всех добавленных серверах")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    if (chatServers == null) {
                        chatServers = getDb().getMap("CHAT_SERVERS");
                    }
                    var servers = Optional.ofNullable(chatServers.get(ctx.chatId())).orElseGet(HashSet::new).stream()
                            .map(server -> sourceQueryService.getServerInfo(server.split(":")[0], Integer.parseInt(server.split(":")[1])))
                            .toList();

                    var onlineMessage = new OnlineMessage(servers, null);
                    var message = "Ошибка генерации сообщения";
                    try {
                        var template = freemarkerConfig.getTemplate("message/online.ftl");

                        var model = new HashMap<String, Object>();
                        model.put("online", onlineMessage);

                        var out = new StringWriter();
                        template.process(model, out);
                        message = out.toString();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    var sendMessage = new SendMessage(String.valueOf(ctx.chatId()), message);
                    sendMessage.enableHtml(true);
                    sendMessage.disableWebPagePreview();
                    sendMessage.setReplyMarkup(onlineMessage.getKeyboard());
                    try {
                        getTelegramClient().execute(sendMessage);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                })
                .build();
    }

    public Ability refresh() {
        return Ability
                .builder()
                .name("refresh")
                .info("Вкл/выкл автоматическое обновление")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    var replyToMessageId = ctx.update().getMessage().getReplyToMessage().getMessageId();

                    Map<Long, Map<Integer, String>> refreshChats = getDb().getMap("REFRESH");
                    var chatRefreshMap = Optional.ofNullable(refreshChats.get(ctx.chatId())).orElseGet(HashMap::new);

                    String result;
                    if (!ctx.firstArg().equals("off")) {
                        var updateButtonData = ctx.update().getMessage().getReplyToMessage().getReplyMarkup().getKeyboard().get(0).get(0).getCallbackData();
                        chatRefreshMap.put(replyToMessageId, updateButtonData);
                        result = "Задача на обновление успешно добавлена";
                    } else {
                        chatRefreshMap.remove(replyToMessageId);
                        result = "Задача на обновление успешно удалена";
                    }
                    refreshChats.put(ctx.chatId(), chatRefreshMap);

                    silent.send(result, ctx.chatId());
                })
                .build();
    }

    public Reply refreshOnline() {
        return Reply.of((bot, upd) -> {
                    try {
                        if (chatServers == null) {
                            chatServers = getDb().getMap("CHAT_SERVERS");
                        }
                        var servers = Optional.ofNullable(chatServers.get(getChatId(upd))).orElseGet(HashSet::new).stream()
                                .map(server -> sourceQueryService.getServerInfo(server.split(":")[0], Integer.parseInt(server.split(":")[1])))
                                .toList();
                        var onlineMessage = new OnlineMessage(servers, ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm z")));
                        var message = "Ошибка генерации сообщения";
                        try {
                            var template = freemarkerConfig.getTemplate("message/online.ftl");

                            var model = new HashMap<String, Object>();
                            model.put("online", onlineMessage);

                            var out = new StringWriter();
                            template.process(model, out);
                            message = out.toString();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        var dateTimePattern = Pattern.compile("Обновлено: (\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2})");
                        var messageDateTimeMatcher = dateTimePattern.matcher(((Message) upd.getCallbackQuery().getMessage()).getText());
                        var messageDateTime = "";
                        if (messageDateTimeMatcher.find()) {
                            messageDateTime = messageDateTimeMatcher.group(1);
                        }

                        var newMessageDateTime = "new";
                        var newMessageDateTimeMatcher = dateTimePattern.matcher(message);
                        if (newMessageDateTimeMatcher.find()) {
                            newMessageDateTime = newMessageDateTimeMatcher.group(1);
                        }

                        if (((Message) upd.getCallbackQuery().getMessage()).getText().contains("Игроки онлайн:")
                                || !messageDateTime.equals(newMessageDateTime)) {
                            var editMessage = new EditMessageText(message);
                            editMessage.setChatId(getChatId(upd));
                            editMessage.setMessageId(upd.getCallbackQuery().getMessage().getMessageId());
                            editMessage.setInlineMessageId(upd.getCallbackQuery().getInlineMessageId());
                            editMessage.enableHtml(true);
                            editMessage.disableWebPagePreview();
                            editMessage.setReplyMarkup(onlineMessage.getKeyboard());
                            try {
                                bot.getTelegramClient().execute(editMessage);
                            } catch (TelegramApiException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            var answerCallbackQuery = new AnswerCallbackQuery(upd.getCallbackQuery().getId());
                            answerCallbackQuery.setText("Данные о сервере актуальны");
                            answerCallbackQuery.setShowAlert(true);
                            try {
                                bot.getTelegramClient().execute(answerCallbackQuery);
                            } catch (TelegramApiException e) {
                                throw new RuntimeException(e);
                            }
                        }

                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                },
                (upd) -> upd.hasCallbackQuery() && upd.getCallbackQuery().getData().equals("online"));
    }

    public Reply showPlayers() {
        return Reply.of((bot, upd) -> {
                    try {
                        var serverInfo = sourceQueryService.getServerInfo(upd.getCallbackQuery().getData().split(":")[0], Integer.parseInt(upd.getCallbackQuery().getData().split(":")[1]));
                        var serverPlayers = sourceQueryService.getServerPlayers(upd.getCallbackQuery().getData().split(":")[0], Integer.parseInt(upd.getCallbackQuery().getData().split(":")[1]));
                        var serverMessage = new ServerMessage(serverInfo, serverPlayers, ZonedDateTime.now(ZoneId.of("Europe/Moscow")).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm z")));
                        var message = "Ошибка генерации сообщения";
                        try {
                            var template = freemarkerConfig.getTemplate("message/server.ftl");

                            var model = new HashMap<String, Object>();
                            model.put("server", serverMessage);

                            var out = new StringWriter();
                            template.process(model, out);
                            message = out.toString();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        var dateTimePattern = Pattern.compile("Обновлено: (\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2})");
                        var messageDateTimeMatcher = dateTimePattern.matcher(((Message) upd.getCallbackQuery().getMessage()).getText());
                        var messageDateTime = "";
                        if (messageDateTimeMatcher.find()) {
                            messageDateTime = messageDateTimeMatcher.group(1);
                        }

                        var newMessageDateTime = "new";
                        var newMessageDateTimeMatcher = dateTimePattern.matcher(message);
                        if (newMessageDateTimeMatcher.find()) {
                            newMessageDateTime = newMessageDateTimeMatcher.group(1);
                        }

                        if (!((Message) upd.getCallbackQuery().getMessage()).getText().contains("Игроки онлайн:")
                                || !messageDateTime.equals(newMessageDateTime)) {
                            var editMessage = new EditMessageText(message);
                            editMessage.setChatId(getChatId(upd));
                            editMessage.setMessageId(upd.getCallbackQuery().getMessage().getMessageId());
                            editMessage.setInlineMessageId(upd.getCallbackQuery().getInlineMessageId());
                            editMessage.enableHtml(true);
                            editMessage.disableWebPagePreview();
                            editMessage.setReplyMarkup(serverMessage.getKeyboard());
                            try {
                                bot.getTelegramClient().execute(editMessage);
                            } catch (TelegramApiException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            var answerCallbackQuery = new AnswerCallbackQuery(upd.getCallbackQuery().getId());
                            answerCallbackQuery.setText("Данные о сервере актуальны");
                            answerCallbackQuery.setShowAlert(true);
                            try {
                                bot.getTelegramClient().execute(answerCallbackQuery);
                            } catch (TelegramApiException e) {
                                throw new RuntimeException(e);
                            }
                        }

                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                },
                (upd) -> upd.hasCallbackQuery() && !upd.getCallbackQuery().getData().startsWith("online"));
    }
}
