FROM openjdk:17-jdk-slim AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN ./mvnw clean package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /app
# Install Tesseract OCR, English language data, and Leptonica
RUN apt-get update && apt-get install -y tesseract-ocr tesseract-ocr-eng libleptonica-dev && rm -rf /var/lib/apt/lists/*
# Set Tesseract environment variables
ENV TESSDATA_PREFIX=/usr/share/tessdata
# Increase Java heap space for memory-intensive OCR operations
ENV JAVA_OPTS="-Xmx384m"
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT java ${JAVA_OPTS} -jar app.jar
