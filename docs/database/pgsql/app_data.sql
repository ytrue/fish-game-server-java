-- ============================================================
-- PostgreSQL 版（由 MySQL 结构自动转换）
-- Schema: app_data
-- 转换规则：
--   * 反引号 `  -> 双引号 "
--   * AUTO_INCREMENT -> serial / bigserial
--   * int(n)/bigint(n)/tinyint(n) -> integer/bigint/smallint
--   * datetime -> timestamp
--   * decimal -> numeric
--   * COMMENT '...' -> COMMENT ON COLUMN ... IS '...'
--   * ENGINE / CHARACTER SET / COLLATE / USING BTREE 已移除
-- ============================================================

DROP TABLE IF EXISTS "tbl_fruit_laba_reward_info";
CREATE TABLE "tbl_fruit_laba_reward_info"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "user_id" bigint NULL DEFAULT NULL,
  "achieve_num" integer NULL DEFAULT NULL,
  "reward_gold" integer NULL DEFAULT NULL,
  "reward_lottery" integer NULL DEFAULT NULL,
  "weather_receive" smallint NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_fruit_laba_reward_info"."id" IS '奖励id';
COMMENT ON COLUMN "tbl_fruit_laba_reward_info"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_fruit_laba_reward_info"."user_id" IS '用户id';
COMMENT ON COLUMN "tbl_fruit_laba_reward_info"."achieve_num" IS '旋转次数';
COMMENT ON COLUMN "tbl_fruit_laba_reward_info"."reward_gold" IS '奖励金币';
COMMENT ON COLUMN "tbl_fruit_laba_reward_info"."reward_lottery" IS '奖励点券';
COMMENT ON COLUMN "tbl_fruit_laba_reward_info"."weather_receive" IS '是否领取';
DROP TABLE IF EXISTS "tbl_app_rate_form";
CREATE TABLE "tbl_app_rate_form"  (
  "id" bigserial,
  "day_time" varchar(255) NOT NULL,
  "rate" varchar(255) NOT NULL DEFAULT '0.00',
  "arppu" varchar(255) NOT NULL DEFAULT '0.00',
  "arpu" varchar(255) NOT NULL DEFAULT '0.00',
  "new_rate" varchar(255) NOT NULL DEFAULT '0.00',
  "new_arppu" varchar(255) NOT NULL DEFAULT '0.00',
  "new_arpu" varchar(255) NOT NULL DEFAULT '0.00',
  "new_pay_num" varchar(10) NOT NULL DEFAULT '0',
  "login_num" varchar(10) NOT NULL DEFAULT '0',
  "pay_num" varchar(10) NOT NULL DEFAULT '0',
  "all_money" varchar(255) NOT NULL DEFAULT '0',
  "agent_id" bigint NOT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_app_rate_form"."rate" IS '付费率';
COMMENT ON COLUMN "tbl_app_rate_form"."arppu" IS 'arppu';
COMMENT ON COLUMN "tbl_app_rate_form"."arpu" IS 'arpu';
COMMENT ON COLUMN "tbl_app_rate_form"."new_rate" IS '新增付费率';
COMMENT ON COLUMN "tbl_app_rate_form"."new_arppu" IS '新增arppu';
COMMENT ON COLUMN "tbl_app_rate_form"."new_arpu" IS '新增arpu';
COMMENT ON COLUMN "tbl_app_rate_form"."new_pay_num" IS '新增付费人数';
COMMENT ON COLUMN "tbl_app_rate_form"."login_num" IS '登陆人数';
COMMENT ON COLUMN "tbl_app_rate_form"."pay_num" IS '付费人数';
COMMENT ON COLUMN "tbl_app_rate_form"."all_money" IS '总金额';
DROP TABLE IF EXISTS "tbl_app_report_form";
CREATE TABLE "tbl_app_report_form"  (
  "id" bigserial,
  "day_time" varchar(50) NULL DEFAULT NULL,
  "day1" varchar(255) NOT NULL DEFAULT '0.00',
  "day2" varchar(255) NOT NULL DEFAULT '0.00',
  "day3" varchar(255) NOT NULL DEFAULT '0.00',
  "day4" varchar(255) NOT NULL DEFAULT '0.00',
  "day5" varchar(255) NOT NULL DEFAULT '0.00',
  "day6" varchar(255) NOT NULL DEFAULT '0.00',
  "day7" varchar(255) NOT NULL DEFAULT '0.00',
  "day8" varchar(255) NOT NULL DEFAULT '0.00',
  "day9" varchar(255) NOT NULL DEFAULT '0.00',
  "day10" varchar(255) NOT NULL DEFAULT '0.00',
  "day11" varchar(255) NOT NULL DEFAULT '0.00',
  "day12" varchar(255) NOT NULL DEFAULT '0.00',
  "day13" varchar(255) NOT NULL DEFAULT '0.00',
  "day14" varchar(255) NOT NULL DEFAULT '0.00',
  "day15" varchar(255) NOT NULL DEFAULT '0.00',
  "day16" varchar(255) NOT NULL DEFAULT '0.00',
  "day17" varchar(255) NOT NULL DEFAULT '0.00',
  "day18" varchar(255) NOT NULL DEFAULT '0.00',
  "day19" varchar(255) NOT NULL DEFAULT '0.00',
  "day20" varchar(255) NOT NULL DEFAULT '0.00',
  "day21" varchar(255) NOT NULL DEFAULT '0.00',
  "day22" varchar(255) NOT NULL DEFAULT '0.00',
  "day23" varchar(255) NOT NULL DEFAULT '0.00',
  "day24" varchar(255) NOT NULL DEFAULT '0.00',
  "day25" varchar(255) NOT NULL DEFAULT '0.00',
  "day26" varchar(255) NOT NULL DEFAULT '0.00',
  "day27" varchar(255) NOT NULL DEFAULT '0.00',
  "day28" varchar(255) NOT NULL DEFAULT '0.00',
  "day29" varchar(255) NOT NULL DEFAULT '0.00',
  "day30" varchar(255) NOT NULL DEFAULT '0.00',
  "type" varchar(255) NOT NULL,
  "agent_id" bigint NOT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_app_report_form"."type" IS '类型：register（注册）  pay(付费）';
DROP TABLE IF EXISTS "tbl_app_user";
CREATE TABLE "tbl_app_user"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "username" varchar(32) NULL DEFAULT NULL,
  "phonenum" varchar(11) NULL DEFAULT NULL,
  "openid" varchar(500) NULL DEFAULT NULL,
  "unionid" varchar(64) NULL DEFAULT NULL,
  "password" varchar(32) NULL DEFAULT NULL,
  "nickname" varchar(64) NULL DEFAULT NULL,
  "head_index" integer NULL DEFAULT NULL,
  "head_url" varchar(512) NULL DEFAULT NULL,
  "sex" integer NULL DEFAULT NULL,
  "my_invite_code" integer NULL DEFAULT NULL,
  "invite_code" integer NULL DEFAULT NULL,
  "user_state" integer NULL DEFAULT 0,
  "online_state" integer NULL DEFAULT 0,
  PRIMARY KEY ("id"),
  CONSTRAINT username UNIQUE ("username")
);
COMMENT ON COLUMN "tbl_app_user"."id" IS '用户id';
COMMENT ON COLUMN "tbl_app_user"."create_time" IS '注册时间';
COMMENT ON COLUMN "tbl_app_user"."username" IS '用户名';
COMMENT ON COLUMN "tbl_app_user"."phonenum" IS '手机号';
COMMENT ON COLUMN "tbl_app_user"."openid" IS '第三方平台id';
COMMENT ON COLUMN "tbl_app_user"."unionid" IS '第三方平台唯一id';
COMMENT ON COLUMN "tbl_app_user"."password" IS '密码(32位md5)';
COMMENT ON COLUMN "tbl_app_user"."nickname" IS '昵称';
COMMENT ON COLUMN "tbl_app_user"."head_index" IS '头像序号';
COMMENT ON COLUMN "tbl_app_user"."head_url" IS '头像地址';
COMMENT ON COLUMN "tbl_app_user"."sex" IS '性别';
COMMENT ON COLUMN "tbl_app_user"."my_invite_code" IS '我的邀请码';
COMMENT ON COLUMN "tbl_app_user"."invite_code" IS '我绑定的邀请码';
COMMENT ON COLUMN "tbl_app_user"."user_state" IS '用户状态';
COMMENT ON COLUMN "tbl_app_user"."online_state" IS '在线状态';
DROP TABLE IF EXISTS "tbl_app_user_authentication";
CREATE TABLE "tbl_app_user_authentication"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "user_id" bigint NOT NULL,
  "name" varchar(16) NULL DEFAULT NULL,
  "idcard_no" varchar(20) NULL DEFAULT NULL,
  "phone_no" varchar(16) NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_app_user_authentication"."id" IS '认证id';
COMMENT ON COLUMN "tbl_app_user_authentication"."create_time" IS '认证时间';
COMMENT ON COLUMN "tbl_app_user_authentication"."user_id" IS '用户id';
COMMENT ON COLUMN "tbl_app_user_authentication"."name" IS '真实姓名';
COMMENT ON COLUMN "tbl_app_user_authentication"."idcard_no" IS '身份证号码';
COMMENT ON COLUMN "tbl_app_user_authentication"."phone_no" IS '手机号码';
DROP TABLE IF EXISTS "tbl_app_wander_subtitle";
CREATE TABLE "tbl_app_wander_subtitle"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "content" varchar(1024) NULL DEFAULT NULL,
  "interval_time" integer NULL DEFAULT NULL,
  "start_time" timestamp NULL DEFAULT NULL,
  "end_time" timestamp NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_app_wander_subtitle"."id" IS '游走字幕id';
COMMENT ON COLUMN "tbl_app_wander_subtitle"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_app_wander_subtitle"."content" IS '字幕内容';
COMMENT ON COLUMN "tbl_app_wander_subtitle"."interval_time" IS '间隔时间';
COMMENT ON COLUMN "tbl_app_wander_subtitle"."start_time" IS '生效时间';
COMMENT ON COLUMN "tbl_app_wander_subtitle"."end_time" IS '失效时间';
DROP TABLE IF EXISTS "tbl_osee_cdk";
CREATE TABLE "tbl_osee_cdk"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "cdkey" varchar(32) NULL DEFAULT NULL,
  "type_id" bigint NOT NULL,
  "rewards" varchar(512) NULL DEFAULT NULL,
  "user_id" bigint NULL DEFAULT NULL,
  "nickname" varchar(512) NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_osee_cdk"."id" IS 'cdk id';
COMMENT ON COLUMN "tbl_osee_cdk"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_osee_cdk"."cdkey" IS 'cdkey内容';
COMMENT ON COLUMN "tbl_osee_cdk"."type_id" IS '类型id';
COMMENT ON COLUMN "tbl_osee_cdk"."rewards" IS '奖励';
COMMENT ON COLUMN "tbl_osee_cdk"."user_id" IS '兑换人id';
COMMENT ON COLUMN "tbl_osee_cdk"."nickname" IS '兑换人昵称';
DROP TABLE IF EXISTS "tbl_osee_cdk_type";
CREATE TABLE "tbl_osee_cdk_type"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "name" varchar(32) NULL DEFAULT NULL,
  "start_with" varchar(32) NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_osee_cdk_type"."id" IS '类型id';
COMMENT ON COLUMN "tbl_osee_cdk_type"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_osee_cdk_type"."name" IS '类型名';
COMMENT ON COLUMN "tbl_osee_cdk_type"."start_with" IS '开头字符';
DROP TABLE IF EXISTS "tbl_osee_lottery_shop";
CREATE TABLE "tbl_osee_lottery_shop"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "index" integer NULL DEFAULT NULL,
  "type" integer NULL DEFAULT NULL,
  "count" bigint NOT NULL,
  "name" varchar(32) NULL DEFAULT NULL,
  "cost" bigint NOT NULL,
  "img" varchar(512) NULL DEFAULT NULL,
  "size" integer NULL DEFAULT NULL,
  "used_size" integer NULL DEFAULT NULL,
  "send_type" integer NULL DEFAULT NULL,
  "stock" integer NULL DEFAULT NULL,
  "refresh_type" integer NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_osee_lottery_shop"."id" IS '类型id';
COMMENT ON COLUMN "tbl_osee_lottery_shop"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_osee_lottery_shop"."index" IS '序号';
COMMENT ON COLUMN "tbl_osee_lottery_shop"."type" IS '类型';
COMMENT ON COLUMN "tbl_osee_lottery_shop"."count" IS '物品数量';
COMMENT ON COLUMN "tbl_osee_lottery_shop"."name" IS '名称';
COMMENT ON COLUMN "tbl_osee_lottery_shop"."cost" IS '消耗奖券数量';
COMMENT ON COLUMN "tbl_osee_lottery_shop"."img" IS '图片';
COMMENT ON COLUMN "tbl_osee_lottery_shop"."size" IS '总数量';
COMMENT ON COLUMN "tbl_osee_lottery_shop"."used_size" IS '已兑换数量';
COMMENT ON COLUMN "tbl_osee_lottery_shop"."send_type" IS '发货类型';
COMMENT ON COLUMN "tbl_osee_lottery_shop"."stock" IS '库存';
COMMENT ON COLUMN "tbl_osee_lottery_shop"."refresh_type" IS '刷新类型';
DROP TABLE IF EXISTS "tbl_osee_lottery_shop_stock";
CREATE TABLE "tbl_osee_lottery_shop_stock"  (
  "id" bigserial,
  "shop_id" bigint NOT NULL,
  "user_id" bigint NULL DEFAULT NULL,
  "number" varchar(100) NOT NULL,
  "password" varchar(100) NOT NULL,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_osee_lottery_shop_stock"."id" IS 'ID';
COMMENT ON COLUMN "tbl_osee_lottery_shop_stock"."shop_id" IS '道具库存属于的商品ID';
COMMENT ON COLUMN "tbl_osee_lottery_shop_stock"."user_id" IS '兑换该卡的玩家ID';
COMMENT ON COLUMN "tbl_osee_lottery_shop_stock"."number" IS '卡号';
COMMENT ON COLUMN "tbl_osee_lottery_shop_stock"."password" IS '卡密';
COMMENT ON COLUMN "tbl_osee_lottery_shop_stock"."create_time" IS '数据创建时间';
DROP TABLE IF EXISTS "tbl_osee_notice";
CREATE TABLE "tbl_osee_notice"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "index" integer NULL DEFAULT NULL,
  "title" varchar(32) NULL DEFAULT NULL,
  "content" varchar(512) NULL DEFAULT NULL,
  "start_time" timestamp NULL DEFAULT NULL,
  "end_time" timestamp NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_osee_notice"."id" IS '类型id';
COMMENT ON COLUMN "tbl_osee_notice"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_osee_notice"."index" IS '序号';
COMMENT ON COLUMN "tbl_osee_notice"."title" IS '标题';
COMMENT ON COLUMN "tbl_osee_notice"."content" IS '内容';
COMMENT ON COLUMN "tbl_osee_notice"."start_time" IS '生效时间';
COMMENT ON COLUMN "tbl_osee_notice"."end_time" IS '失效时间';
DROP TABLE IF EXISTS "tbl_osee_player";
CREATE TABLE "tbl_osee_player"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "user_id" bigint NULL DEFAULT NULL,
  "money" bigint NULL DEFAULT NULL,
  "bank_money" bigint NULL DEFAULT NULL,
  "bank_password" varchar(32) NULL DEFAULT NULL,
  "lottery" bigint NULL DEFAULT NULL,
  "diamond" bigint NULL DEFAULT NULL,
  "vip_level" integer NULL DEFAULT NULL,
  "level" integer NULL DEFAULT NULL,
  "experience" bigint NULL DEFAULT NULL,
  "recharge_money" bigint NULL DEFAULT NULL,
  "lose_control" integer NULL DEFAULT NULL,
  "player_type" integer NULL DEFAULT NULL,
  "bronze_torpedo" bigint NULL DEFAULT NULL,
  "silver_torpedo" bigint NULL DEFAULT NULL,
  "gold_torpedo" bigint NULL DEFAULT NULL,
  "skill_lock" bigint NULL DEFAULT NULL,
  "skill_frozen" bigint NULL DEFAULT NULL,
  "skill_fast" bigint NULL DEFAULT NULL,
  "skill_crit" bigint NULL DEFAULT NULL,
  "boss_bugle" bigint NULL DEFAULT NULL,
  "battery_level" integer NULL DEFAULT NULL,
  "monthcard_expire_date" date NULL DEFAULT NULL,
  "ten_challenge_times" bigint NULL DEFAULT NULL,
  "qszs_battery_expire_date" date NULL DEFAULT NULL,
  "blnh_battery_expire_date" date NULL DEFAULT NULL,
  "lhtz_battery_expire_date" date NULL DEFAULT NULL,
  "swhp_battery_expire_date" date NULL DEFAULT NULL,
  "dragon_crystal" bigint NULL DEFAULT NULL,
  "fen_shen" bigint NULL DEFAULT NULL,
  "send_gift" bigint NOT NULL DEFAULT 0,
  "gold_torpedo_bang" bigint NULL DEFAULT NULL,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_osee_player"."id" IS '玩家id';
COMMENT ON COLUMN "tbl_osee_player"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_osee_player"."user_id" IS '用户id';
COMMENT ON COLUMN "tbl_osee_player"."money" IS '玩家金币';
COMMENT ON COLUMN "tbl_osee_player"."bank_money" IS '保险箱金币';
COMMENT ON COLUMN "tbl_osee_player"."bank_password" IS '保险箱密码';
COMMENT ON COLUMN "tbl_osee_player"."lottery" IS '奖券';
COMMENT ON COLUMN "tbl_osee_player"."diamond" IS '钻石';
COMMENT ON COLUMN "tbl_osee_player"."vip_level" IS 'vip等级';
COMMENT ON COLUMN "tbl_osee_player"."level" IS '玩家等级';
COMMENT ON COLUMN "tbl_osee_player"."experience" IS '玩家经验';
COMMENT ON COLUMN "tbl_osee_player"."recharge_money" IS '充值金额';
COMMENT ON COLUMN "tbl_osee_player"."lose_control" IS '必输控制';
COMMENT ON COLUMN "tbl_osee_player"."player_type" IS '玩家类型';
COMMENT ON COLUMN "tbl_osee_player"."bronze_torpedo" IS '玩家青铜鱼雷数量';
COMMENT ON COLUMN "tbl_osee_player"."silver_torpedo" IS '玩家白银鱼雷数量';
COMMENT ON COLUMN "tbl_osee_player"."gold_torpedo" IS '玩家黄金鱼雷数量';
COMMENT ON COLUMN "tbl_osee_player"."skill_lock" IS '玩家锁定技能数量';
COMMENT ON COLUMN "tbl_osee_player"."skill_frozen" IS '玩家冰冻技能数量';
COMMENT ON COLUMN "tbl_osee_player"."skill_fast" IS '玩家急速技能数量';
COMMENT ON COLUMN "tbl_osee_player"."skill_crit" IS '玩家暴击技能数量';
COMMENT ON COLUMN "tbl_osee_player"."boss_bugle" IS '玩家BOSS号角数量';
COMMENT ON COLUMN "tbl_osee_player"."battery_level" IS '玩家目前拥有的最高炮台等级';
COMMENT ON COLUMN "tbl_osee_player"."monthcard_expire_date" IS '玩家月卡到期时间';
COMMENT ON COLUMN "tbl_osee_player"."ten_challenge_times" IS '玩家拼十剩余挑战次数';
COMMENT ON COLUMN "tbl_osee_player"."qszs_battery_expire_date" IS '骑士之誓炮台外观到期时间';
COMMENT ON COLUMN "tbl_osee_player"."blnh_battery_expire_date" IS '冰龙怒吼炮台外观到期时间';
COMMENT ON COLUMN "tbl_osee_player"."lhtz_battery_expire_date" IS '莲花童子炮台外观到期时间';
COMMENT ON COLUMN "tbl_osee_player"."swhp_battery_expire_date" IS '死亡火炮炮台外观到期时间';
COMMENT ON COLUMN "tbl_osee_player"."dragon_crystal" IS '玩家拥有的龙晶数量';
COMMENT ON COLUMN "tbl_osee_player"."fen_shen" IS '玩家拥有的分身炮道具数量';
COMMENT ON COLUMN "tbl_osee_player"."send_gift" IS '赠送功能：0：正常、1：限制  默认为正常状态';
COMMENT ON COLUMN "tbl_osee_player"."gold_torpedo_bang" IS '玩家非绑定黄金鱼雷数量';
DROP TABLE IF EXISTS "tbl_shopping";
CREATE TABLE "tbl_shopping"  (
  "id" bigserial,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "player_id" bigint NULL DEFAULT NULL,
  "daily_bag_info" varchar(1024) NULL DEFAULT NULL,
  "once_bag_info" varchar(1024) NULL DEFAULT NULL,
  "money_card" smallint NULL DEFAULT NULL,
  "last_receive" timestamp NULL DEFAULT '2010-01-01 00:00:00',
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_shopping"."id" IS 'id';
COMMENT ON COLUMN "tbl_shopping"."create_time" IS '创建时间';
COMMENT ON COLUMN "tbl_shopping"."player_id" IS '用户id';
COMMENT ON COLUMN "tbl_shopping"."daily_bag_info" IS '每日礼包';
COMMENT ON COLUMN "tbl_shopping"."once_bag_info" IS '特惠礼包';
COMMENT ON COLUMN "tbl_shopping"."money_card" IS '金币卡';
COMMENT ON COLUMN "tbl_shopping"."last_receive" IS '最后领取时间';
DROP TABLE IF EXISTS "tbl_ttmy_address";
CREATE TABLE "tbl_ttmy_address"  (
  "id" bigserial,
  "player_id" bigint NOT NULL,
  "name" varchar(50) NOT NULL,
  "phone" varchar(11) NOT NULL,
  "address" varchar(500) NOT NULL,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_ttmy_address"."id" IS 'ID';
COMMENT ON COLUMN "tbl_ttmy_address"."player_id" IS '玩家ID';
COMMENT ON COLUMN "tbl_ttmy_address"."name" IS '玩家昵称';
COMMENT ON COLUMN "tbl_ttmy_address"."phone" IS '玩家手机号码';
COMMENT ON COLUMN "tbl_ttmy_address"."address" IS '玩家收货地址';
COMMENT ON COLUMN "tbl_ttmy_address"."create_time" IS '数据创建时间';
DROP TABLE IF EXISTS "tbl_ttmy_agent";
CREATE TABLE "tbl_ttmy_agent"  (
  "id" serial,
  "player_id" bigint NOT NULL,
  "player_name" varchar(100) NOT NULL,
  "agent_level" integer NOT NULL,
  "agent_player_id" bigint NULL DEFAULT NULL,
  "upper_player_id" bigint NULL DEFAULT NULL,
  "first_commission_rate" numeric(8, 4) NULL DEFAULT 0.0500,
  "second_commission_rate" numeric(8, 4) NULL DEFAULT 0.0500,
  "total_commission" numeric(8, 4) NULL DEFAULT NULL,
  "total_active_money" bigint NOT NULL DEFAULT 0,
  "total_active_dragon_crystal" bigint NOT NULL DEFAULT 0,
  "invite_qrcode_img" text NULL,
  "invite_url" varchar(100) NULL DEFAULT NULL,
  "state" integer NULL DEFAULT NULL,
  "bank" varchar(32) NULL DEFAULT NULL,
  "real_name" varchar(32) NULL DEFAULT NULL,
  "bank_num" varchar(32) NULL DEFAULT NULL,
  "open_bank" varchar(64) NULL DEFAULT NULL,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "first_pay_rate" numeric(8, 0) NULL DEFAULT NULL,
  "other_pay_rate" numeric(8, 0) NULL DEFAULT NULL,
  "pay_way" integer NULL DEFAULT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT player_id_index UNIQUE ("player_id")
);
COMMENT ON COLUMN "tbl_ttmy_agent"."id" IS 'ID';
COMMENT ON COLUMN "tbl_ttmy_agent"."player_id" IS '玩家ID';
COMMENT ON COLUMN "tbl_ttmy_agent"."player_name" IS '玩家昵称';
COMMENT ON COLUMN "tbl_ttmy_agent"."agent_level" IS '代理等级';
COMMENT ON COLUMN "tbl_ttmy_agent"."agent_player_id" IS '上级代理玩家ID';
COMMENT ON COLUMN "tbl_ttmy_agent"."upper_player_id" IS '上级玩家ID';
COMMENT ON COLUMN "tbl_ttmy_agent"."first_commission_rate" IS '一级代理玩家佣金比例';
COMMENT ON COLUMN "tbl_ttmy_agent"."second_commission_rate" IS '二级代理玩家佣金比例';
COMMENT ON COLUMN "tbl_ttmy_agent"."total_commission" IS '赚取的总佣金';
COMMENT ON COLUMN "tbl_ttmy_agent"."total_active_money" IS '赚取的总活跃金币';
COMMENT ON COLUMN "tbl_ttmy_agent"."total_active_dragon_crystal" IS '赚取的总活跃龙晶';
COMMENT ON COLUMN "tbl_ttmy_agent"."invite_qrcode_img" IS '代理邀请二维码图片';
COMMENT ON COLUMN "tbl_ttmy_agent"."invite_url" IS '代理邀请链接';
COMMENT ON COLUMN "tbl_ttmy_agent"."state" IS '代理身份状态';
COMMENT ON COLUMN "tbl_ttmy_agent"."bank" IS '银行';
COMMENT ON COLUMN "tbl_ttmy_agent"."real_name" IS '户名';
COMMENT ON COLUMN "tbl_ttmy_agent"."bank_num" IS '卡号';
COMMENT ON COLUMN "tbl_ttmy_agent"."open_bank" IS '开户行';
COMMENT ON COLUMN "tbl_ttmy_agent"."create_time" IS '数据创建时间';
COMMENT ON COLUMN "tbl_ttmy_agent"."first_pay_rate" IS '官方支付分发比例';
COMMENT ON COLUMN "tbl_ttmy_agent"."other_pay_rate" IS '分发支付比例';
COMMENT ON COLUMN "tbl_ttmy_agent"."pay_way" IS '支付方式';
COMMENT ON COLUMN "tbl_ttmy_agent"."player_id" IS '唯一用户id';
DROP TABLE IF EXISTS "tbl_ttmy_message";
CREATE TABLE "tbl_ttmy_message"  (
  "id" bigserial,
  "title" varchar(100) NOT NULL,
  "content" varchar(500) NOT NULL,
  "read" smallint NULL DEFAULT NULL,
  "receive" smallint NULL DEFAULT NULL,
  "from_id" bigint NULL DEFAULT NULL,
  "to_id" bigint NULL DEFAULT NULL,
  "items_json" varchar(512) NULL DEFAULT NULL,
  "state" integer NOT NULL,
  "create_time" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY ("id")
);
COMMENT ON COLUMN "tbl_ttmy_message"."id" IS 'ID';
COMMENT ON COLUMN "tbl_ttmy_message"."title" IS '标题';
COMMENT ON COLUMN "tbl_ttmy_message"."content" IS '内容';
COMMENT ON COLUMN "tbl_ttmy_message"."read" IS '是否已读';
COMMENT ON COLUMN "tbl_ttmy_message"."receive" IS '是否已接收附件';
COMMENT ON COLUMN "tbl_ttmy_message"."from_id" IS '发件人';
COMMENT ON COLUMN "tbl_ttmy_message"."to_id" IS '收件人';
COMMENT ON COLUMN "tbl_ttmy_message"."items_json" IS '附件信息';
COMMENT ON COLUMN "tbl_ttmy_message"."state" IS '数据状态 0-正常 1-删除';
COMMENT ON COLUMN "tbl_ttmy_message"."create_time" IS '数据创建时间';