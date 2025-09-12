# Sử dụng image chính thức của OpenJDK 17
FROM openjdk:17-jdk-alpine

# Đặt biến môi trường cho tên file jar
ENV JAR_FILE=target/asset-0.0.1-SNAPSHOT.jar

# Tạo thư mục làm việc
WORKDIR /app

# Copy file jar từ target vào container
COPY ${JAR_FILE} app.jar

# Copy file cấu hình nếu cần (ví dụ application.yml)
COPY src/main/resources/application.yml ./src/main/resources/application.yml

# Mở port ứng dụng (ví dụ 8080)
EXPOSE 8080

# Lệnh chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]
