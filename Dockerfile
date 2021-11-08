FROM openjdk:8-jdk-alpine

ARG APP_JAR_NAME
ENV APP_JAR=/opt/service/wallet/$APP_JAR_NAME TZ=UTC

COPY target/$APP_JAR_NAME $APP_JAR

#RUN java $JAVA_OPTS -jar $APP_JAR
ENTRYPOINT java -jar $APP_JAR
