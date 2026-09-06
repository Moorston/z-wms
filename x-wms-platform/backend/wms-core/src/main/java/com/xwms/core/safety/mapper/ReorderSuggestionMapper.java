package com.xwms.core.safety.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.safety.entity.ReorderSuggestion;

@Mapper
public interface ReorderSuggestionMapper extends BaseMapper<ReorderSuggestion> {

    @Select("SELECT * FROM wms_reorder_suggestion WHERE suggestion_no = #{suggestionNo}")
    ReorderSuggestion selectBySuggestionNo(@Param("suggestionNo") String suggestionNo);

    @Select(
            "SELECT * FROM wms_reorder_suggestion WHERE warehouse_code = #{warehouseCode} AND status = #{status} ORDER BY suggest_type, created_time")
    List<ReorderSuggestion> selectByWarehouseAndStatus(
            @Param("warehouseCode") String warehouseCode, @Param("status") String status);

    @Update(
            "UPDATE wms_reorder_suggestion SET status = #{status}, ref_no = #{refNo}, updated_time = NOW() WHERE suggestion_no = #{suggestionNo}")
    int updateStatus(
            @Param("suggestionNo") String suggestionNo,
            @Param("status") String status,
            @Param("refNo") String refNo);
}
