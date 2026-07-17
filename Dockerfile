# 运行阶段
FROM openjdk:17-jdk-slim
WORKDIR /app

# 安装必要的工具
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 复制本地已构建的jar文件
COPY target/*.jar app.jar

# 创建上传和缩略图目录
RUN mkdir -p /app/uploads /app/thumbnails /app/logs

# 暴露端口
EXPOSE 8080

# 设置环境变量
ENV SPRING_PROFILES_ACTIVE=docker

# 启动应用
CMD ["java", "-jar", "app.jar"]
