FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY src/ src/
COPY scripts/ scripts/
RUN bash scripts/build.sh
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/iam-review.jar target/iam-review.jar
COPY web-demo/ web-demo/
COPY sample-data/users.csv sample-data/users.csv
ENV PORT=8080
EXPOSE 8080
CMD ["java", "-cp", "target/iam-review.jar", "com.iamreview.WebServer"]
