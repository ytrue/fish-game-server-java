/*
 Navicat Premium Data Transfer

 Source Server         : MySQL_本地连接
 Source Server Type    : MySQL
 Source Server Version : 80015
 Source Host           : localhost:3306
 Source Schema         : app_data

 Target Server Type    : MySQL
 Target Server Version : 80015
 File Encoding         : 65001

 Date: 16/04/2020 00:24:04
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for fruit_laba_reward_info
-- ----------------------------
DROP TABLE IF EXISTS `fruit_laba_reward_info`;
CREATE TABLE `fruit_laba_reward_info`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '奖励id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '用户id',
  `achieve_num` int(11) NULL DEFAULT NULL COMMENT '旋转次数',
  `reward_gold` int(11) NULL DEFAULT NULL COMMENT '奖励金币',
  `reward_lottery` int(11) NULL DEFAULT NULL COMMENT '奖励点券',
  `weather_receive` tinyint(1) NULL DEFAULT NULL COMMENT '是否领取',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for app_rate_form
-- ----------------------------
DROP TABLE IF EXISTS `app_rate_form`;
CREATE TABLE `app_rate_form`  (
  `id` bigint(10) NOT NULL AUTO_INCREMENT,
  `day_time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `rate` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00' COMMENT '付费率',
  `arppu` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00' COMMENT 'arppu',
  `arpu` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00' COMMENT 'arpu',
  `new_rate` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00' COMMENT '新增付费率',
  `new_arppu` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00' COMMENT '新增arppu',
  `new_arpu` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00' COMMENT '新增arpu',
  `new_pay_num` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '新增付费人数',
  `login_num` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '登陆人数',
  `pay_num` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '付费人数',
  `all_money` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '总金额',
  `agent_id` bigint(10) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 96 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for app_report_form
-- ----------------------------
DROP TABLE IF EXISTS `app_report_form`;
CREATE TABLE `app_report_form`  (
  `id` bigint(10) NOT NULL AUTO_INCREMENT,
  `day_time` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `day1` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day2` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day3` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day4` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day5` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day6` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day7` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day8` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day9` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day10` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day11` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day12` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day13` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day14` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day15` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day16` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day17` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day18` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day19` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day20` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day21` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day22` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day23` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day24` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day25` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day26` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day27` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day28` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day29` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `day30` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0.00',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型：register（注册）  pay(付费）',
  `agent_id` bigint(10) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 899 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for app_user
-- ----------------------------
DROP TABLE IF EXISTS `app_user`;
CREATE TABLE `app_user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '注册时间',
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `phonenum` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `openid` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '第三方平台id',
  `unionid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '第三方平台唯一id',
  `password` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '密码(32位md5)',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '昵称',
  `head_index` int(11) NULL DEFAULT NULL COMMENT '头像序号',
  `head_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像地址',
  `sex` int(1) NULL DEFAULT NULL COMMENT '性别',
  `my_invite_code` int(11) NULL DEFAULT NULL COMMENT '我的邀请码',
  `invite_code` int(11) NULL DEFAULT NULL COMMENT '我绑定的邀请码',
  `user_state` int(255) NULL DEFAULT 0 COMMENT '用户状态',
  `online_state` int(255) NULL DEFAULT 0 COMMENT '在线状态',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 183040 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for app_user_authentication
-- ----------------------------
DROP TABLE IF EXISTS `app_user_authentication`;
CREATE TABLE `app_user_authentication`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '认证id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '认证时间',
  `user_id` bigint(20) NOT NULL COMMENT '用户id',
  `name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `idcard_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '身份证号码',
  `phone_no` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '手机号码',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100003 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for app_wander_subtitle
-- ----------------------------
DROP TABLE IF EXISTS `app_wander_subtitle`;
CREATE TABLE `app_wander_subtitle`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '游走字幕id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `content` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '字幕内容',
  `interval_time` int(11) NULL DEFAULT NULL COMMENT '间隔时间',
  `start_time` timestamp(0) NULL DEFAULT NULL COMMENT '生效时间',
  `end_time` timestamp(0) NULL DEFAULT NULL COMMENT '失效时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100005 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for osee_cdk
-- ----------------------------
DROP TABLE IF EXISTS `osee_cdk`;
CREATE TABLE `osee_cdk`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'cdk id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `cdkey` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'cdkey内容',
  `type_id` bigint(20) NOT NULL COMMENT '类型id',
  `rewards` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '奖励',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '兑换人id',
  `nickname` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '兑换人昵称',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for osee_cdk_type
-- ----------------------------
DROP TABLE IF EXISTS `osee_cdk_type`;
CREATE TABLE `osee_cdk_type`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '类型id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型名',
  `start_with` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '开头字符',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for osee_lottery_shop
-- ----------------------------
DROP TABLE IF EXISTS `osee_lottery_shop`;
CREATE TABLE `osee_lottery_shop`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '类型id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `index` int(11) NULL DEFAULT NULL COMMENT '序号',
  `type` int(11) NULL DEFAULT NULL COMMENT '类型',
  `count` bigint(20) NOT NULL COMMENT '物品数量',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '名称',
  `cost` bigint(20) NOT NULL COMMENT '消耗奖券数量',
  `img` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图片',
  `size` int(11) NULL DEFAULT NULL COMMENT '总数量',
  `used_size` int(11) NULL DEFAULT NULL COMMENT '已兑换数量',
  `send_type` int(11) NULL DEFAULT NULL COMMENT '发货类型',
  `stock` int(11) NULL DEFAULT NULL COMMENT '库存',
  `refresh_type` int(11) NULL DEFAULT NULL COMMENT '刷新类型',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100011 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for osee_lottery_shop_stock
-- ----------------------------
DROP TABLE IF EXISTS `osee_lottery_shop_stock`;
CREATE TABLE `osee_lottery_shop_stock`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `shop_id` bigint(20) NOT NULL COMMENT '道具库存属于的商品ID',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '兑换该卡的玩家ID',
  `number` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '卡号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '卡密',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '数据创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for osee_notice
-- ----------------------------
DROP TABLE IF EXISTS `osee_notice`;
CREATE TABLE `osee_notice`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '类型id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `index` int(11) NULL DEFAULT NULL COMMENT '序号',
  `title` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标题',
  `content` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '内容',
  `start_time` timestamp(0) NULL DEFAULT NULL COMMENT '生效时间',
  `end_time` timestamp(0) NULL DEFAULT NULL COMMENT '失效时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for osee_player
-- ----------------------------
DROP TABLE IF EXISTS `osee_player`;
CREATE TABLE `osee_player`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '玩家id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `user_id` bigint(20) NULL DEFAULT NULL COMMENT '用户id',
  `money` bigint(20) NULL DEFAULT NULL COMMENT '玩家金币',
  `bank_money` bigint(20) NULL DEFAULT NULL COMMENT '保险箱金币',
  `bank_password` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '保险箱密码',
  `lottery` bigint(20) NULL DEFAULT NULL COMMENT '奖券',
  `diamond` bigint(20) NULL DEFAULT NULL COMMENT '钻石',
  `vip_level` int(11) NULL DEFAULT NULL COMMENT 'vip等级',
  `level` int(11) NULL DEFAULT NULL COMMENT '玩家等级',
  `experience` bigint(20) NULL DEFAULT NULL COMMENT '玩家经验',
  `recharge_money` bigint(20) NULL DEFAULT NULL COMMENT '充值金额',
  `lose_control` int(11) NULL DEFAULT NULL COMMENT '必输控制',
  `player_type` int(11) NULL DEFAULT NULL COMMENT '玩家类型',
  `bronze_torpedo` bigint(20) NULL DEFAULT NULL COMMENT '玩家青铜鱼雷数量',
  `silver_torpedo` bigint(20) NULL DEFAULT NULL COMMENT '玩家白银鱼雷数量',
  `gold_torpedo` bigint(20) NULL DEFAULT NULL COMMENT '玩家黄金鱼雷数量',
  `skill_lock` bigint(20) NULL DEFAULT NULL COMMENT '玩家锁定技能数量',
  `skill_frozen` bigint(20) NULL DEFAULT NULL COMMENT '玩家冰冻技能数量',
  `skill_fast` bigint(20) NULL DEFAULT NULL COMMENT '玩家急速技能数量',
  `skill_crit` bigint(20) NULL DEFAULT NULL COMMENT '玩家暴击技能数量',
  `boss_bugle` bigint(20) NULL DEFAULT NULL COMMENT '玩家BOSS号角数量',
  `battery_level` int(11) NULL DEFAULT NULL COMMENT '玩家目前拥有的最高炮台等级',
  `monthcard_expire_date` date NULL DEFAULT NULL COMMENT '玩家月卡到期时间',
  `ten_challenge_times` bigint(20) NULL DEFAULT NULL COMMENT '玩家拼十剩余挑战次数',
  `qszs_battery_expire_date` date NULL DEFAULT NULL COMMENT '骑士之誓炮台外观到期时间',
  `blnh_battery_expire_date` date NULL DEFAULT NULL COMMENT '冰龙怒吼炮台外观到期时间',
  `lhtz_battery_expire_date` date NULL DEFAULT NULL COMMENT '莲花童子炮台外观到期时间',
  `swhp_battery_expire_date` date NULL DEFAULT NULL COMMENT '死亡火炮炮台外观到期时间',
  `dragon_crystal` bigint(20) NULL DEFAULT NULL COMMENT '玩家拥有的龙晶数量',
  `fen_shen` bigint(20) NULL DEFAULT NULL COMMENT '玩家拥有的分身炮道具数量',
  `send_gift` bigint(20) NOT NULL DEFAULT 0 COMMENT '赠送功能：0：正常、1：限制  默认为正常状态',
  `gold_torpedo_bang` bigint(20) NULL DEFAULT NULL COMMENT '玩家非绑定黄金鱼雷数量',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 194537 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for shopping
-- ----------------------------
DROP TABLE IF EXISTS `shopping`;
CREATE TABLE `shopping`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `player_id` bigint(20) NULL DEFAULT NULL COMMENT '用户id',
  `daily_bag_info` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '每日礼包',
  `once_bag_info` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '特惠礼包',
  `money_card` tinyint(1) NULL DEFAULT NULL COMMENT '金币卡',
  `last_receive` timestamp(0) NULL DEFAULT '2010-01-01 00:00:00' COMMENT '最后领取时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100109 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ttmy_address
-- ----------------------------
DROP TABLE IF EXISTS `ttmy_address`;
CREATE TABLE `ttmy_address`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `player_id` bigint(20) NOT NULL COMMENT '玩家ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '玩家昵称',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '玩家手机号码',
  `address` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '玩家收货地址',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '数据创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ttmy_agent
-- ----------------------------
DROP TABLE IF EXISTS `ttmy_agent`;
CREATE TABLE `ttmy_agent`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `player_id` bigint(20) NOT NULL COMMENT '玩家ID',
  `player_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '玩家昵称',
  `agent_level` int(11) NOT NULL COMMENT '代理等级',
  `agent_player_id` bigint(20) NULL DEFAULT NULL COMMENT '上级代理玩家ID',
  `upper_player_id` bigint(20) NULL DEFAULT NULL COMMENT '上级玩家ID',
  `first_commission_rate` decimal(8, 4) NULL DEFAULT 0.0500 COMMENT '一级代理玩家佣金比例',
  `second_commission_rate` decimal(8, 4) NULL DEFAULT 0.0500 COMMENT '二级代理玩家佣金比例',
  `total_commission` decimal(8, 4) NULL DEFAULT NULL COMMENT '赚取的总佣金',
  `total_active_money` bigint(20) NOT NULL DEFAULT 0 COMMENT '赚取的总活跃金币',
  `total_active_dragon_crystal` bigint(20) NOT NULL DEFAULT 0 COMMENT '赚取的总活跃龙晶',
  `invite_qrcode_img` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '代理邀请二维码图片',
  `invite_url` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '代理邀请链接',
  `state` int(11) NULL DEFAULT NULL COMMENT '代理身份状态',
  `bank` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '银行',
  `real_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '户名',
  `bank_num` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '卡号',
  `open_bank` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '开户行',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '数据创建时间',
  `first_pay_rate` decimal(8, 0) NULL DEFAULT NULL COMMENT '官方支付分发比例',
  `other_pay_rate` decimal(8, 0) NULL DEFAULT NULL COMMENT '分发支付比例',
  `pay_way` int(10) NULL DEFAULT NULL COMMENT '支付方式',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `player_id_index`(`player_id`) USING BTREE COMMENT '唯一用户id'
) ENGINE = InnoDB AUTO_INCREMENT = 100651 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ttmy_message
-- ----------------------------
DROP TABLE IF EXISTS `ttmy_message`;
CREATE TABLE `ttmy_message`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '内容',
  `read` tinyint(1) NULL DEFAULT NULL COMMENT '是否已读',
  `receive` tinyint(1) NULL DEFAULT NULL COMMENT '是否已接收附件',
  `from_id` bigint(20) NULL DEFAULT NULL COMMENT '发件人',
  `to_id` bigint(20) NULL DEFAULT NULL COMMENT '收件人',
  `items_json` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '附件信息',
  `state` int(11) NOT NULL COMMENT '数据状态 0-正常 1-删除',
  `create_time` timestamp(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '数据创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 101280 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
