package com.xwms.core.performance.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.performance.entity.Employee;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

    @Select(
            "SELECT * FROM wms_employee WHERE warehouse_code = #{warehouse} AND status = 'ACTIVE' AND deleted = 0 ORDER BY employee_no")
    List<Employee> selectActiveByWarehouse(@Param("warehouse") String warehouse);

    @Select(
            "SELECT * FROM wms_employee WHERE department = #{department} AND status = 'ACTIVE' AND deleted = 0 ORDER BY employee_no")
    List<Employee> selectActiveByDepartment(@Param("department") String department);
}
