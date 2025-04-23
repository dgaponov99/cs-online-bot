package com.github.dgaponov99.cs.onlinebot.service;

import com.ibasco.agql.protocols.valve.source.query.SourceQueryClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.github.dgaponov99.cs.onlinebot.dto.ServerInfoDTO;
import com.github.dgaponov99.cs.onlinebot.dto.ServerPlayerDTO;
import com.github.dgaponov99.cs.onlinebot.mapper.SourceServerMapper;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SourceQueryService {

    private final SourceQueryClient sourceQueryClient;
    private final SourceServerMapper sourceServerMapper;

    public ServerInfoDTO getServerInfo(String hostname, int port) {
        var address = new InetSocketAddress(hostname, port);
        var response = sourceQueryClient.getInfo(address).join();
        return sourceServerMapper.serverInfoToDTO(response.getResult());
    }

    public List<ServerPlayerDTO> getServerPlayers(String hostname, int port) {
        var address = new InetSocketAddress(hostname, port);
        var response = sourceQueryClient.getPlayers(address).join();
        return response.getResult().stream()
                .sorted(Collections.reverseOrder((p1, p2) -> {
                    var scoreCompare = Integer.compare(p1.getScore(), p2.getScore());
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                    return -1 * Float.compare(p1.getDuration(), p2.getDuration());
                }))
                .map(sourceServerMapper::serverPlayerToDTO)
                .toList();
    }

}
