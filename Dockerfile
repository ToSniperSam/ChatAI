# 基础镜像
FROM openjdk:8-jre-slim

# 作者
MAINTAINER yangla

# 配置
ENV PARAMS=""

# 时区
ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone



# 添加应用
ADD target/chatai-0.0.1-SNAPSHOT.jar /chatai.jar

ENTRYPOINT ["sh", "-c", "java -Dspring.profiles.active=prod -jar $JAVA_OPTS /chatai.jar $PARAMS"]

