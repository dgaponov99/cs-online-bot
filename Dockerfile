FROM azul/zulu-openjdk-alpine:21
RUN mkdir /app
WORKDIR /app
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app/cs-online-bot.jar
CMD exec java -jar cs-online-bot.jar -Dfile.encoding=UTF-8 ${JAVA_OPTS}