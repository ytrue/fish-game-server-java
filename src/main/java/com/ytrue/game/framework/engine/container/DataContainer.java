package com.ytrue.game.framework.engine.container;

import com.ytrue.game.framework.engine.annotation.AppData;
import com.ytrue.game.framework.engine.data.BaseCsvData;
import com.ytrue.game.framework.engine.utils.GsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 数据容器。
 *
 * <p>负责加载并缓存 {@code @AppData} 标注的 CSV 静态配置数据，对外提供按 id、随机、范围等方式
 * 查询配置实体的能力。数据按「类名 → (数据 id → 数据实体)」两级结构缓存，首次访问时惰性加载。</p>
 *
 * <p>线程安全：缓存表为 {@link ConcurrentHashMap}，惰性加载通过 {@code computeIfAbsent} 完成，
 * 保证并发下每张表只加载一次。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@SuppressWarnings("unchecked")
public class DataContainer {

    /**
     * 数据记录表：类名 → (数据 id → 数据实体)。
     */
    private static final Map<String, Map<Long, BaseCsvData>> DATA_MAP = new ConcurrentHashMap<>();

    /**
     * 私有构造器，禁止实例化（纯静态容器）。
     */
    private DataContainer() {
    }

    /**
     * 初始化数据。
     *
     * @param csvBean 配置实体实例（取其类型加载对应 CSV）
     */
    public static void dataInit(BaseCsvData csvBean) {
        dataInit(csvBean.getClass());
    }

    /**
     * 初始化数据。
     *
     * @param clazz 配置实体类型（需标注 {@code @AppData}）
     */
    public static void dataInit(Class<? extends BaseCsvData> clazz) {
        DATA_MAP.put(clazz.getName(), load(clazz));
    }

    /**
     * 获取数据（未缓存时惰性加载）。
     *
     * <p>用 {@code computeIfAbsent} 而非「先查再放」，保证并发下每张表只加载一次；
     * 加载结果即使是空表也会被缓存，避免配置文件缺失时每次查询都重试读盘。</p>
     *
     * @param clazz 配置实体类型
     * @param <T>   配置实体类型泛型
     * @return 对应类的「id → 实体」映射
     */
    private static <T extends BaseCsvData> Map<Long, BaseCsvData> getOrLoad(Class<T> clazz) {
        return DATA_MAP.computeIfAbsent(clazz.getName(), key -> load(clazz));
    }

    /**
     * 从 CSV 文件加载一张配置表。
     *
     * <p>未标注 {@code @AppData} 或读取失败时返回空表，由调用方决定如何兜底。</p>
     *
     * @param clazz 配置实体类型
     * @param <T>   配置实体类型泛型
     * @return 「id → 实体」映射
     */
    private static <T extends BaseCsvData> Map<Long, BaseCsvData> load(Class<T> clazz) {
        Map<Long, BaseCsvData> innerMap = new HashMap<>();

        // 读取 @AppData 注解，未标注的类直接返回空表
        AppData appData = clazz.getAnnotation(AppData.class);
        if (appData == null) {
            return innerMap;
        }

        // try-with-resources：无论解析是否异常，都保证文件句柄被关闭
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(appData.fileUrl()), appData.charSet())) {

            // 首行为表头，逐行解析出记录集
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setHeader().build().parse(reader);

            for (CSVRecord record : records) {
                try {
                    // 行记录 → JSON → 实体对象（借 Gson 完成表头字段到实体字段的映射）
                    String recordJson = GsonUtils.toJson(record.toMap());
                    BaseCsvData instance = GsonUtils.fromJson(recordJson, clazz);

                    // 以实体 id 为键放入本表
                    innerMap.put(instance.getId(), instance);
                } catch (Exception e) {
                    // 单行解析失败不影响其它行
                    log.error("CSV单行解析发生异常:[{}]", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("初始化CSV数据发生异常:[{}]", e.getMessage(), e);
        }
        return innerMap;
    }

    /**
     * 获取单条数据。
     *
     * @param id    数据 id
     * @param clazz 配置实体类型
     * @param <T>   配置实体类型泛型
     * @return 数据实体；不存在时返回 {@code null}
     */
    public static <T extends BaseCsvData> T getData(long id, Class<T> clazz) {
        return (T) getOrLoad(clazz).get(id);
    }

    /**
     * 获取随机单条数据。
     *
     * @param clazz 配置实体类型
     * @param <T>   配置实体类型泛型
     * @return 随机数据实体；无数据时返回 {@code null}
     */
    public static <T extends BaseCsvData> T getRandomData(Class<T> clazz) {
        return getRandomData(clazz, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    /**
     * 获取 id 范围内随机单条数据。
     *
     * @param clazz 配置实体类型
     * @param start id 下界（含）
     * @param end   id 上界（含）
     * @param <T>   配置实体类型泛型
     * @return 随机数据实体；范围内无数据时返回 {@code null}
     */
    public static <T extends BaseCsvData> T getRandomData(Class<T> clazz, long start, long end) {
        List<T> dataList = getDataList(clazz, start, end);
        // 打乱后取第一个，即随机一条
        Collections.shuffle(dataList, ThreadLocalRandom.current());
        return !dataList.isEmpty() ? dataList.getFirst() : null;
    }

    /**
     * 获取多条随机数据。
     *
     * @param clazz 配置实体类型
     * @param size  数量
     * @param <T>   配置实体类型泛型
     * @return 随机数据列表；无数据时返回 {@code null}
     */
    public static <T extends BaseCsvData> List<T> getRandomDataList(Class<T> clazz, int size) {
        return getRandomDataList(clazz, size, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    /**
     * 获取 id 范围内多条随机数据。
     *
     * @param clazz 配置实体类型
     * @param size  数量
     * @param start id 下界（含）
     * @param end   id 上界（含）
     * @param <T>   配置实体类型泛型
     * @return 随机数据列表；无数据时返回 {@code null}
     */
    public static <T extends BaseCsvData> List<T> getRandomDataList(Class<T> clazz, int size, long start, long end) {
        List<T> dataList = getDataList(clazz, start, end);
        if (dataList.isEmpty()) {
            return null;
        }
        // 打乱后取前 size 条，即随机多条
        Collections.shuffle(dataList, ThreadLocalRandom.current());
        // size 可能大于实际条数，取两者较小值，避免 subList 越界
        return new LinkedList<>(dataList.subList(0, Math.min(size, dataList.size())));
    }

    /**
     * 获取所有数据。
     *
     * @param clazz 配置实体类型
     * @param <T>   配置实体类型泛型
     * @return 数据列表
     */
    public static <T extends BaseCsvData> List<T> getDataList(Class<T> clazz) {
        // 与 getRandomData(clazz) 保持一致，用 long 的边界而非 int 的，
        // 否则 id 超出 int 范围的数据会被静默过滤掉
        return getDataList(clazz, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    /**
     * 获取 id 范围内的所有数据。
     *
     * @param clazz 配置实体类型
     * @param start id 下界（含）
     * @param end   id 上界（含）
     * @param <T>   配置实体类型泛型
     * @return 数据列表
     */
    public static <T extends BaseCsvData> List<T> getDataList(Class<T> clazz, long start, long end) {
        Collection<BaseCsvData> lineMap = getOrLoad(clazz).values();
        // 过滤出 id 落在 [start, end] 区间内的数据
        List<BaseCsvData> dataList = lineMap.stream()
                .filter(data -> (data.getId() >= start && data.getId() <= end))
                .toList();
        return new LinkedList<>((Collection<? extends T>) dataList);
    }
}
