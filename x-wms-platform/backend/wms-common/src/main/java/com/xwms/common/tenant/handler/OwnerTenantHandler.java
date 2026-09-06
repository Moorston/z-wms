package com.xwms.common.tenant.handler;

import java.util.Set;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;

import com.xwms.common.tenant.context.OwnerContext;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;

/**
 * 货主租户处理器 MyBatis-Plus TenantLineInnerInterceptor 通过此处理器自动为SQL拼接 owner_code 条件。
 *
 * <p>工作原理： 1. 查询(SELECT)：自动在WHERE后追加 AND owner_code = 'xxx' 2. 插入(INSERT)：自动在字段列表中追加
 * owner_code，值为当前货主 3. 更新(UPDATE)：自动在WHERE后追加 AND owner_code = 'xxx' 4. 删除(DELETE)：自动在WHERE后追加 AND
 * owner_code = 'xxx'
 *
 * <p>忽略规则： - OwnerContext.isIgnore()=true 时不拼接（系统级查询） - OwnerContext.get()=null 时不拼接（未登录/内部调用） -
 * 系统表(sys_*)和不需要隔离的表不拼接
 */
@Slf4j
public class OwnerTenantHandler implements TenantLineHandler {

    /** 不需要货主隔离的表名（系统表、字典表等） 这些表没有owner_code字段，必须排除 */
    private static final Set<String> IGNORE_TABLES =
            Set.of(
                    // 系统管理表
                    "sys_user",
                    "sys_role",
                    "sys_permission",
                    "sys_user_role",
                    "sys_role_permission",
                    // 仓库/库区/库位（仓库级隔离，非货主级）
                    "wms_warehouse",
                    "wms_area",
                    "wms_location",
                    // 货主档案自身
                    "wms_owner");

    @Override
    public Expression getTenantId() {
        String ownerCode = OwnerContext.get();
        if (ownerCode == null) {
            // 未设置货主时返回null，插件会跳过拼接
            return new NullValue();
        }
        return new StringValue(ownerCode);
    }

    @Override
    public String getTenantIdColumn() {
        return "owner_code";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        // 1. 显式忽略的表
        if (IGNORE_TABLES.contains(tableName.toLowerCase())) {
            return true;
        }
        // 2. 上下文标记忽略（系统级查询/跨货主统计）
        if (OwnerContext.isIgnore()) {
            return true;
        }
        // 3. 未设置货主时忽略（避免内部任务出错）
        if (!OwnerContext.hasOwner()) {
            return true;
        }
        return false;
    }
}
