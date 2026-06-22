# MediCare 推送规范

## 推送范围

只推送项目开发和说明所需文件：

1. 后端开发文件：`medicare-server/`、`medicare-server-archetype/`
2. 前端开发文件：`medicare-web/`
3. 数据库脚本：`sql/`
4. 测试和辅助脚本：`scripts/`
5. 项目根目录下的 Markdown 说明文件，例如 `plan.md`、`step.md`、`push-requirements.md`
6. 必要的配置文件，例如 `.gitignore`

## 不推送内容

以下内容不要提交到 Git 仓库：

1. `.vscode/`
2. `.idea/`
3. `DOC/`
4. `node_modules/`
5. `dist/`
6. `target/`、`build/`、`out/`
7. `logs/`
8. `.DS_Store`
9. 本地环境变量文件，例如 `.env.local`、`.env.*.local`
10. 临时文件、备份文件、压缩包、个人 IDE 配置

## 推送前检查

每次提交前先执行：

```bash
git status --short
```

确认暂存区中不包含 `.vscode/`、`DOC/`、编译产物、日志和本地配置。

推荐提交流程：

```bash
git add -A
git status --short
git commit -m "提交说明"
git push
```

如果发现误加入不应推送的文件，先取消跟踪：

```bash
git rm -r --cached .vscode DOC
git add .gitignore push-requirements.md
git commit -m "chore: update push requirements"
git push
```
