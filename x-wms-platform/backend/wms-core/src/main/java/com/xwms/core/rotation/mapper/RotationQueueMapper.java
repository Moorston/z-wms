package com.xwms.core.rotation.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.rotation.entity.RotationQueue;

@Mapper
public interface RotationQueueMapper extends BaseMapper<RotationQueue> {

    /** 查询队列中按周转顺序排序的批次 */
    @Select(
            "SELECT * FROM wms_rotation_queue WHERE queue_id = #{queueId} AND status = 'ACTIVE' AND quantity > 0 ORDER BY sort_order ASC")
    List<RotationQueue> selectByQueueId(@Param("queueId") String queueId);

    /** 查询SKU在所有库位的周转队列 */
    @Select(
            "SELECT * FROM wms_rotation_queue WHERE sku_code = #{skuCode} AND status = 'ACTIVE' AND quantity > 0 ORDER BY sort_value ASC")
    List<RotationQueue> selectBySku(@Param("skuCode") String skuCode);

    /** 扣减队列数量（原子SQL） */
    @Update(
            "UPDATE wms_rotation_queue SET quantity = quantity - #{qty}, updated_time = NOW() WHERE id = #{id} AND quantity >= #{qty}")
    int deductQuantity(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /** 查询即将过期的批次 */
    @Select(
            "SELECT * FROM wms_rotation_queue WHERE expire_date IS NOT NULL AND expire_date <= NOW() + #{days} AND status = 'ACTIVE' AND quantity > 0 ORDER BY expire_date ASC")
    List<RotationQueue> selectExpiringBatches(@Param("days") int days);
}
