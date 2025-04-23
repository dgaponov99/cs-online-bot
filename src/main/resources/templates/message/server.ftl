<#-- @ftlvariable name="server" type="com.github.dgaponov99.cs.onlinebot.message.ServerMessage" -->
<b>${server.serverInfo.name?html}</b>
Адрес сервера: <code>${server.serverInfo.ip?html}</code>
Карта: <b>${server.serverInfo.mapName?html}</b>
Онлайн: ${server.serverInfo.numOfPlayers}/${server.serverInfo.maxPlayers}

Игроки онлайн:
<#list server.serverPlayers as player>
${player?index + 1}. <b>${player.name?html}</b>, фраги: ${player.score}, время: ${player.duration}
</#list>

<#if server.updateDate?has_content>
Обновлено: ${server.updateDate?html}
</#if>