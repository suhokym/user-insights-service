FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon -q
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", \
  "--add-opens=java.base/java.lang=ALL-UNNAMED", \
  "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED", \
  "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED", \
  "--add-opens=java.base/java.io=ALL-UNNAMED", \
  "--add-opens=java.base/java.net=ALL-UNNAMED", \
  "--add-opens=java.base/java.nio=ALL-UNNAMED", \
  "--add-opens=java.base/java.util=ALL-UNNAMED", \
  "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED", \
  "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED", \
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED", \
  "--add-opens=java.base/sun.nio.cs=ALL-UNNAMED", \
  "--add-opens=java.base/sun.security.action=ALL-UNNAMED", \
  "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED", \
  "-jar", "app.jar"]
