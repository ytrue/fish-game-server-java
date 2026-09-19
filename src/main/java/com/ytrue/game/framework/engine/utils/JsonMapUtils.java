package com.ytrue.game.framework.engine.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Map 与对象互转工具类。
 *
 * <p>主要用于后台 GM 接口：GM 请求以 {@code Map<String, Object>} 形式传入、以 JSON 字符串返回，
 * 本类负责在「对象」与「Map」之间搬运数据，以及从参数 Map 中按指定类型取值。</p>
 *
 * @since 1.0.0
 */
@Slf4j
public final class JsonMapUtils {

    /**
     * 私有构造器，禁止实例化（纯工具类）。
     */
    private JsonMapUtils() {
    }

    /**
     * 将对象的字段反射转成 Map。
     *
     * <p>会沿继承链向上收集字段（含父类），{@link Date} 类型统一转成毫秒时间戳，
     * 便于序列化成 JSON 后仍能被前端正确解析（ISO 字符串各语言处理不一致）。</p>
     *
     * @param obj 待转换的对象
     * @return 字段名 → 字段值 的映射
     * @throws Exception 反射访问失败时抛出
     */
    public static Map<String, Object> objectToMap(Object obj) throws Exception {
        Map<String, Object> objMap = new HashMap<>();

        // 从实际类型一路向上收集到 Object 之前，保证父类字段也不遗漏
        Class<?> clazz = obj.getClass();
        while (clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    if (field.getType() == Date.class) {
                        // 日期转毫秒时间戳
                        Date value = (Date) field.get(obj);
                        objMap.put(field.getName(), value == null ? null : value.getTime());
                    } else {
                        objMap.put(field.getName(), field.get(obj));
                    }
                } catch (Exception e) {
                    // 单个字段读取失败（如静态字段、合成字段）不影响其它字段
                    log.error("转换对象字段时出现异常:[{}]", e.getMessage(), e);
                }
            }
            clazz = clazz.getSuperclass();
        }

        return objMap;
    }

    /**
     * 将对象列表批量转成 Map 列表。
     *
     * @param objs 待转换的对象列表
     * @return Map 列表
     * @throws Exception 反射访问失败时抛出
     */
    public static List<Map<String, Object>> objectsToMaps(List<?> objs) throws Exception {
        List<Map<String, Object>> objMaps = new LinkedList<>();
        for (Object obj : objs) {
            objMaps.add(objectToMap(obj));
        }
        return objMaps;
    }

    /**
     * 从参数 Map 中按键取值，并转换成指定类型。
     *
     * <p>存在的意义：JSON 反序列化成 {@code Map<String, Object>} 后，<b>所有数字都会变成
     * {@link Double}</b>（Gson 的默认行为）。直接 {@code (Long) param.get("id")} 会抛
     * {@link ClassCastException}，必须经由本方法按目标类型转换。</p>
     *
     * <p>例：前端传来 {@code {"pingTime": 1234567890123}}，取到的原始值是
     * {@code Double 1.234567890123E12}；用 {@code TYPE_LONG} 转换后才得到
     * {@code Long 1234567890123}。</p>
     *
     * <p>转换失败（如字符串无法解析成数字）时返回 {@code null}，不抛异常。</p>
     *
     * @param param 参数 Map
     * @param key   参数名
     * @param type  目标类型
     * @param <T>   目标类型泛型
     * @return 转换后的值；键不存在或转换失败时返回 {@code null}
     * @throws Exception 保留声明以便调用方统一处理（本方法内部已捕获转换异常）
     */
    @SuppressWarnings("unchecked")
    public static <T> T parseObject(Map<String, Object> param, String key, JsonInnerType type) throws Exception {
        Object obj = param.get(key);

        try {
            if (obj instanceof Double dObj) {
                // Gson 把 JSON 数字统一解析成 Double，按目标类型换算
                return switch (type) {
                    case TYPE_DATE -> (T) new Date(Math.round(dObj));
                    case TYPE_LONG -> (T) Long.valueOf(Math.round(dObj));
                    case TYPE_INT -> (T) Integer.valueOf((int) Math.round(dObj));
                    case TYPE_STRING -> (T) Double.toString(dObj);
                    // 其余类型（含 TYPE_DOUBLE / TYPE_FLOAT）保持原值返回
                    default -> (T) obj;
                };
            } else if (obj instanceof String sObj) {
                // 前端也常把数字写成字符串形式，同样按目标类型解析
                return switch (type) {
                    case TYPE_DATE -> (T) new Date(Long.parseLong(sObj));
                    case TYPE_DOUBLE -> (T) Double.valueOf(sObj);
                    case TYPE_FLOAT -> (T) Float.valueOf(sObj);
                    case TYPE_INT -> (T) Integer.valueOf(sObj);
                    case TYPE_LONG -> (T) Long.valueOf(sObj);
                    // 字符串与字符串数组保持原值
                    default -> (T) obj;
                };
            }
            // 其它类型（含 null）原样返回
            return (T) obj;
        } catch (Exception e) {
            log.info("转换类型出错，原值:[{}][{}]", obj == null ? "null" : obj.getClass().getSimpleName(), obj);
        }
        return null;
    }

    /**
     * 取值时可选的目标类型。
     *
     * @since 1.0.0
     */
    public enum JsonInnerType {

        /** 整型。 */
        TYPE_INT,

        /** 长整型。 */
        TYPE_LONG,

        /** 字符串。 */
        TYPE_STRING,

        /** 字符串数组。 */
        TYPE_STRING_ARRAY,

        /** 单精度浮点型。 */
        TYPE_FLOAT,

        /** 双精度浮点型。 */
        TYPE_DOUBLE,

        /** 日期型（值为毫秒时间戳）。 */
        TYPE_DATE

    }

}
