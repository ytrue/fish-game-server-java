package com.ytrue.game.framework.engine.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.LongSerializationPolicy;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * JSON 工具类（基于 Gson）。
 *
 * <p>持有全局共享的、经过定制配置的 {@link Gson} 实例，并对常用的序列化 / 反序列化操作做
 * 静态封装，供服务端各处统一使用，无需关心底层 {@link Gson} 对象的创建与配置。</p>
 *
 * <p>定制规则：</p>
 * <ul>
 *     <li>为 {@link Integer} / {@code int} 注册 {@link IntegerAdapter}，空串与 {@code "null"} 兜底为 {@code 0}；</li>
 *     <li>为 {@link Long} / {@code long} 注册 {@link LongAdapter}，空串与 {@code "null"} 兜底为 {@code 0L}；</li>
 *     <li>{@code long} 序列化采用字符串形式（{@link LongSerializationPolicy#STRING}），
 *         避免超出 JavaScript 安全整数范围（{@code 2^53}）导致精度丢失。</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class GsonUtils {

    /**
     * 全局共享的 Gson 实例（单例）。
     *
     * <p>线程安全，可在多线程环境（网络收发、定时任务等）中并发复用，无需重复创建。</p>
     */
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Integer.class, new IntegerAdapter())
            .registerTypeAdapter(int.class, new IntegerAdapter())
            .registerTypeAdapter(Long.class, new LongAdapter())
            .registerTypeAdapter(long.class, new LongAdapter())
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .create();

    /**
     * 带缩进格式化的 Gson 实例（与 {@link #GSON} 配置一致，仅额外开启 pretty printing）。
     *
     * <p>仅用于需要人类可读 JSON 的场景（如日志、调试输出），日常序列化仍用 {@link #GSON}。</p>
     */
    private static final Gson PRETTY_GSON = GSON.newBuilder().setPrettyPrinting().create();

    /**
     * 私有构造器，禁止实例化（纯工具类）。
     */
    private GsonUtils() {
    }

    /**
     * 将对象序列化为 JSON 字符串。
     *
     * @param obj 待序列化的对象
     * @return JSON 字符串；若 {@code obj} 为 {@code null} 则返回 {@code "null"}
     */
    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }

    /**
     * 将 JSON 字符串反序列化为指定类型对象。
     *
     * @param json  JSON 字符串
     * @param clazz 目标类型
     * @param <T>   目标类型泛型
     * @return 反序列化结果
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    /**
     * 将 JSON 字符串反序列化为指定（泛型）类型对象。
     *
     * @param json JSON 字符串
     * @param type 目标类型（含泛型信息，配合 {@link com.google.gson.reflect.TypeToken} 使用）
     * @param <T>  目标类型泛型
     * @return 反序列化结果
     */
    public static <T> T fromJson(String json, Type type) {
        return GSON.fromJson(json, type);
    }

    /**
     * 获取全局共享的 Gson 实例。
     *
     * <p>供需要直接使用 {@link Gson} 底层能力（如自定义 {@code TypeAdapter} 场景）的调用方访问。</p>
     *
     * @return 配置好的、线程安全的 Gson 实例
     */
    public static Gson gson() {
        return GSON;
    }

    /**
     * 将 JSON 字符串反序列化为 {@link List}。
     *
     * @param json        JSON 字符串
     * @param elementType 列表元素类型
     * @param <T>         列表元素类型泛型
     * @return 反序列化后的列表；若 {@code json} 为空或 {@code "null"} 则返回 {@code null}
     */
    public static <T> List<T> fromJsonToList(String json, Class<T> elementType) {
        Type listType = TypeToken.getParameterized(List.class, elementType).getType();
        return GSON.fromJson(json, listType);
    }

    /**
     * 将 JSON 字符串反序列化为 {@link Map}{@code <String, Object>}。
     *
     * @param json JSON 字符串
     * @return 反序列化后的映射；若 {@code json} 为空或 {@code "null"} 则返回 {@code null}
     */
    public static Map<String, Object> fromJsonToMap(String json) {
        Type mapType = new TypeToken<Map<String, Object>>() {
        }.getType();
        return GSON.fromJson(json, mapType);
    }

    /**
     * 将对象序列化为带缩进格式化的 JSON 字符串（人类可读）。
     *
     * @param obj 待序列化的对象
     * @return 格式化后的 JSON 字符串；若 {@code obj} 为 {@code null} 则返回 {@code "null"}
     */
    public static String toPrettyJson(Object obj) {
        return PRETTY_GSON.toJson(obj);
    }

    /**
     * 判断字符串是否为合法 JSON。
     *
     * <p>{@code null}、空串、纯空白串均视为不合法；能被成功解析为 JSON 元素（含 {@code "null"}
     * 字面量）的字符串视为合法。</p>
     *
     * @param json 待校验的字符串
     * @return 合法返回 {@code true}，否则返回 {@code false}
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        try {
            GSON.fromJson(json, JsonElement.class);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    /**
     * {@link Integer} 类型的 Gson 自定义适配器。
     *
     * <p>反序列化时，若数值字段被写成空字符串（{@code ""}）或字符串 {@code "null"}，兜底为 {@code 0}，
     * 避免默认转换抛出异常；序列化时按原始整型写出。</p>
     */
    private static final class IntegerAdapter implements JsonSerializer<Integer>, JsonDeserializer<Integer> {

        @Override
        public Integer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json.getAsString().isEmpty() || json.getAsString().equals("null")) {
                return 0;
            }
            return json.getAsInt();
        }

        @Override
        public JsonElement serialize(Integer src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src);
        }
    }

    /**
     * {@link Long} 类型的 Gson 自定义适配器。
     *
     * <p>反序列化时，若长整型字段被写成空字符串（{@code ""}）或字符串 {@code "null"}，兜底为 {@code 0L}，
     * 避免默认转换抛出异常；序列化时按原始长整型写出。</p>
     */
    private static final class LongAdapter implements JsonSerializer<Long>, JsonDeserializer<Long> {

        @Override
        public Long deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json.getAsString().isEmpty() || json.getAsString().equals("null")) {
                return 0L;
            }
            return json.getAsLong();
        }

        @Override
        public JsonElement serialize(Long src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src);
        }
    }

}
