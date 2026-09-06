package com.xwms.common.config;

import java.time.LocalDateTime;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;

import com.xwms.common.tenant.handler.OwnerTenantHandler;

/**
 * MyBatis Plus配置 1. 货主租户插件（自动拼接owner_code，必须在分页插件之前） 2. 分页插件（MySQL方言） 3.
 * 自动填充（createdAt/updatedAt/createdBy/updatedBy）
 *
 * <p>插件顺序很重要：TenantLineInnerInterceptor 必须在 PaginationInnerInterceptor 之前，
 * 否则分页count查询不会被租户条件过滤，导致总数错误。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 租户插件必须在分页插件之前
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new OwnerTenantHandler()));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(
                        metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(
                        metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
                // createdBy/updatedBy 从Security上下文获取（TODO）
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(
                        metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
