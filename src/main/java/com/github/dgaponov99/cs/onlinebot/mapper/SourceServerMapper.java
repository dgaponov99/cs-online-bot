package com.github.dgaponov99.cs.onlinebot.mapper;

import com.ibasco.agql.protocols.valve.source.query.info.SourceServer;
import com.ibasco.agql.protocols.valve.source.query.players.SourcePlayer;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import com.github.dgaponov99.cs.onlinebot.dto.ServerInfoDTO;
import com.github.dgaponov99.cs.onlinebot.dto.ServerPlayerDTO;

@Mapper(componentModel = "spring")
public abstract class SourceServerMapper {

    @Mapping(target = "ip", expression = "java(sourceServer.getAddress().getHostName() + \":\" + sourceServer.getAddress().getPort())")
    public abstract ServerInfoDTO serverInfoToDTO(SourceServer sourceServer);

    public abstract ServerPlayerDTO serverPlayerToDTO(SourcePlayer sourcePlayer);

    @AfterMapping
    protected void enrichDTOFormatDuration(SourcePlayer sourcePlayer, @MappingTarget ServerPlayerDTO serverPlayerDTO) {
        var durationFormatted = DurationFormatUtils
                .formatDuration((long) sourcePlayer.getDuration() * 1000, "HH:mm:ss")
                .replace("00:", "");
        serverPlayerDTO.setDuration(durationFormatted);
    }

}



