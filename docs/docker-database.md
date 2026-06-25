# Docker 数据库管理

项目数据库已统一交给 Docker Compose 管理：

```bash
docker compose up -d mysql adminer qdrant
```

- MySQL: `127.0.0.1:${MYSQL_HOST_PORT:-3306}`
- 数据库名: `${MYSQL_DATABASE:-medicare}`
- 用户名: `root`
- 密码: `${MYSQL_ROOT_PASSWORD:-a123456.}`
- 管理界面: `http://127.0.0.1:${ADMINER_HOST_PORT:-8081}`
- Qdrant HTTP: `http://127.0.0.1:${QDRANT_HTTP_PORT:-6333}`
- Qdrant gRPC: `127.0.0.1:${QDRANT_GRPC_PORT:-6334}`

首次启动空数据卷时，MySQL 容器会按顺序执行 `sql/` 目录中的初始化脚本，包括 `medicare.sql` 和 `migration_v3.sql`。

如果本机已有 MySQL 占用 `3306`，可以改用：

```bash
MYSQL_HOST_PORT=3307 DB_PORT=3307 docker compose up -d mysql adminer qdrant
```

然后后端使用相同的 `DB_PORT` 启动即可。

> 注意：向量数据库 Qdrant 默认监听 `localhost:6334`。若后端也使用 Docker Compose 启动，请将 `AI_VECTOR_HOST` 设置为服务名 `qdrant`。
