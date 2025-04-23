package com.github.dgaponov99.cs.onlinebot.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import com.github.dgaponov99.cs.onlinebot.dto.ServerInfoDTO;
import com.github.dgaponov99.cs.onlinebot.dto.ServerPlayerDTO;

import java.util.List;

@Data
@AllArgsConstructor
public class ServerMessage {

    private ServerInfoDTO serverInfo;
    private List<ServerPlayerDTO> serverPlayers;

    private String updateDate;

    public InlineKeyboardMarkup getKeyboard() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton
                                .builder()
                                .text("Обновить")
                                .callbackData(serverInfo.getIp())
                                .build(),
                        InlineKeyboardButton
                                .builder()
                                .text("Назад")
                                .callbackData("online")
                                .build()
                ))
                .build();
    }

}
