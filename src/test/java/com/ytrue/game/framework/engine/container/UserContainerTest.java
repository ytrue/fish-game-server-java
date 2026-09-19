package com.ytrue.game.framework.engine.container;

import com.ytrue.game.framework.database.data.entity.UserEntity;
import com.ytrue.game.framework.engine.data.ServerUser;

/**
 * 用户容器索引测试。
 *
 * <p>不依赖 JUnit，直接跑 {@code main} 即可（IDE 里点绿色箭头）：</p>
 *
 * <p>重点覆盖两类回归：</p>
 * <ol>
 *     <li><b>覆盖</b>——未登录用户没有实体，{@code getId()} 恒为 0、openid/unionid/username
 *         恒为空串。若照写索引，所有连接会挤在同几个键上互相覆盖；</li>
 *     <li><b>NPE</b>——已登录用户也可能缺某一项（账号密码用户没有 openid），
 *         而 {@code ConcurrentHashMap} 不允许 null 键，直接写入会抛异常。</li>
 * </ol>
 *
 * @since 1.0.0
 */
public class UserContainerTest {

    /** 通过数。 */
    static int pass = 0;

    /** 失败数。 */
    static int fail = 0;

    public static void main(String[] args) {
        section("1. 未登录连接：只有连接维度可索引");
        String c1 = "conn-A";
        String c2 = "conn-B";
        String c3 = "conn-C";
        ServerUser u1 = preLogin(c1);
        ServerUser u2 = preLogin(c2);
        ServerUser u3 = preLogin(c3);
        UserContainer.putServerUser(u1);
        UserContainer.putServerUser(u2);
        UserContainer.putServerUser(u3);

        check("三个连接都登记成功", UserContainer.getActiveUserCount(), 3);
        check("  按连接能查到第一个", UserContainer.getUserByConnect(c1), u1);
        check("  按连接能查到第二个", UserContainer.getUserByConnect(c2), u2);
        check("  按连接能查到第三个", UserContainer.getUserByConnect(c3), u3);

        // 核心回归点：未登录用户不得占用 id=0 / 空串 这些键
        check("未登录用户不占用 id=0 索引", UserContainer.getActiveUserById(0), null);
        check("未登录用户不占用空 unionid 索引", UserContainer.getUserByUnionid(""), null);
        check("未登录用户不占用空用户名索引", UserContainer.getUserByUsername(""), null);
        check("不存在的连接查不到", UserContainer.getUserByConnect("no-such"), null);

        section("2. 移除未登录连接");
        UserContainer.removeServerUser(u2);
        check("数量减一", UserContainer.getActiveUserCount(), 2);
        check("  被移除的查不到", UserContainer.getUserByConnect(c2), null);
        check("  其他两个不受影响（1）", UserContainer.getUserByConnect(c1), u1);
        check("  其他两个不受影响（2）", UserContainer.getUserByConnect(c3), u3);

        section("3. 账号密码用户：openid/unionid/username 全为 null（不得抛 NPE）");
        ServerUser pwdUser = loggedIn(200001L, "pwd-user", null, null, null);
        try {
            UserContainer.putServerUser(pwdUser);
            ok("putServerUser 未抛异常");
        } catch (Exception e) {
            bad("putServerUser 抛了异常", e.getClass().getName() + ": " + e.getMessage());
        }
        check("  按 id 能查到", UserContainer.getActiveUserById(200001L), pwdUser);
        check("  按用户名能查到", UserContainer.getUserByUsername("pwd-user"), pwdUser);
        check("  null 键未污染空 unionid 索引", UserContainer.getUserByUnionid(""), null);

        section("4. 微信用户：有 openid，无 unionid / username（不得抛 NPE）");
        ServerUser wxUser = loggedIn(200002L, null, "wx-openid-1", null, null);
        try {
            UserContainer.putServerUser(wxUser);
            ok("putServerUser 未抛异常");
        } catch (Exception e) {
            bad("putServerUser 抛了异常", e.getClass().getName() + ": " + e.getMessage());
        }
        check("  按 id 能查到", UserContainer.getActiveUserById(200002L), wxUser);

        section("5. 完整用户：四个维度都可查");
        ServerUser full = loggedIn(200003L, "full-user", "openid-3", "unionid-3", null);
        UserContainer.putServerUser(full);
        check("按 id", UserContainer.getActiveUserById(200003L), full);
        check("按 unionid", UserContainer.getUserByUnionid("unionid-3"), full);
        check("按用户名", UserContainer.getUserByUsername("full-user"), full);

        section("6. 移除已登录用户：不误删他人");
        UserContainer.removeServerUser(full);
        check("自身已移除", UserContainer.getActiveUserById(200003L), null);
        check("  同批次的密码用户不受影响", UserContainer.getActiveUserById(200001L), pwdUser);
        check("  同批次的微信用户不受影响", UserContainer.getActiveUserById(200002L), wxUser);

        section("7. 重复 put 同一用户不产生重复条目");
        int before = UserContainer.getActiveUserCount();
        UserContainer.putServerUser(pwdUser);
        UserContainer.putServerUser(pwdUser);
        check("连接表数量不变", UserContainer.getActiveUserCount(), before);

        System.out.printf("%n========== 通过 %d / 失败 %d ==========%n", pass, fail);
        System.exit(fail > 0 ? 1 : 0);
    }

    // ==================== 构造测试数据 ====================

    /**
     * 构造一个「刚连上、还没登录」的用户：只有连接标识，没有实体。
     *
     * @param connect 连接标识
     * @return 未登录用户
     */
    static ServerUser preLogin(String connect) {
        ServerUser user = new ServerUser();
        user.setConnect(connect);
        user.setLastActiveTime(System.currentTimeMillis());
        return user;
    }

    /**
     * 构造一个「已登录」的用户：带实体，各身份字段按传入值设置。
     *
     * @param id       用户 id
     * @param username 用户名（可为 {@code null}）
     * @param openid   openid（可为 {@code null}）
     * @param unionid  unionid（可为 {@code null}）
     * @param connect  连接标识，为 {@code null} 时自动生成
     * @return 已登录用户
     */
    static ServerUser loggedIn(long id, String username, String openid, String unionid, String connect) {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setUsername(username);
        entity.setOpenid(openid);
        entity.setUnionid(unionid);

        ServerUser user = new ServerUser();
        user.setEntity(entity);
        user.setConnect(connect != null ? connect : "conn-" + id);
        return user;
    }

    // ==================== 断言与工具 ====================

    /** 打印分段标题。 */
    static void section(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    /** 断言相等。 */
    static void check(String what, Object actual, Object expected) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            pass++;
            System.out.printf("  [通过] %-32s = %s%n", what, actual == null ? "null" : "OK");
        } else {
            fail++;
            System.out.printf("  [失败] %-32s 期望=%s 实际=%s%n",
                    what, expected == null ? "null" : "OK", actual == null ? "null" : "OK");
        }
    }

    /** 记一次通过。 */
    static void ok(String msg) {
        pass++;
        System.out.println("  [通过] " + msg);
    }

    /** 记一次失败。 */
    static void bad(String what, String detail) {
        fail++;
        System.out.println("  [失败] " + what + " -> " + detail);
    }

}
