-- ============================================================
-- PostgreSQL 版（由 MySQL 结构自动转换）
-- Schema: app_log
-- 转换规则：
--   * 反引号 `  -> 双引号 "
--   * AUTO_INCREMENT -> serial / bigserial
--   * int(n)/bigint(n)/tinyint(n) -> integer/bigint/smallint
--   * datetime -> timestamp
--   * decimal -> numeric
--   * COMMENT '...' -> COMMENT ON COLUMN ... IS '...'
--   * ENGINE / CHARACTER SET / COLLATE / USING BTREE 已移除
-- ============================================================

DROP TABLE IF EXISTS "tbl_agent_cut_log";
CREATE TABLE "tbl_agent_cut_log"  (
  "id" serial,
  "player_id" bigint NOT NULL,
  "agent_id" bigint NOT NULL,
  "game" integer NOT NULL,
  "cut_money" bigint NULL DEFAULT NULL,
  "cut_dragon_crystal" bigint NULL DEFAULT NULL,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_agent_cut_log"."id" IS 'ID';
COMMENT ON COLUMN "tbl_agent_cut_log"."player_id" IS '玩家ID';
COMMENT ON COLUMN "tbl_agent_cut_log"."agent_id" IS '代理ID';
COMMENT ON COLUMN "tbl_agent_cut_log"."game" IS '游戏';
COMMENT ON COLUMN "tbl_agent_cut_log"."cut_money" IS '金币';
COMMENT ON COLUMN "tbl_agent_cut_log"."cut_dragon_crystal" IS '龙晶';
COMMENT ON COLUMN "tbl_agent_cut_log"."create_time" IS '数据创建时间';
DROP TABLE IF EXISTS "tbl_agent_cut_receive_log";
CREATE TABLE "tbl_agent_cut_receive_log"  (
  "id" serial,
  "agent_id" bigint NOT NULL,
  "agent_name" varchar(100) NOT NULL,
  "money" bigint NULL DEFAULT NULL,
  "dragon_crystal" bigint NULL DEFAULT NULL,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_agent_cut_receive_log"."id" IS 'ID';
COMMENT ON COLUMN "tbl_agent_cut_receive_log"."agent_id" IS '代理玩家ID';
COMMENT ON COLUMN "tbl_agent_cut_receive_log"."agent_name" IS '代理玩家昵称';
COMMENT ON COLUMN "tbl_agent_cut_receive_log"."money" IS '金币数量';
COMMENT ON COLUMN "tbl_agent_cut_receive_log"."dragon_crystal" IS '花费的佣金';
COMMENT ON COLUMN "tbl_agent_cut_receive_log"."create_time" IS '数据创建时间';
DROP TABLE IF EXISTS "tbl_app_game_log";
CREATE TABLE "tbl_app_game_log"  (
  "id" serial,
  "number" integer NOT NULL,
  "income" integer NOT NULL,
  "reward_id" integer NOT NULL,
  "stock" bigint NOT NULL,
  "mode" integer NOT NULL,
  "type" integer NOT NULL,
  "create_time" timestamp NOT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_app_game_log"."id" IS '捕鱼竞技模式游戏记录id';
COMMENT ON COLUMN "tbl_app_game_log"."number" IS '参赛人数';
COMMENT ON COLUMN "tbl_app_game_log"."income" IS '报名收入';
COMMENT ON COLUMN "tbl_app_game_log"."reward_id" IS '奖励支出 id';
COMMENT ON COLUMN "tbl_app_game_log"."stock" IS '库存';
COMMENT ON COLUMN "tbl_app_game_log"."mode" IS '游戏模式';
COMMENT ON COLUMN "tbl_app_game_log"."type" IS '游戏类型';
COMMENT ON COLUMN "tbl_app_game_log"."create_time" IS '创建时间';
DROP TABLE IF EXISTS "tbl_app_login_log";
CREATE TABLE "tbl_app_login_log"  (
  "id" bigserial,
  "user_id" bigint NULL DEFAULT NULL,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "exit_time" timestamp NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_app_login_log"."id" IS '登录id';
COMMENT ON COLUMN "tbl_app_login_log"."user_id" IS '用户id';
COMMENT ON COLUMN "tbl_app_login_log"."create_time" IS '登录时间';
COMMENT ON COLUMN "tbl_app_login_log"."exit_time" IS '退出时间';
DROP TABLE IF EXISTS "tbl_app_rank_log";
CREATE TABLE "tbl_app_rank_log"  (
  "id" serial,
  "player_id" bigint NOT NULL,
  "nickname" varchar(255) NOT NULL,
  "score" integer NOT NULL,
  "rank" integer NOT NULL,
  "mode" integer NOT NULL,
  "cost" integer NOT NULL,
  "games" integer NOT NULL,
  "change" integer NOT NULL,
  "reward_id" integer NULL DEFAULT NULL,
  "receive" integer NOT NULL,
  "receive_time" timestamp NULL DEFAULT NULL,
  "create_time" timestamp NOT NULL,
  "email_id" bigint NOT NULL,
  "type" integer NOT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_app_rank_log"."id" IS '竞技模式玩家比赛排名表';
COMMENT ON COLUMN "tbl_app_rank_log"."player_id" IS '玩家id';
COMMENT ON COLUMN "tbl_app_rank_log"."nickname" IS '玩家昵称';
COMMENT ON COLUMN "tbl_app_rank_log"."score" IS '玩家积分';
COMMENT ON COLUMN "tbl_app_rank_log"."rank" IS '玩家排名';
COMMENT ON COLUMN "tbl_app_rank_log"."mode" IS '竞技模式类型（1：大奖赛、2：全民赛、3：满人赛、4：道具赛）';
COMMENT ON COLUMN "tbl_app_rank_log"."cost" IS '报名消耗钻石数';
COMMENT ON COLUMN "tbl_app_rank_log"."games" IS '游戏参与局数';
COMMENT ON COLUMN "tbl_app_rank_log"."change" IS '玩家金币变化';
COMMENT ON COLUMN "tbl_app_rank_log"."reward_id" IS '奖励表id';
COMMENT ON COLUMN "tbl_app_rank_log"."receive" IS '玩家奖励是否领取';
COMMENT ON COLUMN "tbl_app_rank_log"."receive_time" IS '领取时间';
COMMENT ON COLUMN "tbl_app_rank_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_app_rank_log"."email_id" IS '邮件id';
COMMENT ON COLUMN "tbl_app_rank_log"."type" IS '日赛：1 周赛：2';
DROP TABLE IF EXISTS "tbl_app_rank_reward_log";
CREATE TABLE "tbl_app_rank_reward_log"  (
  "id" serial,
  "name" varchar(255) NULL DEFAULT NULL,
  "rank" integer NOT NULL,
  "type" integer NOT NULL,
  "status" integer NOT NULL,
  "reward_id" integer NOT NULL,
  "update_time" timestamp NOT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_app_rank_reward_log"."name" IS '名称';
COMMENT ON COLUMN "tbl_app_rank_reward_log"."rank" IS '小于等于该排名';
COMMENT ON COLUMN "tbl_app_rank_reward_log"."type" IS '类型，日排名1 周排名2';
COMMENT ON COLUMN "tbl_app_rank_reward_log"."status" IS '状态';
COMMENT ON COLUMN "tbl_app_rank_reward_log"."reward_id" IS '奖励表id';
COMMENT ON COLUMN "tbl_app_rank_reward_log"."update_time" IS '最后一次更新时间';
DROP TABLE IF EXISTS "tbl_app_reward_log";
CREATE TABLE "tbl_app_reward_log"  (
  "id" serial,
  "player_id" bigint NOT NULL,
  "gold" integer NOT NULL,
  "diamond" integer NOT NULL,
  "lower_ball" integer NOT NULL,
  "middle_ball" integer NOT NULL,
  "high_ball" integer NOT NULL,
  "skill_lock" integer NOT NULL,
  "skill_fast" integer NOT NULL,
  "skill_crit" integer NOT NULL,
  "skill_frozen" integer NOT NULL,
  "boss_bugle" integer NOT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_app_reward_log"."id" IS '玩家奖励表';
COMMENT ON COLUMN "tbl_app_reward_log"."player_id" IS '玩家id';
COMMENT ON COLUMN "tbl_app_reward_log"."gold" IS '金币';
COMMENT ON COLUMN "tbl_app_reward_log"."diamond" IS '钻石';
COMMENT ON COLUMN "tbl_app_reward_log"."lower_ball" IS '低阶龙珠';
COMMENT ON COLUMN "tbl_app_reward_log"."middle_ball" IS '中阶龙珠';
COMMENT ON COLUMN "tbl_app_reward_log"."high_ball" IS '高阶龙珠';
COMMENT ON COLUMN "tbl_app_reward_log"."skill_lock" IS '锁定技能';
COMMENT ON COLUMN "tbl_app_reward_log"."skill_fast" IS '极速技能';
COMMENT ON COLUMN "tbl_app_reward_log"."skill_crit" IS '暴击技能';
COMMENT ON COLUMN "tbl_app_reward_log"."skill_frozen" IS '冰冻技能';
COMMENT ON COLUMN "tbl_app_reward_log"."boss_bugle" IS 'BOSS号角';
DROP TABLE IF EXISTS "tbl_dragon_crystal_exchange_log";
CREATE TABLE "tbl_dragon_crystal_exchange_log"  (
  "id" serial,
  "player_id" bigint NOT NULL,
  "exchange_type" integer NOT NULL,
  "bronze_torpedo_before" bigint NULL DEFAULT NULL,
  "silver_torpedo_before" bigint NULL DEFAULT NULL,
  "gold_torpedo_before" bigint NULL DEFAULT NULL,
  "bronze_torpedo_change" bigint NULL DEFAULT NULL,
  "silver_torpedo_change" bigint NULL DEFAULT NULL,
  "gold_torpedo_change" bigint NULL DEFAULT NULL,
  "dragon_crystal_before" bigint NULL DEFAULT NULL,
  "dragon_crystal_change" bigint NULL DEFAULT NULL,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_dragon_crystal_exchange_log"."id" IS 'ID';
COMMENT ON COLUMN "tbl_dragon_crystal_exchange_log"."player_id" IS '玩家ID';
COMMENT ON COLUMN "tbl_dragon_crystal_exchange_log"."exchange_type" IS '兑换类型 0-兑换龙晶 1-兑换鱼雷';
COMMENT ON COLUMN "tbl_dragon_crystal_exchange_log"."bronze_torpedo_before" IS '青铜鱼雷数量前';
COMMENT ON COLUMN "tbl_dragon_crystal_exchange_log"."silver_torpedo_before" IS '白银鱼雷数量前';
COMMENT ON COLUMN "tbl_dragon_crystal_exchange_log"."gold_torpedo_before" IS '黄金鱼雷数量前';
COMMENT ON COLUMN "tbl_dragon_crystal_exchange_log"."bronze_torpedo_change" IS '青铜鱼雷数量变';
COMMENT ON COLUMN "tbl_dragon_crystal_exchange_log"."silver_torpedo_change" IS '白银鱼雷数量变';
COMMENT ON COLUMN "tbl_dragon_crystal_exchange_log"."gold_torpedo_change" IS '黄金鱼雷数量变';
COMMENT ON COLUMN "tbl_dragon_crystal_exchange_log"."dragon_crystal_before" IS '龙晶数量前';
COMMENT ON COLUMN "tbl_dragon_crystal_exchange_log"."dragon_crystal_change" IS '龙晶数量变';
COMMENT ON COLUMN "tbl_dragon_crystal_exchange_log"."create_time" IS '数据创建时间';
DROP TABLE IF EXISTS "tbl_osee_cut_money_log";
CREATE TABLE "tbl_osee_cut_money_log"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "user_id" bigint NULL DEFAULT NULL,
  "game" integer NOT NULL,
  "cut_money" bigint NULL DEFAULT NULL,
  "type" integer NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_osee_cut_money_log"."id" IS '记录id';
COMMENT ON COLUMN "tbl_osee_cut_money_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_osee_cut_money_log"."user_id" IS '玩家id';
COMMENT ON COLUMN "tbl_osee_cut_money_log"."game" IS '游戏';
COMMENT ON COLUMN "tbl_osee_cut_money_log"."cut_money" IS '变动金币';
DROP TABLE IF EXISTS "tbl_osee_expend_log";
CREATE TABLE "tbl_osee_expend_log"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "user_id" bigint NULL DEFAULT NULL,
  "nickname" varchar(32) NULL DEFAULT NULL,
  "pay_type" integer NOT NULL,
  "diamond" bigint NULL DEFAULT NULL,
  "money" bigint NULL DEFAULT NULL,
  "lottery" bigint NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_osee_expend_log"."id" IS '记录id';
COMMENT ON COLUMN "tbl_osee_expend_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_osee_expend_log"."user_id" IS '玩家id';
COMMENT ON COLUMN "tbl_osee_expend_log"."nickname" IS '昵称';
COMMENT ON COLUMN "tbl_osee_expend_log"."pay_type" IS '支出类型';
COMMENT ON COLUMN "tbl_osee_expend_log"."diamond" IS '支出钻石';
COMMENT ON COLUMN "tbl_osee_expend_log"."money" IS '支出金币';
COMMENT ON COLUMN "tbl_osee_expend_log"."lottery" IS '支出奖券';
DROP TABLE IF EXISTS "tbl_osee_fightten_record_log";
CREATE TABLE "tbl_osee_fightten_record_log"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "money" bigint NULL DEFAULT NULL,
  "playerId" bigint NULL DEFAULT NULL,
  "nickname" varchar(32) NOT NULL,
  "playBeforeMoney" bigint NULL DEFAULT NULL,
  "playAfterMoney" bigint NULL DEFAULT NULL,
  "input" bigint NOT NULL,
  "rate" integer NULL DEFAULT NULL,
  "cardType" varchar(32) NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_osee_fightten_record_log"."id" IS '记录id';
COMMENT ON COLUMN "tbl_osee_fightten_record_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_osee_fightten_record_log"."money" IS '账户金币变动数额';
COMMENT ON COLUMN "tbl_osee_fightten_record_log"."playerId" IS '玩家id';
COMMENT ON COLUMN "tbl_osee_fightten_record_log"."nickname" IS '玩家昵称';
COMMENT ON COLUMN "tbl_osee_fightten_record_log"."playBeforeMoney" IS '游戏前剩余金币';
COMMENT ON COLUMN "tbl_osee_fightten_record_log"."playAfterMoney" IS '游戏后剩余金币';
COMMENT ON COLUMN "tbl_osee_fightten_record_log"."input" IS '下注金额';
COMMENT ON COLUMN "tbl_osee_fightten_record_log"."rate" IS '倍率';
COMMENT ON COLUMN "tbl_osee_fightten_record_log"."cardType" IS '牌型';
DROP TABLE IF EXISTS "tbl_osee_fishing_record_log";
CREATE TABLE "tbl_osee_fishing_record_log"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "player_id" bigint NULL DEFAULT NULL,
  "room_index" integer NULL DEFAULT NULL,
  "spend_money" bigint NULL DEFAULT NULL,
  "win_money" bigint NULL DEFAULT NULL,
  "drop_bronze_torpedo_num" bigint NULL DEFAULT NULL,
  "drop_silver_torpedo_num" bigint NULL DEFAULT NULL,
  "drop_gold_torpedo_num" bigint NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_osee_fishing_record_log"."id" IS '记录id';
COMMENT ON COLUMN "tbl_osee_fishing_record_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_osee_fishing_record_log"."player_id" IS '玩家id';
COMMENT ON COLUMN "tbl_osee_fishing_record_log"."room_index" IS '场次序号';
COMMENT ON COLUMN "tbl_osee_fishing_record_log"."spend_money" IS '花费金币';
COMMENT ON COLUMN "tbl_osee_fishing_record_log"."win_money" IS '赢取金币';
COMMENT ON COLUMN "tbl_osee_fishing_record_log"."drop_bronze_torpedo_num" IS '掉落的青铜鱼雷数量';
COMMENT ON COLUMN "tbl_osee_fishing_record_log"."drop_silver_torpedo_num" IS '掉落的白银鱼雷数量';
COMMENT ON COLUMN "tbl_osee_fishing_record_log"."drop_gold_torpedo_num" IS '掉落的黄金鱼雷数量';
DROP TABLE IF EXISTS "tbl_osee_fruit_record_log";
CREATE TABLE "tbl_osee_fruit_record_log"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "money" bigint NULL DEFAULT NULL,
  "playerId" bigint NULL DEFAULT NULL,
  "nickname" varchar(32) NOT NULL,
  "playBeforeMoney" bigint NULL DEFAULT NULL,
  "playAfterMoney" bigint NULL DEFAULT NULL,
  "cost" bigint NULL DEFAULT NULL,
  "lineNum" integer NULL DEFAULT NULL,
  "totalWin" bigint NULL DEFAULT NULL,
  "info" varchar(255) NULL DEFAULT NULL,
  "type" integer NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_osee_fruit_record_log"."id" IS '记录id';
COMMENT ON COLUMN "tbl_osee_fruit_record_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_osee_fruit_record_log"."money" IS '账户金币变动数额';
COMMENT ON COLUMN "tbl_osee_fruit_record_log"."playerId" IS '玩家id';
COMMENT ON COLUMN "tbl_osee_fruit_record_log"."nickname" IS '玩家昵称';
COMMENT ON COLUMN "tbl_osee_fruit_record_log"."playBeforeMoney" IS '游戏前剩余金币';
COMMENT ON COLUMN "tbl_osee_fruit_record_log"."playAfterMoney" IS '游戏后剩余金币';
COMMENT ON COLUMN "tbl_osee_fruit_record_log"."cost" IS '下注消耗';
COMMENT ON COLUMN "tbl_osee_fruit_record_log"."lineNum" IS '下注条数';
COMMENT ON COLUMN "tbl_osee_fruit_record_log"."totalWin" IS '中奖赢取';
COMMENT ON COLUMN "tbl_osee_fruit_record_log"."info" IS '中奖详情';
COMMENT ON COLUMN "tbl_osee_fruit_record_log"."type" IS '类型';
DROP TABLE IF EXISTS "tbl_osee_gobang_record_log";
CREATE TABLE "tbl_osee_gobang_record_log"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "money" bigint NULL DEFAULT NULL,
  "winner_id" bigint NULL DEFAULT NULL,
  "winner_nickname" varchar(32) NOT NULL,
  "winner_before_money" bigint NULL DEFAULT NULL,
  "winner_after_money" bigint NULL DEFAULT NULL,
  "loser_id" bigint NULL DEFAULT NULL,
  "loser_nickname" varchar(32) NOT NULL,
  "loser_before_money" bigint NULL DEFAULT NULL,
  "loser_after_money" bigint NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_osee_gobang_record_log"."id" IS '记录id';
COMMENT ON COLUMN "tbl_osee_gobang_record_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_osee_gobang_record_log"."money" IS '学费';
COMMENT ON COLUMN "tbl_osee_gobang_record_log"."winner_id" IS '获胜者id';
COMMENT ON COLUMN "tbl_osee_gobang_record_log"."winner_nickname" IS '获胜者昵称';
COMMENT ON COLUMN "tbl_osee_gobang_record_log"."winner_before_money" IS '获胜前金币';
COMMENT ON COLUMN "tbl_osee_gobang_record_log"."winner_after_money" IS '获胜后金币';
COMMENT ON COLUMN "tbl_osee_gobang_record_log"."loser_id" IS '失败者id';
COMMENT ON COLUMN "tbl_osee_gobang_record_log"."loser_nickname" IS '失败者昵称';
COMMENT ON COLUMN "tbl_osee_gobang_record_log"."loser_before_money" IS '失败前金币';
COMMENT ON COLUMN "tbl_osee_gobang_record_log"."loser_after_money" IS '失败后金币';
DROP TABLE IF EXISTS "tbl_osee_lottery_draw_log";
CREATE TABLE "tbl_osee_lottery_draw_log"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "player_id" bigint NOT NULL,
  "item_id" integer NOT NULL,
  "item_num" bigint NOT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_osee_lottery_draw_log"."id" IS '记录id';
COMMENT ON COLUMN "tbl_osee_lottery_draw_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_osee_lottery_draw_log"."player_id" IS '玩家id';
COMMENT ON COLUMN "tbl_osee_lottery_draw_log"."item_id" IS '物品id';
COMMENT ON COLUMN "tbl_osee_lottery_draw_log"."item_num" IS '物品数量';
DROP TABLE IF EXISTS "tbl_osee_player_tenure_log";
CREATE TABLE "tbl_osee_player_tenure_log"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "user_id" bigint NULL DEFAULT NULL,
  "nickname" varchar(32) NULL DEFAULT NULL,
  "reason" integer NOT NULL,
  "pre_diamond" bigint NULL DEFAULT NULL,
  "change_diamond" bigint NULL DEFAULT NULL,
  "pre_money" bigint NULL DEFAULT NULL,
  "change_money" bigint NULL DEFAULT NULL,
  "pre_lottery" bigint NULL DEFAULT NULL,
  "change_lottery" bigint NULL DEFAULT NULL,
  "pre_bank_money" bigint NULL DEFAULT NULL,
  "change_bank_money" bigint NULL DEFAULT NULL,
  "pre_bronze_torpedo" bigint NULL DEFAULT NULL,
  "change_bronze_torpedo" bigint NULL DEFAULT NULL,
  "pre_silver_torpedo" bigint NULL DEFAULT NULL,
  "change_silver_torpedo" bigint NULL DEFAULT NULL,
  "pre_gold_torpedo" bigint NULL DEFAULT NULL,
  "change_gold_torpedo" bigint NULL DEFAULT NULL,
  "pre_skill_lock" bigint NULL DEFAULT NULL,
  "change_skill_lock" bigint NULL DEFAULT NULL,
  "pre_skill_frozen" bigint NULL DEFAULT NULL,
  "change_skill_frozen" bigint NULL DEFAULT NULL,
  "pre_skill_fast" bigint NULL DEFAULT NULL,
  "change_skill_fast" bigint NULL DEFAULT NULL,
  "pre_skill_crit" bigint NULL DEFAULT NULL,
  "change_skill_crit" bigint NULL DEFAULT NULL,
  "pre_boss_bugle" bigint NULL DEFAULT NULL,
  "change_boss_bugle" bigint NULL DEFAULT NULL,
  "pre_dragon_crystal" bigint NULL DEFAULT NULL,
  "change_dragon_crystal" bigint NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."id" IS '记录id';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."user_id" IS '玩家id';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."nickname" IS '昵称';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."reason" IS '变动来源';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."pre_diamond" IS '变动前钻石';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."change_diamond" IS '变动钻石';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."pre_money" IS '变动前金币';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."change_money" IS '变动金币';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."pre_lottery" IS '变动前奖券';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."change_lottery" IS '变动奖券';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."pre_bank_money" IS '变动前保险箱金币';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."change_bank_money" IS '变动保险箱金币';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."pre_bronze_torpedo" IS '变动前青铜鱼雷';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."change_bronze_torpedo" IS '变动的青铜鱼雷数量';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."pre_silver_torpedo" IS '变动前白银鱼雷';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."change_silver_torpedo" IS '变动的白银鱼雷数量';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."pre_gold_torpedo" IS '变动前黄金鱼雷';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."change_gold_torpedo" IS '变动的黄金鱼雷数量';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."pre_skill_lock" IS '变动前锁定技能数量';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."change_skill_lock" IS '变动的锁定技能数量';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."pre_skill_frozen" IS '变动前冰冻技能数量';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."change_skill_frozen" IS '变动的冰冻技能数量';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."pre_skill_fast" IS '变动前急速技能数量';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."change_skill_fast" IS '变动的急速技能数量';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."pre_skill_crit" IS '变动前暴击技能数量';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."change_skill_crit" IS '变动的暴击技能数量';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."pre_boss_bugle" IS '变动前boss号角数量';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."change_boss_bugle" IS '变动的boss号角数量';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."pre_dragon_crystal" IS '变动前龙晶数量';
COMMENT ON COLUMN "tbl_osee_player_tenure_log"."change_dragon_crystal" IS '变动龙晶';
DROP TABLE IF EXISTS "tbl_osee_real_lottery_log";
CREATE TABLE "tbl_osee_real_lottery_log"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "order_num" varchar(32) NULL DEFAULT NULL,
  "user_id" bigint NOT NULL,
  "nickname" varchar(32) NULL DEFAULT NULL,
  "reward_name" varchar(32) NULL DEFAULT NULL,
  "count" integer NOT NULL,
  "cost" integer NOT NULL,
  "creator" varchar(32) NULL DEFAULT NULL,
  "consignee" varchar(32) NULL DEFAULT NULL,
  "phone_num" varchar(32) NULL DEFAULT NULL,
  "address" varchar(32) NULL DEFAULT NULL,
  "order_state" integer NOT NULL,
  "stock_id" bigint NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_osee_real_lottery_log"."id" IS '记录id';
COMMENT ON COLUMN "tbl_osee_real_lottery_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_osee_real_lottery_log"."order_num" IS '订单号';
COMMENT ON COLUMN "tbl_osee_real_lottery_log"."user_id" IS '兑换人id';
COMMENT ON COLUMN "tbl_osee_real_lottery_log"."nickname" IS '昵称';
COMMENT ON COLUMN "tbl_osee_real_lottery_log"."reward_name" IS '商品名';
COMMENT ON COLUMN "tbl_osee_real_lottery_log"."count" IS '兑换数量';
COMMENT ON COLUMN "tbl_osee_real_lottery_log"."cost" IS '消耗数量';
COMMENT ON COLUMN "tbl_osee_real_lottery_log"."creator" IS '创建人';
COMMENT ON COLUMN "tbl_osee_real_lottery_log"."consignee" IS '收货人';
COMMENT ON COLUMN "tbl_osee_real_lottery_log"."phone_num" IS '手机号';
COMMENT ON COLUMN "tbl_osee_real_lottery_log"."address" IS '收货地址';
COMMENT ON COLUMN "tbl_osee_real_lottery_log"."order_state" IS '状态';
COMMENT ON COLUMN "tbl_osee_real_lottery_log"."stock_id" IS '对应兑换的库存物品ID';
DROP TABLE IF EXISTS "tbl_osee_recharge_log";
CREATE TABLE "tbl_osee_recharge_log"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "order_num" varchar(32) NULL DEFAULT NULL,
  "user_id" bigint NULL DEFAULT NULL,
  "nickname" varchar(32) NULL DEFAULT NULL,
  "pay_money" integer NOT NULL,
  "shop_name" varchar(32) NULL DEFAULT NULL,
  "shop_type" integer NOT NULL,
  "count" integer NOT NULL,
  "creator" varchar(32) NULL DEFAULT NULL,
  "recharge_type" integer NOT NULL,
  "order_state" integer NOT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_osee_recharge_log"."id" IS '记录id';
COMMENT ON COLUMN "tbl_osee_recharge_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_osee_recharge_log"."order_num" IS '订单号';
COMMENT ON COLUMN "tbl_osee_recharge_log"."user_id" IS '玩家id';
COMMENT ON COLUMN "tbl_osee_recharge_log"."nickname" IS '昵称';
COMMENT ON COLUMN "tbl_osee_recharge_log"."pay_money" IS '支付金额';
COMMENT ON COLUMN "tbl_osee_recharge_log"."shop_name" IS '商品名';
COMMENT ON COLUMN "tbl_osee_recharge_log"."shop_type" IS '类型';
COMMENT ON COLUMN "tbl_osee_recharge_log"."count" IS '数量';
COMMENT ON COLUMN "tbl_osee_recharge_log"."creator" IS '创建数量';
COMMENT ON COLUMN "tbl_osee_recharge_log"."recharge_type" IS '充值方式';
COMMENT ON COLUMN "tbl_osee_recharge_log"."order_state" IS '订单状态';
DROP TABLE IF EXISTS "tbl_osee_two_eight_record_log";
CREATE TABLE "tbl_osee_two_eight_record_log"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "money" bigint NULL DEFAULT NULL,
  "playerId" bigint NULL DEFAULT NULL,
  "nickname" varchar(32) NOT NULL,
  "playBeforeMoney" bigint NULL DEFAULT NULL,
  "playAfterMoney" bigint NULL DEFAULT NULL,
  "input" bigint NOT NULL,
  "cardType" varchar(128) NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_osee_two_eight_record_log"."id" IS '记录id';
COMMENT ON COLUMN "tbl_osee_two_eight_record_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_osee_two_eight_record_log"."money" IS '账户金币变动数额';
COMMENT ON COLUMN "tbl_osee_two_eight_record_log"."playerId" IS '玩家id';
COMMENT ON COLUMN "tbl_osee_two_eight_record_log"."nickname" IS '玩家昵称';
COMMENT ON COLUMN "tbl_osee_two_eight_record_log"."playBeforeMoney" IS '游戏前剩余金币';
COMMENT ON COLUMN "tbl_osee_two_eight_record_log"."playAfterMoney" IS '游戏后剩余金币';
COMMENT ON COLUMN "tbl_osee_two_eight_record_log"."input" IS '下注金额';
COMMENT ON COLUMN "tbl_osee_two_eight_record_log"."cardType" IS '牌型';
DROP TABLE IF EXISTS "tbl_osee_unreal_lottery_log";
CREATE TABLE "tbl_osee_unreal_lottery_log"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "order_num" varchar(32) NULL DEFAULT NULL,
  "user_id" bigint NOT NULL,
  "nickname" varchar(32) NULL DEFAULT NULL,
  "reward_name" varchar(32) NULL DEFAULT NULL,
  "type" integer NOT NULL,
  "count" integer NOT NULL,
  "item_id" integer NOT NULL,
  "cost" integer NOT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_osee_unreal_lottery_log"."id" IS '记录id';
COMMENT ON COLUMN "tbl_osee_unreal_lottery_log"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_osee_unreal_lottery_log"."order_num" IS '订单号';
COMMENT ON COLUMN "tbl_osee_unreal_lottery_log"."user_id" IS '兑换人id';
COMMENT ON COLUMN "tbl_osee_unreal_lottery_log"."nickname" IS '昵称';
COMMENT ON COLUMN "tbl_osee_unreal_lottery_log"."reward_name" IS '商品名';
COMMENT ON COLUMN "tbl_osee_unreal_lottery_log"."type" IS '类型';
COMMENT ON COLUMN "tbl_osee_unreal_lottery_log"."count" IS '兑换数量';
COMMENT ON COLUMN "tbl_osee_unreal_lottery_log"."item_id" IS '消耗类型';
COMMENT ON COLUMN "tbl_osee_unreal_lottery_log"."cost" IS '消耗数量';
DROP TABLE IF EXISTS "tbl_ttmy_agent_commission_info_log";
CREATE TABLE "tbl_ttmy_agent_commission_info_log"  (
  "id" serial,
  "player_id" bigint NOT NULL,
  "player_name" varchar(100) NOT NULL,
  "shop_name" varchar(100) NOT NULL,
  "channel_id" bigint NULL DEFAULT NULL,
  "promoter_id" bigint NULL DEFAULT NULL,
  "commission" numeric(8, 4) NULL DEFAULT NULL,
  "sec_commission" numeric(8, 4) NULL DEFAULT NULL,
  "money" numeric(8, 4) NULL DEFAULT NULL,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_ttmy_agent_commission_info_log"."id" IS 'ID';
COMMENT ON COLUMN "tbl_ttmy_agent_commission_info_log"."player_id" IS '充值玩家ID';
COMMENT ON COLUMN "tbl_ttmy_agent_commission_info_log"."player_name" IS '充值玩家昵称';
COMMENT ON COLUMN "tbl_ttmy_agent_commission_info_log"."shop_name" IS '商品名';
COMMENT ON COLUMN "tbl_ttmy_agent_commission_info_log"."channel_id" IS '渠道ID';
COMMENT ON COLUMN "tbl_ttmy_agent_commission_info_log"."promoter_id" IS '推广员ID';
COMMENT ON COLUMN "tbl_ttmy_agent_commission_info_log"."commission" IS '计算出的渠道商佣金数量(取整)';
COMMENT ON COLUMN "tbl_ttmy_agent_commission_info_log"."sec_commission" IS '计算出的推广员佣金数量(取整)';
COMMENT ON COLUMN "tbl_ttmy_agent_commission_info_log"."money" IS '充值金额';
COMMENT ON COLUMN "tbl_ttmy_agent_commission_info_log"."create_time" IS '数据创建时间';
DROP TABLE IF EXISTS "tbl_ttmy_agent_commission_log";
CREATE TABLE "tbl_ttmy_agent_commission_log"  (
  "id" serial,
  "player_id" bigint NOT NULL,
  "player_name" varchar(100) NOT NULL,
  "agent_player_id" bigint NULL DEFAULT NULL,
  "agent_player_name" varchar(100) NULL DEFAULT NULL,
  "commission_rate" numeric(8, 4) NULL DEFAULT 0.0500,
  "commission" numeric(8, 4) NULL DEFAULT NULL,
  "money" bigint NULL DEFAULT NULL,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_ttmy_agent_commission_log"."id" IS 'ID';
COMMENT ON COLUMN "tbl_ttmy_agent_commission_log"."player_id" IS '充值玩家ID';
COMMENT ON COLUMN "tbl_ttmy_agent_commission_log"."player_name" IS '充值玩家昵称';
COMMENT ON COLUMN "tbl_ttmy_agent_commission_log"."agent_player_id" IS '玩家上级代理ID(不一定是直属)';
COMMENT ON COLUMN "tbl_ttmy_agent_commission_log"."agent_player_name" IS '玩家上级代理昵称(不一定是直属)';
COMMENT ON COLUMN "tbl_ttmy_agent_commission_log"."commission_rate" IS '计算的佣金比例';
COMMENT ON COLUMN "tbl_ttmy_agent_commission_log"."commission" IS '计算出的佣金数量(取整)';
COMMENT ON COLUMN "tbl_ttmy_agent_commission_log"."money" IS '充值金额';
COMMENT ON COLUMN "tbl_ttmy_agent_commission_log"."create_time" IS '数据创建时间';
DROP TABLE IF EXISTS "tbl_ttmy_agent_withdraw_log";
CREATE TABLE "tbl_ttmy_agent_withdraw_log"  (
  "id" serial,
  "agent_id" bigint NOT NULL,
  "bank" varchar(32) NOT NULL,
  "real_name" varchar(32) NOT NULL,
  "bank_num" varchar(32) NOT NULL,
  "open_bank" varchar(64) NOT NULL,
  "money" bigint NULL DEFAULT NULL,
  "state" integer NULL DEFAULT NULL,
  "creator" varchar(32) NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_ttmy_agent_withdraw_log"."id" IS 'ID';
COMMENT ON COLUMN "tbl_ttmy_agent_withdraw_log"."agent_id" IS '代理ID';
COMMENT ON COLUMN "tbl_ttmy_agent_withdraw_log"."bank" IS '银行';
COMMENT ON COLUMN "tbl_ttmy_agent_withdraw_log"."real_name" IS '户名';
COMMENT ON COLUMN "tbl_ttmy_agent_withdraw_log"."bank_num" IS '卡号';
COMMENT ON COLUMN "tbl_ttmy_agent_withdraw_log"."open_bank" IS '开户行';
COMMENT ON COLUMN "tbl_ttmy_agent_withdraw_log"."money" IS '提现金额';
COMMENT ON COLUMN "tbl_ttmy_agent_withdraw_log"."state" IS '状态';
COMMENT ON COLUMN "tbl_ttmy_agent_withdraw_log"."creator" IS '操作管理员';
DROP TABLE IF EXISTS "tbl_ttmy_commission_exchange_log";
CREATE TABLE "tbl_ttmy_commission_exchange_log"  (
  "id" serial,
  "agent_id" bigint NOT NULL,
  "agent_name" varchar(100) NOT NULL,
  "bronze_torpedo_num" bigint NULL DEFAULT NULL,
  "silver_torpedo_num" bigint NULL DEFAULT NULL,
  "gold_torpedo_num" bigint NULL DEFAULT NULL,
  "gold_num" bigint NULL DEFAULT NULL,
  "cost_commission" bigint NULL DEFAULT NULL,
  "rest_commission" bigint NULL DEFAULT NULL,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_ttmy_commission_exchange_log"."id" IS 'ID';
COMMENT ON COLUMN "tbl_ttmy_commission_exchange_log"."agent_id" IS '代理玩家ID';
COMMENT ON COLUMN "tbl_ttmy_commission_exchange_log"."agent_name" IS '代理玩家昵称';
COMMENT ON COLUMN "tbl_ttmy_commission_exchange_log"."bronze_torpedo_num" IS '青铜鱼雷数量';
COMMENT ON COLUMN "tbl_ttmy_commission_exchange_log"."silver_torpedo_num" IS '白银鱼雷数量';
COMMENT ON COLUMN "tbl_ttmy_commission_exchange_log"."gold_torpedo_num" IS '黄金鱼雷数量';
COMMENT ON COLUMN "tbl_ttmy_commission_exchange_log"."gold_num" IS '金币数量';
COMMENT ON COLUMN "tbl_ttmy_commission_exchange_log"."cost_commission" IS '花费的佣金';
COMMENT ON COLUMN "tbl_ttmy_commission_exchange_log"."rest_commission" IS '该次兑换后剩余的佣金';
COMMENT ON COLUMN "tbl_ttmy_commission_exchange_log"."create_time" IS '数据创建时间';
DROP TABLE IF EXISTS "tbl_ttmy_give_gift_log";
CREATE TABLE "tbl_ttmy_give_gift_log"  (
  "id" bigserial,
  "from_id" bigint NULL DEFAULT NULL,
  "from_name" varchar(100) NULL DEFAULT NULL,
  "to_id" bigint NULL DEFAULT NULL,
  "to_name" varchar(100) NULL DEFAULT NULL,
  "gift_name" varchar(100) NULL DEFAULT NULL,
  "gift_num" bigint NULL DEFAULT NULL,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_ttmy_give_gift_log"."id" IS 'ID';
COMMENT ON COLUMN "tbl_ttmy_give_gift_log"."from_id" IS '赠送人ID';
COMMENT ON COLUMN "tbl_ttmy_give_gift_log"."from_name" IS '赠送人昵称';
COMMENT ON COLUMN "tbl_ttmy_give_gift_log"."to_id" IS '被赠送人ID';
COMMENT ON COLUMN "tbl_ttmy_give_gift_log"."to_name" IS '被赠送人昵称';
COMMENT ON COLUMN "tbl_ttmy_give_gift_log"."gift_name" IS '赠送礼物名称';
COMMENT ON COLUMN "tbl_ttmy_give_gift_log"."gift_num" IS '赠送礼物数量';
COMMENT ON COLUMN "tbl_ttmy_give_gift_log"."create_time" IS '数据创建时间';
DROP TABLE IF EXISTS "tbl_ttmy_ten_challenge_ranking_log";
CREATE TABLE "tbl_ttmy_ten_challenge_ranking_log"  (
  "id" bigserial,
  "user_id" bigint NOT NULL,
  "nickname" varchar(64) NULL DEFAULT NULL,
  "head_index" integer NULL DEFAULT NULL,
  "head_url" varchar(512) NULL DEFAULT NULL,
  "score" bigint NOT NULL,
  "update_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_ttmy_ten_challenge_ranking_log"."id" IS 'ID';
COMMENT ON COLUMN "tbl_ttmy_ten_challenge_ranking_log"."user_id" IS '用户id';
COMMENT ON COLUMN "tbl_ttmy_ten_challenge_ranking_log"."nickname" IS '昵称';
COMMENT ON COLUMN "tbl_ttmy_ten_challenge_ranking_log"."head_index" IS '头像序号';
COMMENT ON COLUMN "tbl_ttmy_ten_challenge_ranking_log"."head_url" IS '头像地址';
COMMENT ON COLUMN "tbl_ttmy_ten_challenge_ranking_log"."score" IS '得分';
COMMENT ON COLUMN "tbl_ttmy_ten_challenge_ranking_log"."update_time" IS '数据更新时间';
COMMENT ON COLUMN "tbl_ttmy_ten_challenge_ranking_log"."create_time" IS '数据创建时间';