/*
 Navicat Premium Data Transfer

 Source Server         : MySQL_本地连接
 Source Server Type    : MySQL
 Source Server Version : 80015
 Source Host           : localhost:3306
 Source Schema         : app_log

 Target Server Type    : MySQL
 Target Server Version : 80015
 File Encoding         : 65001

 Date: 16/04/2020 00:24:16
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for tbl_agent_cut_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_agent_cut_log`;
CREATE TABLE `tbl_agent_cut_log`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `player_id` bigint(20) NOT NULL COMMENT '玩家ID',
  `agent_id` bigint(20) NOT NULL COMMENT '代理ID',
  `game` int(8) NOT NULL COMMENT '游戏',
  `cut_money` bigint(20) NULL DEFAULT NULL COMMENT '金币',
  `cut_dragon_crystal` bigint(20) NULL DEFAULT NULL COMMENT '龙晶',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '数据创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 105070 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_agent_cut_receive_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_agent_cut_receive_log`;
CREATE TABLE `tbl_agent_cut_receive_log`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `agent_id` bigint(20) NOT NULL COMMENT '代理玩家ID',
  `agent_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '代理玩家昵称',
  `money` bigint(20) NULL DEFAULT NULL COMMENT '金币数量',
  `dragon_crystal` bigint(20) NULL DEFAULT NULL COMMENT '花费的佣金',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '数据创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100058 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_app_game_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_app_game_log`;
CREATE TABLE `tbl_app_game_log`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '捕鱼竞技模式游戏记录id',
  `number` int(11) NOT NULL COMMENT '参赛人数',
  `income` int(255) NOT NULL COMMENT '报名收入',
  `reward_id` int(255) NOT NULL COMMENT '奖励支出 id',
  `stock` bigint(20) NOT NULL COMMENT '库存',
  `mode` int(255) NOT NULL COMMENT '游戏模式',
  `type` int(255) NOT NULL COMMENT '游戏类型',
  `create_time` datetime(0) NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 120 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_app_login_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_app_login_log`;
CREATE TABLE `tbl_app_login_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '登录id',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '用户id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '登录时间',
  `exit_time` timestamp(0) NULL DEFAULT NULL COMMENT '退出时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 106678 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_app_rank_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_app_rank_log`;
CREATE TABLE `tbl_app_rank_log`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '竞技模式玩家比赛排名表',
  `player_id` bigint(20) NOT NULL COMMENT '玩家id',
  `nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '玩家昵称',
  `score` int(255) NOT NULL COMMENT '玩家积分',
  `rank` int(255) NOT NULL COMMENT '玩家排名',
  `mode` int(255) NOT NULL COMMENT '竞技模式类型（1：大奖赛、2：全民赛、3：满人赛、4：道具赛）',
  `cost` int(10) NOT NULL COMMENT '报名消耗钻石数',
  `games` int(255) NOT NULL COMMENT '游戏参与局数',
  `change` int(255) NOT NULL COMMENT '玩家金币变化',
  `reward_id` int(255) NULL DEFAULT NULL COMMENT '奖励表id',
  `receive` int(255) NOT NULL COMMENT '玩家奖励是否领取',
  `receive_time` datetime(0) NULL DEFAULT NULL COMMENT '领取时间',
  `create_time` datetime(0) NOT NULL COMMENT '创建时间',
  `email_id` bigint(20) NOT NULL COMMENT '邮件id',
  `type` int(255) NOT NULL COMMENT '日赛：1 周赛：2',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 184 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_app_rank_reward_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_app_rank_reward_log`;
CREATE TABLE `tbl_app_rank_reward_log`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '名称',
  `rank` int(255) NOT NULL COMMENT '小于等于该排名',
  `type` int(255) NOT NULL COMMENT '类型，日排名1 周排名2',
  `status` int(255) NOT NULL COMMENT '状态',
  `reward_id` int(11) NOT NULL COMMENT '奖励表id',
  `update_time` datetime(0) NOT NULL COMMENT '最后一次更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 55 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_app_reward_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_app_reward_log`;
CREATE TABLE `tbl_app_reward_log`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '玩家奖励表',
  `player_id` bigint(20) NOT NULL COMMENT '玩家id',
  `gold` int(11) NOT NULL COMMENT '金币',
  `diamond` int(11) NOT NULL COMMENT '钻石',
  `lower_ball` int(11) NOT NULL COMMENT '低阶龙珠',
  `middle_ball` int(11) NOT NULL COMMENT '中阶龙珠',
  `high_ball` int(11) NOT NULL COMMENT '高阶龙珠',
  `skill_lock` int(11) NOT NULL COMMENT '锁定技能',
  `skill_fast` int(255) NOT NULL COMMENT '极速技能',
  `skill_crit` int(255) NOT NULL COMMENT '暴击技能',
  `skill_frozen` int(255) NOT NULL COMMENT '冰冻技能',
  `boss_bugle` int(255) NOT NULL COMMENT 'BOSS号角',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 274 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_dragon_crystal_exchange_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_dragon_crystal_exchange_log`;
CREATE TABLE `tbl_dragon_crystal_exchange_log`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `player_id` bigint(20) NOT NULL COMMENT '玩家ID',
  `exchange_type` int(11) NOT NULL COMMENT '兑换类型 0-兑换龙晶 1-兑换鱼雷',
  `bronze_torpedo_before` bigint(20) NULL DEFAULT NULL COMMENT '青铜鱼雷数量前',
  `silver_torpedo_before` bigint(20) NULL DEFAULT NULL COMMENT '白银鱼雷数量前',
  `gold_torpedo_before` bigint(20) NULL DEFAULT NULL COMMENT '黄金鱼雷数量前',
  `bronze_torpedo_change` bigint(20) NULL DEFAULT NULL COMMENT '青铜鱼雷数量变',
  `silver_torpedo_change` bigint(20) NULL DEFAULT NULL COMMENT '白银鱼雷数量变',
  `gold_torpedo_change` bigint(20) NULL DEFAULT NULL COMMENT '黄金鱼雷数量变',
  `dragon_crystal_before` bigint(20) NULL DEFAULT NULL COMMENT '龙晶数量前',
  `dragon_crystal_change` bigint(20) NULL DEFAULT NULL COMMENT '龙晶数量变',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '数据创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_osee_cut_money_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_osee_cut_money_log`;
CREATE TABLE `tbl_osee_cut_money_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '玩家id',
  `game` int(11) NOT NULL COMMENT '游戏',
  `cut_money` bigint(20) NULL DEFAULT NULL COMMENT '变动金币',
  `type` int(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 123076 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_osee_expend_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_osee_expend_log`;
CREATE TABLE `tbl_osee_expend_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '玩家id',
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `pay_type` int(11) NOT NULL COMMENT '支出类型',
  `diamond` bigint(20) NULL DEFAULT NULL COMMENT '支出钻石',
  `money` bigint(20) NULL DEFAULT NULL COMMENT '支出金币',
  `lottery` bigint(20) NULL DEFAULT NULL COMMENT '支出奖券',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 101622 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_osee_fightten_record_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_osee_fightten_record_log`;
CREATE TABLE `tbl_osee_fightten_record_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `money` bigint(20) NULL DEFAULT NULL COMMENT '账户金币变动数额',
  `playerId` bigint(20) NULL DEFAULT NULL COMMENT '玩家id',
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '玩家昵称',
  `playBeforeMoney` bigint(20) NULL DEFAULT NULL COMMENT '游戏前剩余金币',
  `playAfterMoney` bigint(20) NULL DEFAULT NULL COMMENT '游戏后剩余金币',
  `input` bigint(20) NOT NULL COMMENT '下注金额',
  `rate` int(20) NULL DEFAULT NULL COMMENT '倍率',
  `cardType` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '牌型',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 530 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_osee_fishing_record_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_osee_fishing_record_log`;
CREATE TABLE `tbl_osee_fishing_record_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `player_id` bigint(20) NULL DEFAULT NULL COMMENT '玩家id',
  `room_index` int(11) NULL DEFAULT NULL COMMENT '场次序号',
  `spend_money` bigint(20) NULL DEFAULT NULL COMMENT '花费金币',
  `win_money` bigint(20) NULL DEFAULT NULL COMMENT '赢取金币',
  `drop_bronze_torpedo_num` bigint(20) NULL DEFAULT NULL COMMENT '掉落的青铜鱼雷数量',
  `drop_silver_torpedo_num` bigint(20) NULL DEFAULT NULL COMMENT '掉落的白银鱼雷数量',
  `drop_gold_torpedo_num` bigint(20) NULL DEFAULT NULL COMMENT '掉落的黄金鱼雷数量',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 108708 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_osee_fruit_record_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_osee_fruit_record_log`;
CREATE TABLE `tbl_osee_fruit_record_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `money` bigint(20) NULL DEFAULT NULL COMMENT '账户金币变动数额',
  `playerId` bigint(20) NULL DEFAULT NULL COMMENT '玩家id',
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '玩家昵称',
  `playBeforeMoney` bigint(20) NULL DEFAULT NULL COMMENT '游戏前剩余金币',
  `playAfterMoney` bigint(20) NULL DEFAULT NULL COMMENT '游戏后剩余金币',
  `cost` bigint(20) NULL DEFAULT NULL COMMENT '下注消耗',
  `lineNum` int(20) NULL DEFAULT NULL COMMENT '下注条数',
  `totalWin` bigint(20) NULL DEFAULT NULL COMMENT '中奖赢取',
  `info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '中奖详情',
  `type` int(255) NULL DEFAULT NULL COMMENT '类型',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 120554 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_osee_gobang_record_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_osee_gobang_record_log`;
CREATE TABLE `tbl_osee_gobang_record_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `money` bigint(20) NULL DEFAULT NULL COMMENT '学费',
  `winner_id` bigint(20) NULL DEFAULT NULL COMMENT '获胜者id',
  `winner_nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '获胜者昵称',
  `winner_before_money` bigint(20) NULL DEFAULT NULL COMMENT '获胜前金币',
  `winner_after_money` bigint(20) NULL DEFAULT NULL COMMENT '获胜后金币',
  `loser_id` bigint(20) NULL DEFAULT NULL COMMENT '失败者id',
  `loser_nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '失败者昵称',
  `loser_before_money` bigint(20) NULL DEFAULT NULL COMMENT '失败前金币',
  `loser_after_money` bigint(20) NULL DEFAULT NULL COMMENT '失败后金币',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_osee_lottery_draw_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_osee_lottery_draw_log`;
CREATE TABLE `tbl_osee_lottery_draw_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `player_id` bigint(20) NOT NULL COMMENT '玩家id',
  `item_id` int(10) NOT NULL COMMENT '物品id',
  `item_num` bigint(20) NOT NULL COMMENT '物品数量',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100211 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_osee_player_tenure_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_osee_player_tenure_log`;
CREATE TABLE `tbl_osee_player_tenure_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '玩家id',
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `reason` int(11) NOT NULL COMMENT '变动来源',
  `pre_diamond` bigint(20) NULL DEFAULT NULL COMMENT '变动前钻石',
  `change_diamond` bigint(20) NULL DEFAULT NULL COMMENT '变动钻石',
  `pre_money` bigint(20) NULL DEFAULT NULL COMMENT '变动前金币',
  `change_money` bigint(20) NULL DEFAULT NULL COMMENT '变动金币',
  `pre_lottery` bigint(20) NULL DEFAULT NULL COMMENT '变动前奖券',
  `change_lottery` bigint(20) NULL DEFAULT NULL COMMENT '变动奖券',
  `pre_bank_money` bigint(20) NULL DEFAULT NULL COMMENT '变动前保险箱金币',
  `change_bank_money` bigint(20) NULL DEFAULT NULL COMMENT '变动保险箱金币',
  `pre_bronze_torpedo` bigint(20) NULL DEFAULT NULL COMMENT '变动前青铜鱼雷',
  `change_bronze_torpedo` bigint(20) NULL DEFAULT NULL COMMENT '变动的青铜鱼雷数量',
  `pre_silver_torpedo` bigint(20) NULL DEFAULT NULL COMMENT '变动前白银鱼雷',
  `change_silver_torpedo` bigint(20) NULL DEFAULT NULL COMMENT '变动的白银鱼雷数量',
  `pre_gold_torpedo` bigint(20) NULL DEFAULT NULL COMMENT '变动前黄金鱼雷',
  `change_gold_torpedo` bigint(20) NULL DEFAULT NULL COMMENT '变动的黄金鱼雷数量',
  `pre_skill_lock` bigint(20) NULL DEFAULT NULL COMMENT '变动前锁定技能数量',
  `change_skill_lock` bigint(20) NULL DEFAULT NULL COMMENT '变动的锁定技能数量',
  `pre_skill_frozen` bigint(20) NULL DEFAULT NULL COMMENT '变动前冰冻技能数量',
  `change_skill_frozen` bigint(20) NULL DEFAULT NULL COMMENT '变动的冰冻技能数量',
  `pre_skill_fast` bigint(20) NULL DEFAULT NULL COMMENT '变动前急速技能数量',
  `change_skill_fast` bigint(20) NULL DEFAULT NULL COMMENT '变动的急速技能数量',
  `pre_skill_crit` bigint(20) NULL DEFAULT NULL COMMENT '变动前暴击技能数量',
  `change_skill_crit` bigint(20) NULL DEFAULT NULL COMMENT '变动的暴击技能数量',
  `pre_boss_bugle` bigint(20) NULL DEFAULT NULL COMMENT '变动前boss号角数量',
  `change_boss_bugle` bigint(20) NULL DEFAULT NULL COMMENT '变动的boss号角数量',
  `pre_dragon_crystal` bigint(20) NULL DEFAULT NULL COMMENT '变动前龙晶数量',
  `change_dragon_crystal` bigint(20) NULL DEFAULT NULL COMMENT '变动龙晶',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 243331 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_osee_real_lottery_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_osee_real_lottery_log`;
CREATE TABLE `tbl_osee_real_lottery_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `order_num` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '订单号',
  `user_id` bigint(20) NOT NULL COMMENT '兑换人id',
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `reward_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品名',
  `count` int(11) NOT NULL COMMENT '兑换数量',
  `cost` int(11) NOT NULL COMMENT '消耗数量',
  `creator` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `consignee` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '收货人',
  `phone_num` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `address` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '收货地址',
  `order_state` int(11) NOT NULL COMMENT '状态',
  `stock_id` bigint(20) NULL DEFAULT NULL COMMENT '对应兑换的库存物品ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_osee_recharge_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_osee_recharge_log`;
CREATE TABLE `tbl_osee_recharge_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `order_num` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '订单号',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '玩家id',
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `pay_money` int(11) NOT NULL COMMENT '支付金额',
  `shop_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品名',
  `shop_type` int(11) NOT NULL COMMENT '类型',
  `count` int(11) NOT NULL COMMENT '数量',
  `creator` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建数量',
  `recharge_type` int(11) NOT NULL COMMENT '充值方式',
  `order_state` int(11) NOT NULL COMMENT '订单状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100720 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_osee_two_eight_record_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_osee_two_eight_record_log`;
CREATE TABLE `tbl_osee_two_eight_record_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `money` bigint(20) NULL DEFAULT NULL COMMENT '账户金币变动数额',
  `playerId` bigint(20) NULL DEFAULT NULL COMMENT '玩家id',
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '玩家昵称',
  `playBeforeMoney` bigint(20) NULL DEFAULT NULL COMMENT '游戏前剩余金币',
  `playAfterMoney` bigint(20) NULL DEFAULT NULL COMMENT '游戏后剩余金币',
  `input` bigint(20) NOT NULL COMMENT '下注金额',
  `cardType` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '牌型',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_osee_unreal_lottery_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_osee_unreal_lottery_log`;
CREATE TABLE `tbl_osee_unreal_lottery_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `order_num` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '订单号',
  `user_id` bigint(20) NOT NULL COMMENT '兑换人id',
  `nickname` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `reward_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '商品名',
  `type` int(11) NOT NULL COMMENT '类型',
  `count` int(11) NOT NULL COMMENT '兑换数量',
  `item_id` int(11) NOT NULL COMMENT '消耗类型',
  `cost` int(11) NOT NULL COMMENT '消耗数量',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_ttmy_agent_commission_info_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_ttmy_agent_commission_info_log`;
CREATE TABLE `tbl_ttmy_agent_commission_info_log`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `player_id` bigint(20) NOT NULL COMMENT '充值玩家ID',
  `player_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '充值玩家昵称',
  `shop_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '商品名',
  `channel_id` bigint(20) NULL DEFAULT NULL COMMENT '渠道ID',
  `promoter_id` bigint(20) NULL DEFAULT NULL COMMENT '推广员ID',
  `commission` decimal(8, 4) NULL DEFAULT NULL COMMENT '计算出的渠道商佣金数量(取整)',
  `sec_commission` decimal(8, 4) NULL DEFAULT NULL COMMENT '计算出的推广员佣金数量(取整)',
  `money` decimal(8, 4) NULL DEFAULT NULL COMMENT '充值金额',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '数据创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100088 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_ttmy_agent_commission_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_ttmy_agent_commission_log`;
CREATE TABLE `tbl_ttmy_agent_commission_log`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `player_id` bigint(20) NOT NULL COMMENT '充值玩家ID',
  `player_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '充值玩家昵称',
  `agent_player_id` bigint(20) NULL DEFAULT NULL COMMENT '玩家上级代理ID(不一定是直属)',
  `agent_player_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '玩家上级代理昵称(不一定是直属)',
  `commission_rate` decimal(8, 4) NULL DEFAULT 0.0500 COMMENT '计算的佣金比例',
  `commission` decimal(8, 4) NULL DEFAULT NULL COMMENT '计算出的佣金数量(取整)',
  `money` bigint(20) NULL DEFAULT NULL COMMENT '充值金额',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '数据创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100152 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_ttmy_agent_withdraw_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_ttmy_agent_withdraw_log`;
CREATE TABLE `tbl_ttmy_agent_withdraw_log`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `agent_id` bigint(20) NOT NULL COMMENT '代理ID',
  `bank` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '银行',
  `real_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '户名',
  `bank_num` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '卡号',
  `open_bank` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '开户行',
  `money` bigint(20) NULL DEFAULT NULL COMMENT '提现金额',
  `state` int(2) NULL DEFAULT NULL COMMENT '状态',
  `creator` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '操作管理员',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_ttmy_commission_exchange_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_ttmy_commission_exchange_log`;
CREATE TABLE `tbl_ttmy_commission_exchange_log`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `agent_id` bigint(20) NOT NULL COMMENT '代理玩家ID',
  `agent_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '代理玩家昵称',
  `bronze_torpedo_num` bigint(20) NULL DEFAULT NULL COMMENT '青铜鱼雷数量',
  `silver_torpedo_num` bigint(20) NULL DEFAULT NULL COMMENT '白银鱼雷数量',
  `gold_torpedo_num` bigint(20) NULL DEFAULT NULL COMMENT '黄金鱼雷数量',
  `gold_num` bigint(20) NULL DEFAULT NULL COMMENT '金币数量',
  `cost_commission` bigint(20) NULL DEFAULT NULL COMMENT '花费的佣金',
  `rest_commission` bigint(20) NULL DEFAULT NULL COMMENT '该次兑换后剩余的佣金',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '数据创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_ttmy_give_gift_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_ttmy_give_gift_log`;
CREATE TABLE `tbl_ttmy_give_gift_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `from_id` bigint(20) NULL DEFAULT NULL COMMENT '赠送人ID',
  `from_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '赠送人昵称',
  `to_id` bigint(20) NULL DEFAULT NULL COMMENT '被赠送人ID',
  `to_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '被赠送人昵称',
  `gift_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '赠送礼物名称',
  `gift_num` bigint(20) NULL DEFAULT NULL COMMENT '赠送礼物数量',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '数据创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100201 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for tbl_ttmy_ten_challenge_ranking_log
-- ----------------------------
DROP TABLE IF EXISTS `tbl_ttmy_ten_challenge_ranking_log`;
CREATE TABLE `tbl_ttmy_ten_challenge_ranking_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户id',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `head_index` int(11) NULL DEFAULT NULL COMMENT '头像序号',
  `head_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像地址',
  `score` bigint(20) NOT NULL COMMENT '得分',
  `update_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '数据更新时间',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '数据创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
