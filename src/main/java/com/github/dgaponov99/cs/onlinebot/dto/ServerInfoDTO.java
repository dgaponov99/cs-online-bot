package com.github.dgaponov99.cs.onlinebot.dto;

import lombok.Data;

@Data
public class ServerInfoDTO {

    private String name;
    private String ip;
    private String mapName;
    private int numOfPlayers;
    private int maxPlayers;

}
