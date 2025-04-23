<#-- @ftlvariable name="online" type="com.github.dgaponov99.cs.onlinebot.message.OnlineMessage" -->
<#list online.serverInfoList as server>
${server?index + 1}. <b>${server.name?html}</b>
Адрес сервера: <code>${server.ip?html}</code>
Карта: <b>${server.mapName?html}</b>
Онлайн: ${server.numOfPlayers}/${server.maxPlayers}

</#list>
<#if online.updateDate?has_content>
Обновлено: ${online.updateDate?html}
</#if>