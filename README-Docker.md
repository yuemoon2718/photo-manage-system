# 图片管理系统 Docker 部署指南

## 🐳 Docker Compose 部署

### 快速启动

1. **克隆项目并进入目录**
   ```bash
   cd /path/to/your/project
   ```

2. **启动所有服务**
   ```bash
   docker-compose up -d
   ```

3. **查看服务状态**
   ```bash
   docker-compose ps
   ```

4. **访问应用**
   - 主应用: http://localhost:8080
   - 通过Nginx: http://localhost:80
   - 数据库: localhost:3306

### 开发环境

使用开发环境配置启动：
```bash
docker-compose -f docker-compose.dev.yml up -d
```

开发环境特点：
- 支持热重载
- 调试端口开放 (5005)
- 数据库端口为 3307
- 源代码挂载

### 服务说明

#### 🗄️ MySQL 数据库
- **容器名**: image_management_mysql
- **端口**: 3306
- **数据库**: image_management_system
- **用户名**: appuser
- **密码**: apppassword123
- **Root密码**: rootpassword123

#### 🚀 应用服务
- **容器名**: image_management_app
- **端口**: 8080
- **环境**: Docker
- **健康检查**: /actuator/health

#### 🔄 Redis 缓存
- **容器名**: image_management_redis
- **端口**: 6379
- **用途**: 会话缓存、数据缓存

#### 🌐 Nginx 反向代理
- **容器名**: image_management_nginx
- **端口**: 80, 443
- **功能**: 静态文件服务、负载均衡

### 常用命令

#### 启动服务
```bash
# 后台启动
docker-compose up -d

# 查看日志
docker-compose logs -f app

# 重启服务
docker-compose restart app
```

#### 数据库操作
```bash
# 连接数据库
docker-compose exec mysql mysql -u appuser -p image_management_system

# 备份数据库
docker-compose exec mysql mysqldump -u appuser -p image_management_system > backup.sql

# 恢复数据库
docker-compose exec -T mysql mysql -u appuser -p image_management_system < backup.sql
```

#### 应用管理
```bash
# 查看应用日志
docker-compose logs -f app

# 进入应用容器
docker-compose exec app bash

# 重新构建应用
docker-compose build app
docker-compose up -d app
```

#### 文件上传
```bash
# 查看上传文件
docker-compose exec app ls -la /app/uploads

# 清理上传文件
docker-compose exec app rm -rf /app/uploads/*
```

### 环境变量配置

#### 数据库配置
```yaml
SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/image_management_system
SPRING_DATASOURCE_USERNAME: appuser
SPRING_DATASOURCE_PASSWORD: apppassword123
```

#### 文件上传配置
```yaml
UPLOAD_PATH: /app/uploads
MAX_FILE_SIZE: 10MB
MAX_REQUEST_SIZE: 10MB
```

### 数据持久化

所有重要数据都通过Docker卷持久化：
- `mysql_data`: 数据库数据
- `redis_data`: Redis缓存数据
- `upload_data`: 上传文件

### 故障排除

#### 1. 应用启动失败
```bash
# 查看详细日志
docker-compose logs app

# 检查数据库连接
docker-compose exec app curl http://localhost:8080/actuator/health
```

#### 2. 数据库连接问题
```bash
# 检查数据库状态
docker-compose exec mysql mysqladmin -u root -p status

# 重启数据库
docker-compose restart mysql
```

#### 3. 文件上传问题
```bash
# 检查上传目录权限
docker-compose exec app ls -la /app/uploads

# 重新创建上传目录
docker-compose exec app mkdir -p /app/uploads
```

### 生产环境部署

1. **修改密码**
   ```bash
   # 修改 docker-compose.yml 中的密码
   MYSQL_ROOT_PASSWORD: your_secure_password
   MYSQL_PASSWORD: your_secure_password
   ```

2. **配置HTTPS**
   - 将SSL证书挂载到Nginx容器
   - 修改nginx.conf启用HTTPS

3. **资源限制**
   ```yaml
   deploy:
     resources:
       limits:
         memory: 512M
         cpus: '0.5'
   ```

### 监控和维护

#### 健康检查
```bash
# 检查所有服务健康状态
docker-compose ps

# 应用健康检查
curl http://localhost:8080/actuator/health
```

#### 日志管理
```bash
# 查看所有服务日志
docker-compose logs

# 查看特定服务日志
docker-compose logs -f app mysql
```

#### 清理资源
```bash
# 停止并删除容器
docker-compose down

# 删除所有数据卷（谨慎使用）
docker-compose down -v

# 清理未使用的镜像
docker system prune -a
```
