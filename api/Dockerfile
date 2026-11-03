# syntax=docker/dockerfile:1
# Build em dois estágios: o primeiro tem o JDK completo e compila; o segundo leva só
# o JRE e o jar. A imagem final fica bem menor e sem ferramentas de build.

# ---------- Estágio 1: build ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copia primeiro só o que define as dependências. Enquanto o pom.xml não mudar,
# o Docker reaproveita esta camada e não baixa tudo de novo a cada build.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q dependency:go-offline

COPY src/ src/
# Os testes rodam no CI (que tem Docker para o Testcontainers), não aqui.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q package -DskipTests \
    && cp target/*.jar app.jar

# ---------- Estágio 2: runtime ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Roda como usuário sem privilégios: se a aplicação for comprometida, o invasor não é root.
RUN useradd --system --uid 1001 rotadev
USER rotadev

COPY --from=build /app/app.jar app.jar

# A JVM lê o limite de memória do container. MaxRAMPercentage=60 deixa ~40% para
# o que não é heap (metaspace, threads, buffers). Ver ADR 0013.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
