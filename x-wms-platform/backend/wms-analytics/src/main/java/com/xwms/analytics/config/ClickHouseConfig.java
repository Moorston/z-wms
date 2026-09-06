package com.xwms.analytics.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariDataSource;

/** ClickHouse数据源配置 独立于业务数据库（Oracle），用于OLAP分析查询 */
@Configuration
public class ClickHouseConfig {

    @Bean(name = "clickHouseDataSource")
    @ConfigurationProperties(prefix = "clickhouse.datasource")
    public DataSource clickHouseDataSource() {
        return new HikariDataSource();
    }

    @Bean(name = "clickHouseJdbcTemplate")
    public JdbcTemplate clickHouseJdbcTemplate(
            @Qualifier("clickHouseDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
