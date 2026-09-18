package com.ytrue.game.framework.database.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * MyBatis 双数据源配置（主库 app_data + 日志库 app_log）。
 *
 * <p>定义两个数据源（分别绑定 {@code spring.datasource.data} / {@code spring.datasource.log}），
 * 并为它们各装配一套 {@link SqlSessionFactory}；再通过两个 {@link MapperScan} 把不同包路径的
 * Mapper 路由到对应会话工厂：主库 Mapper 走 {@code @Primary} 的 {@code dataDbFactory}，
 * 日志库 Mapper 走 {@code sqlSessionFactoryRef} 指定的 {@code logDbFactory}。</p>
 *
 * <p>注意：数据源 Bean 由本类自身定义，故 {@code SqlSessionFactory} 通过<b>方法调用</b>
 * （{@code dataDbSource()}）引用它们，而非构造注入——若用构造注入去注入「本类自己定义的
 * Bean」，会形成循环依赖（{@code BeanCurrentlyInCreationException}）。方法调用依赖
 * {@code @Configuration} 的 CGLIB 代理返回容器中的单例，规避了该问题。</p>
 *
 * @since 1.0.0
 */
@Configuration
@MapperScan(basePackages = {"com.ytrue.game.framework.database.data.mapper", "com.ytrue.game.business.*.data"})
@MapperScan(basePackages = {"com.ytrue.game.framework.database.log.mapper", "com.ytrue.game.business.*.log"},
        sqlSessionFactoryRef = "logDbFactory")
public class MyBatisConfig {

    /**
     * 主库数据源（app_data）。
     *
     * @return 绑定 {@code spring.datasource.data} 配置的数据源
     */
    @Bean(name = "dataDbSource")
    @ConfigurationProperties(prefix = "spring.datasource.data")
    public DataSource dataDbSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * 日志库数据源（app_log）。
     *
     * @return 绑定 {@code spring.datasource.log} 配置的数据源
     */
    @Bean(name = "logDbSource")
    @ConfigurationProperties(prefix = "spring.datasource.log")
    public DataSource logDbSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * 主库 SqlSessionFactory。
     *
     * @return 主库会话工厂
     * @throws Exception 构建失败时抛出
     */
    @Primary
    @Bean(name = "dataDbFactory")
    public SqlSessionFactory dataSqlSessionFactory() throws Exception {
        return build(dataDbSource());
    }

    /**
     * 日志库 SqlSessionFactory。
     *
     * @return 日志库会话工厂
     * @throws Exception 构建失败时抛出
     */
    @Bean(name = "logDbFactory")
    public SqlSessionFactory logSqlSessionFactory() throws Exception {
        return build(logDbSource());
    }

    /**
     * 基于指定数据源构建开启了驼峰映射的 {@link SqlSessionFactory}。
     *
     * @param dataSource 目标数据源
     * @return 构建好的会话工厂
     * @throws Exception 构建失败时抛出
     */
    private static SqlSessionFactory build(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        // 开启下划线 → 驼峰字段映射（如 user_state → userState）
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);
        return factoryBean.getObject();
    }

}
