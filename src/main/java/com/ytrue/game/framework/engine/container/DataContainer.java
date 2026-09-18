package com.ytrue.game.framework.engine.container;

import com.ytrue.game.framework.engine.annotation.AppData;
import com.ytrue.game.framework.engine.data.BaseCsvData;
import com.ytrue.game.framework.engine.utils.GsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 数据容器。
 *
 * <p>负责加载并缓存 {@code @AppData} 标注的 CSV 静态配置数据，对外提供按 id、随机、范围等方式
 * 查询配置实体的能力。数据按「类名 → (数据 id → 数据实体)」两级结构缓存，首次访问时惰性加载。</p>
 *
 * @since 1.0.0
 */
@Slf4j
@SuppressWarnings("unchecked")
public class DataContainer {

    /**
     * 数据记录表：类名 → (数据 id → 数据实体)。
     */
    private static final Map<String, Map<Long, BaseCsvData>> DATA_MAP = new HashMap<>();

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
        // 读取 @AppData 注解，未标注的类直接跳过
        AppData appData = clazz.getAnnotation(AppData.class);
        if (appData == null) {
            return;
        }

        try {
            // 按注解配置的文件路径与编码打开 CSV 文件
            FileInputStream fileInputStream = new FileInputStream(appData.fileUrl());
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, appData.charSet());

            // 首行为表头，逐行解析出记录集
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setHeader().build().parse(inputStreamReader);

            Map<Long, BaseCsvData> innerMap = new HashMap<>();

            for (CSVRecord record : records) {
                try {
                    // 行记录 → JSON → 实体对象（借 Gson 完成表头字段到实体字段的映射）
                    String recordJson = GsonUtils.toJson(record.toMap());
                    BaseCsvData instance = GsonUtils.fromJson(recordJson, clazz);

                    // 以实体 id 为键放入本表
                    innerMap.put(instance.getId(), instance);
                } catch (Exception ignored) {
                    // 单行解析失败忽略，不影响其它行
                }
            }
            // 以类名为键缓存整表数据
            DATA_MAP.put(clazz.getName(), innerMap);
        } catch (Exception e) {
            log.error("初始化CSV数据发生异常:[{}]", e.getMessage(), e);
        }
    }

    /**
     * 获取数据（未缓存时惰性加载）。
     *
     * @param clazz 配置实体类型
     * @param <T>   配置实体类型泛型
     * @return 对应类的「id → 实体」映射
     */
    private static <T extends BaseCsvData> Map<Long, BaseCsvData> getOrLoad(Class<T> clazz) {
        // 未缓存则先惰性加载
        if (!DATA_MAP.containsKey(clazz.getName())) {
            dataInit(clazz);
        }
        return DATA_MAP.get(clazz.getName());
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
        // 打乱后取前 size 条，即随机多条
        Collections.shuffle(dataList, ThreadLocalRandom.current());
        return !dataList.isEmpty() ? new LinkedList<>(dataList.subList(0, size)) : null;
    }

    /**
     * 获取所有数据。
     *
     * @param clazz 配置实体类型
     * @param <T>   配置实体类型泛型
     * @return 数据列表
     */
    public static <T extends BaseCsvData> List<T> getDataList(Class<T> clazz) {
        return getDataList(clazz, Integer.MIN_VALUE, Integer.MAX_VALUE);
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
