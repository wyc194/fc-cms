# 第一阶段：构建层
FROM eclipse-temurin:21-jre-jammy as builder
WORKDIR /application
ARG JAR_FILE=*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

# 第二阶段：运行层
FROM eclipse-temurin:21-jre-jammy
WORKDIR /application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./

# 设置 JVM 内存参数
ENV JAVA_OPTS="-Xmx1536m -Xms1536m -XX:+UseG1GC"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
