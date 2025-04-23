package com.github.dgaponov99.cs.onlinebot.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import com.github.dgaponov99.cs.onlinebot.dto.ServerInfoDTO;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class OnlineMessage {

    private List<ServerInfoDTO> serverInfoList;

    private String updateDate;

    public InlineKeyboardMarkup getKeyboard() {
        var buttons = new ArrayList<InlineKeyboardButton>();
        for (int i = 0; i < serverInfoList.size(); i++) {
            buttons.add(InlineKeyboardButton
                    .builder()
                    .text(String.valueOf(i + 1))
                    .callbackData(serverInfoList.get(i).getIp())
                    .build());
        }
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton
                                .builder()
                                .text("Обновить")
                                .callbackData("online")
                                .build()
                ))
                .keyboardRow(new InlineKeyboardRow(buttons))
                .build();
    }

}
