# 数据库脚本

服务端使用的两个数据库的建表脚本，原为 Navicat 导出的 MySQL DDL（仅表结构，无数据）。

## 目录

| 目录 | 说明 |
|---|---|
| `mysql/` | 原样复制的 MySQL 版本（`app_data.sql`、`app_log.sql`） |
| `pgsql/` | 由 MySQL 结构自动转换的 PostgreSQL 版本 |

## 库说明

| 文件 | Schema | 表数 | 用途 |
|---|---|---|---|
| `app_data.sql` | `app_data` | 16 | 主业务库（用户、玩家、商城、代理、CDK 等） |
| `app_log.sql` | `app_log` | 26 | 日志库（登录、游戏记录、充值、佣金等流水） |

> 后台管理库 `ttmy_admin.sql`（约 116MB）按「后台不分析」的约定未纳入。

## 关于 `tbl_fruit_laba_reward_info` 表

水果拉霸奖励表的名字在旧工程里存在三处不一致，本次已统一：

| 来源 | 表名 |
|---|---|
| Navicat 导出（原始 dump） | `fruitlabarewardinfo`（全小写） |
| 旧代码 `FruitLaBaRewardInfoMapper.TABLE_NAME` | `fruitLaBaRewardInfo`（驼峰） |
| 本次统一后 | `tbl_fruit_laba_reward_info` |

- 该表实际上由 `OseeInit` 启动时调用 `createTable()` 动态创建，真实名字是驼峰 `fruitLaBaRewardInfo`；
- Windows 的 MySQL 默认表名不区分大小写，掩盖了「全小写 ≠ 驼峰」这个隐患，部署到 Linux 会踩坑；
- 本次统一为 `tbl_fruit_laba_reward_info`，补齐 `tbl_` 前缀（其余 15 张表均有前缀），与全库规范一致。

> 迁移 mapper 时，需把 `FruitLaBaRewardInfoMapper.TABLE_NAME` 一并改为 `tbl_fruit_laba_reward_info`。

## MySQL → PostgreSQL 转换规则

| MySQL | PostgreSQL |
|---|---|
| 反引号 `` ` `` 标识符 | 双引号 `"` 标识符 |
| `AUTO_INCREMENT` | `serial` / `bigserial` |
| `int(n)` / `bigint(n)` / `tinyint(n)` | `integer` / `bigint` / `smallint` |
| `datetime` | `timestamp` |
| `decimal` | `numeric` |
| `COMMENT '...'`（列内联） | `COMMENT ON COLUMN ... IS '...'`（独立语句） |
| `ENGINE = ... CHARACTER SET = ... COLLATE = ...` | 移除 |
| `PRIMARY KEY (...) USING BTREE` | `PRIMARY KEY (...)` |
| `UNIQUE INDEX name(col) USING BTREE` | `CONSTRAINT name UNIQUE (col)` |

## 使用

```bash
# PostgreSQL
psql -U <user> -d app_data -f pgsql/app_data.sql
psql -U <user> -d app_log  -f pgsql/app_log.sql
```
