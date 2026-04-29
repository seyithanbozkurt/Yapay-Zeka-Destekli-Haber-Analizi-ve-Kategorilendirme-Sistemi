FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Izole Spark child JVM icin ANTLR 4.9.3 (Hibernate ile classpath cakismasini onler); JRE imajinda .m2 yok
RUN mvn -q -DskipTests clean package \
    && mkdir -p /app/deps \
    && mvn -q org.apache.maven.plugins:maven-dependency-plugin:3.6.1:copy \
        -DoutputDirectory=/app/deps \
        -Dartifact=org.antlr:antlr4-runtime:4.9.3:jar

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir -p /app/deps
COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/deps/antlr4-runtime-4.9.3.jar /app/deps/antlr4-runtime-4.9.3.jar
ENV ML_CLASSIFIER_ANTLR493_JAR=/app/deps/antlr4-runtime-4.9.3.jar
EXPOSE 8989
# Spark driver (local): JDK 21 ile SecurityManager; docker-compose'da JAVA_TOOL_OPTIONS ile de verilebilir
ENTRYPOINT ["java","-Djava.security.manager=allow","-jar","app.jar"]