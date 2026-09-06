# 多阶段构建：WMS微服务通用Dockerfile
# 用法：docker build --build-arg MODULE=wms-core -t xwms/wms-core:latest .

FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
ARG MODULE
COPY pom.xml .
COPY wms-common/pom.xml wms-common/
COPY ${MODULE}/pom.xml ${MODULE}/
RUN mvn dependency:go-offline -B || true
COPY . .
RUN mvn clean package -pl ${MODULE} -am -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG MODULE
COPY --from=builder /build/${MODULE}/target/${MODULE}.jar app.jar
RUN addgroup -S app && adduser -S app -G app && chown -R app:app /app
USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseZGC -Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
