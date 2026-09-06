package com.xwms.core.plugin.industry.ecommerce.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.xwms.core.plugin.industry.ecommerce.entity.FlashSaleActivity;

/** 秒杀活动Mapper */
@Mapper
public interface FlashSaleActivityMapper extends BaseMapper<FlashSaleActivity> {

    /** 根据活动编号查询 */
    FlashSaleActivity selectByActivityNo(@Param("activityNo") String activityNo);

    /** 查询进行中的活动 */
    List<FlashSaleActivity> selectActiveActivities();

    /** 查询待开始的活动 */
    List<FlashSaleActivity> selectPendingActivities();

    /** 根据店铺和状态查询 */
    List<FlashSaleActivity> selectByShopAndStatus(
            @Param("shopCode") String shopCode, @Param("status") String status);

    /** 查询库存未锁定的活动 */
    List<FlashSaleActivity> selectUnlockedStockActivities();
}
