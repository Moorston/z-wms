-- ============================================================
-- X WMS 数据字典初始化数据
-- 包含WMS系统常用的字典类型和字典项
-- ============================================================

-- 字典类型
INSERT INTO sys_dict_type (id, dict_code, dict_name, description, status, sort_order, remark, created_time, updated_time) VALUES
(1, 'inbound_type', '入库单类型', '入库业务的单据类型分类', 'ACTIVE', 1, '采购入库/退货入库/调拨入库等', NOW(), NOW()),
(2, 'inbound_status', '入库单状态', '入库单的生命周期状态', 'ACTIVE', 2, '创建/收货/质检/上架/完成', NOW(), NOW()),
(3, 'outbound_type', '出库单类型', '出库业务的单据类型分类', 'ACTIVE', 3, '销售出库/调拨出库/退货出库等', NOW(), NOW()),
(4, 'outbound_status', '出库单状态', '出库单的生命周期状态', 'ACTIVE', 4, '创建/分配/拣货/复核/发运', NOW(), NOW()),
(5, 'inventory_status', '库存状态', '库存的质量/业务状态', 'ACTIVE', 5, '正常/冻结/残次/待检', NOW(), NOW()),
(6, 'location_type', '库位类型', '仓库库位的功能分类', 'ACTIVE', 6, '存储区/拣货区/收货区等', NOW(), NOW()),
(7, 'work_task_type', '作业类型', '仓库作业任务的类型', 'ACTIVE', 7, '收货/上架/拣货/复核等', NOW(), NOW()),
(8, 'work_task_status', '作业状态', '作业任务的执行状态', 'ACTIVE', 8, '待执行/执行中/已完成', NOW(), NOW()),
(9, 'wave_status', '波次状态', '波次的生命周期状态', 'ACTIVE', 9, '创建/执行中/已完成/取消', NOW(), NOW()),
(10, 'express_company', '快递商', '支持的快递公司', 'ACTIVE', 10, '顺丰/京东/圆通等', NOW(), NOW()),
(11, 'industry_type', '行业类型', '货主所属行业', 'ACTIVE', 11, '医药/食品/电商/通用', NOW(), NOW()),
(12, 'owner_type', '货主类型', '货主的业务类型', 'ACTIVE', 12, '制造商/分销商/零售商', NOW(), NOW()),
(13, 'customer_type', '客户类型', '客户的业务类型', 'ACTIVE', 13, 'B2B/B2C/门店', NOW(), NOW()),
(14, 'qc_result', '质检结果', '入库质检的结果', 'ACTIVE', 14, '合格/不合格/让步接收', NOW(), NOW()),
(15, 'check_status', '盘点状态', '盘点单的状态', 'ACTIVE', 15, '创建/盘点中/已完成', NOW(), NOW());

-- 入库单类型
INSERT INTO sys_dict_item (id, dict_code, item_value, item_label, description, sort_order, status, parent_value, ext_attrs, remark, created_time, updated_time) VALUES
(101, 'inbound_type', 'PURCHASE', '采购入库', '供应商采购到货入库', 1, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(102, 'inbound_type', 'RETURN', '退货入库', '客户退货回库', 2, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(103, 'inbound_type', 'TRANSFER', '调拨入库', '仓库间调拨入库', 3, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(104, 'inbound_type', 'PRODUCTION', '生产入库', '生产完工入库', 4, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(105, 'inbound_type', 'CROSS_DOCK', '越库入库', '越库操作（直接转出库）', 5, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW());

-- 入库单状态
INSERT INTO sys_dict_item (id, dict_code, item_value, item_label, description, sort_order, status, parent_value, ext_attrs, remark, created_time, updated_time) VALUES
(201, 'inbound_status', 'CREATED', '已创建', '入库单已创建，待收货', 1, 'ACTIVE', NULL, '{"color":"blue"}', NULL, NOW(), NOW()),
(202, 'inbound_status', 'RECEIVING', '收货中', '正在收货', 2, 'ACTIVE', NULL, '{"color":"orange"}', NULL, NOW(), NOW()),
(203, 'inbound_status', 'RECEIVED', '已收货', '收货完成，待质检', 3, 'ACTIVE', NULL, '{"color":"cyan"}', NULL, NOW(), NOW()),
(204, 'inbound_status', 'QCING', '质检中', '正在质检', 4, 'ACTIVE', NULL, '{"color":"purple"}', NULL, NOW(), NOW()),
(205, 'inbound_status', 'QC_DONE', '质检完成', '质检完成，待上架', 5, 'ACTIVE', NULL, '{"color":"teal"}', NULL, NOW(), NOW()),
(206, 'inbound_status', 'PUTAWAY', '上架中', '正在上架', 6, 'ACTIVE', NULL, '{"color":"yellow"}', NULL, NOW(), NOW()),
(207, 'inbound_status', 'COMPLETED', '已完成', '入库全部完成', 7, 'ACTIVE', NULL, '{"color":"green"}', NULL, NOW(), NOW()),
(208, 'inbound_status', 'CANCELLED', '已取消', '入库单已取消', 8, 'ACTIVE', NULL, '{"color":"gray"}', NULL, NOW(), NOW());

-- 出库单类型
INSERT INTO sys_dict_item (id, dict_code, item_value, item_label, description, sort_order, status, parent_value, ext_attrs, remark, created_time, updated_time) VALUES
(301, 'outbound_type', 'SALES', '销售出库', '客户销售订单出库', 1, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(302, 'outbound_type', 'TRANSFER', '调拨出库', '仓库间调拨出库', 2, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(303, 'outbound_type', 'RETURN', '退货出库', '退回供应商出库', 3, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(304, 'outbound_type', 'SAMPLE', '样品出库', '样品领用出库', 4, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(305, 'outbound_type', 'SCRAP', '报废出库', '库存报废出库', 5, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW());

-- 出库单状态
INSERT INTO sys_dict_item (id, dict_code, item_value, item_label, description, sort_order, status, parent_value, ext_attrs, remark, created_time, updated_time) VALUES
(401, 'outbound_status', 'CREATED', '已创建', '出库单已创建，待分配', 1, 'ACTIVE', NULL, '{"color":"blue"}', NULL, NOW(), NOW()),
(402, 'outbound_status', 'ALLOCATED', '已分配', '库存已预占分配', 2, 'ACTIVE', NULL, '{"color":"cyan"}', NULL, NOW(), NOW()),
(403, 'outbound_status', 'PICKING', '拣货中', '正在拣货', 3, 'ACTIVE', NULL, '{"color":"orange"}', NULL, NOW(), NOW()),
(404, 'outbound_status', 'PICKED', '已拣货', '拣货完成，待复核', 4, 'ACTIVE', NULL, '{"color":"teal"}', NULL, NOW(), NOW()),
(405, 'outbound_status', 'CHECKING', '复核中', '正在复核', 5, 'ACTIVE', NULL, '{"color":"purple"}', NULL, NOW(), NOW()),
(406, 'outbound_status', 'PACKED', '已打包', '打包完成，待发运', 6, 'ACTIVE', NULL, '{"color":"yellow"}', NULL, NOW(), NOW()),
(407, 'outbound_status', 'SHIPPED', '已发运', '已发货出库', 7, 'ACTIVE', NULL, '{"color":"green"}', NULL, NOW(), NOW()),
(408, 'outbound_status', 'CANCELLED', '已取消', '出库单已取消', 8, 'ACTIVE', NULL, '{"color":"gray"}', NULL, NOW(), NOW());

-- 库存状态
INSERT INTO sys_dict_item (id, dict_code, item_value, item_label, description, sort_order, status, parent_value, ext_attrs, remark, created_time, updated_time) VALUES
(501, 'inventory_status', 'NORMAL', '正常', '可正常出库的库存', 1, 'ACTIVE', NULL, '{"color":"green"}', NULL, NOW(), NOW()),
(502, 'inventory_status', 'FROZEN', '冻结', '被冻结不可出库', 2, 'ACTIVE', NULL, '{"color":"blue"}', NULL, NOW(), NOW()),
(503, 'inventory_status', 'DEFECTIVE', '残次', '质量有问题的库存', 3, 'ACTIVE', NULL, '{"color":"red"}', NULL, NOW(), NOW()),
(504, 'inventory_status', 'PENDING_QC', '待检', '等待质检的库存', 4, 'ACTIVE', NULL, '{"color":"orange"}', NULL, NOW(), NOW()),
(505, 'inventory_status', 'RESERVED', '预留', '已被订单预占', 5, 'ACTIVE', NULL, '{"color":"purple"}', NULL, NOW(), NOW());

-- 库位类型
INSERT INTO sys_dict_item (id, dict_code, item_value, item_label, description, sort_order, status, parent_value, ext_attrs, remark, created_time, updated_time) VALUES
(601, 'location_type', 'STORAGE', '存储区', '长期存储货物', 1, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(602, 'location_type', 'PICKING', '拣货区', '拣货作业区', 2, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(603, 'location_type', 'RECEIVING', '收货区', '临时存放待上架货物', 3, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(604, 'location_type', 'SHIPPING', '发货区', '待发运货物暂存', 4, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(605, 'location_type', 'QC', '质检区', '质检作业区', 5, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(606, 'location_type', 'RETURN', '退货区', '退货货物暂存', 6, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(607, 'location_type', 'CROSS_DOCK', '越库区', '越库作业区', 7, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW());

-- 作业类型
INSERT INTO sys_dict_item (id, dict_code, item_value, item_label, description, sort_order, status, parent_value, ext_attrs, remark, created_time, updated_time) VALUES
(701, 'work_task_type', 'RECEIVE', '收货', '入库收货作业', 1, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(702, 'work_task_type', 'PUTAWAY', '上架', '入库上架作业', 2, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(703, 'work_task_type', 'PICK', '拣货', '出库拣货作业', 3, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(704, 'work_task_type', 'CHECK', '复核', '出库复核作业', 4, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(705, 'work_task_type', 'PACK', '打包', '出库打包作业', 5, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(706, 'work_task_type', 'SHIP', '发运', '出库发运作业', 6, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(707, 'work_task_type', 'QC', '质检', '入库质检作业', 7, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(708, 'work_task_type', 'MOVE', '移库', '库位间移动', 8, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(709, 'work_task_type', 'COUNT', '盘点', '库存盘点作业', 9, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW());

-- 作业状态
INSERT INTO sys_dict_item (id, dict_code, item_value, item_label, description, sort_order, status, parent_value, ext_attrs, remark, created_time, updated_time) VALUES
(801, 'work_task_status', 'PENDING', '待执行', '作业已创建，待执行', 1, 'ACTIVE', NULL, '{"color":"gray"}', NULL, NOW(), NOW()),
(802, 'work_task_status', 'PROCESSING', '执行中', '作业正在执行', 2, 'ACTIVE', NULL, '{"color":"orange"}', NULL, NOW(), NOW()),
(803, 'work_task_status', 'COMPLETED', '已完成', '作业已完成', 3, 'ACTIVE', NULL, '{"color":"green"}', NULL, NOW(), NOW()),
(804, 'work_task_status', 'CANCELLED', '已取消', '作业已取消', 4, 'ACTIVE', NULL, '{"color":"red"}', NULL, NOW(), NOW()),
(805, 'work_task_status', 'EXCEPTION', '异常', '作业执行异常', 5, 'ACTIVE', NULL, '{"color":"purple"}', NULL, NOW(), NOW());

-- 波次状态
INSERT INTO sys_dict_item (id, dict_code, item_value, item_label, description, sort_order, status, parent_value, ext_attrs, remark, created_time, updated_time) VALUES
(901, 'wave_status', 'CREATED', '已创建', '波次已创建，待执行', 1, 'ACTIVE', NULL, '{"color":"blue"}', NULL, NOW(), NOW()),
(902, 'wave_status', 'PROCESSING', '执行中', '波次正在执行', 2, 'ACTIVE', NULL, '{"color":"orange"}', NULL, NOW(), NOW()),
(903, 'wave_status', 'COMPLETED', '已完成', '波次全部完成', 3, 'ACTIVE', NULL, '{"color":"green"}', NULL, NOW(), NOW()),
(904, 'wave_status', 'CANCELLED', '已取消', '波次已取消', 4, 'ACTIVE', NULL, '{"color":"gray"}', NULL, NOW(), NOW()),
(905, 'wave_status', 'PARTIAL', '部分完成', '部分订单完成', 5, 'ACTIVE', NULL, '{"color":"yellow"}', NULL, NOW(), NOW());

-- 快递商
INSERT INTO sys_dict_item (id, dict_code, item_value, item_label, description, sort_order, status, parent_value, ext_attrs, remark, created_time, updated_time) VALUES
(1001, 'express_company', 'SF', '顺丰速运', '顺丰快递', 1, 'ACTIVE', NULL, '{"code":"SF","logo":"sf.png"}', NULL, NOW(), NOW()),
(1002, 'express_company', 'JD', '京东物流', '京东快递', 2, 'ACTIVE', NULL, '{"code":"JD","logo":"jd.png"}', NULL, NOW(), NOW()),
(1003, 'express_company', 'YTO', '圆通速递', '圆通快递', 3, 'ACTIVE', NULL, '{"code":"YTO","logo":"yto.png"}', NULL, NOW(), NOW()),
(1004, 'express_company', 'ZTO', '中通快递', '中通快递', 4, 'ACTIVE', NULL, '{"code":"ZTO","logo":"zto.png"}', NULL, NOW(), NOW()),
(1005, 'express_company', 'STO', '申通快递', '申通快递', 5, 'ACTIVE', NULL, '{"code":"STO","logo":"sto.png"}', NULL, NOW(), NOW()),
(1006, 'express_company', 'YUNDA', '韵达快递', '韵达快递', 6, 'ACTIVE', NULL, '{"code":"YUNDA","logo":"yunda.png"}', NULL, NOW(), NOW()),
(1007, 'express_company', 'EMS', '邮政EMS', '中国邮政EMS', 7, 'ACTIVE', NULL, '{"code":"EMS","logo":"ems.png"}', NULL, NOW(), NOW()),
(1008, 'express_company', 'DBL', '德邦快递', '德邦物流', 8, 'ACTIVE', NULL, '{"code":"DBL","logo":"dbl.png"}', NULL, NOW(), NOW());

-- 行业类型
INSERT INTO sys_dict_item (id, dict_code, item_value, item_label, description, sort_order, status, parent_value, ext_attrs, remark, created_time, updated_time) VALUES
(1101, 'industry_type', 'PHARMA', '医药', '医药行业（GSP合规）', 1, 'ACTIVE', NULL, '{"plugin":"gsp"}', NULL, NOW(), NOW()),
(1102, 'industry_type', 'FOOD', '食品', '食品行业（冷链）', 2, 'ACTIVE', NULL, '{"plugin":"coldchain"}', NULL, NOW(), NOW()),
(1103, 'industry_type', 'ECOMMERCE', '电商', '电商行业（大促）', 3, 'ACTIVE', NULL, '{"plugin":"ecommerce"}', NULL, NOW(), NOW()),
(1104, 'industry_type', 'GENERAL', '通用', '通用仓储', 4, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW());

-- 货主类型
INSERT INTO sys_dict_item (id, dict_code, item_value, item_label, description, sort_order, status, parent_value, ext_attrs, remark, created_time, updated_time) VALUES
(1201, 'owner_type', 'MANUFACTURER', '制造商', '生产制造企业', 1, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(1202, 'owner_type', 'DISTRIBUTOR', '分销商', '分销/批发企业', 2, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(1203, 'owner_type', 'RETAILER', '零售商', '零售企业', 3, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(1204, 'owner_type', 'ECOMMERCE', '电商', '电商平台/卖家', 4, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW());

-- 客户类型
INSERT INTO sys_dict_item (id, dict_code, item_value, item_label, description, sort_order, status, parent_value, ext_attrs, remark, created_time, updated_time) VALUES
(1301, 'customer_type', 'B2B', '企业客户', '企业对企业', 1, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(1302, 'customer_type', 'B2C', '个人客户', '企业对个人', 2, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
(1303, 'customer_type', 'STORE', '门店', '线下门店', 3, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW());

-- 质检结果
INSERT INTO sys_dict_item (id, dict_code, item_value, item_label, description, sort_order, status, parent_value, ext_attrs, remark, created_time, updated_time) VALUES
(1401, 'qc_result', 'PASS', '合格', '质检合格', 1, 'ACTIVE', NULL, '{"color":"green"}', NULL, NOW(), NOW()),
(1402, 'qc_result', 'FAIL', '不合格', '质检不合格', 2, 'ACTIVE', NULL, '{"color":"red"}', NULL, NOW(), NOW()),
(1403, 'qc_result', 'CONCESSION', '让步接收', '有瑕疵但可接收', 3, 'ACTIVE', NULL, '{"color":"orange"}', NULL, NOW(), NOW()),
(1404, 'qc_result', 'PENDING', '待检', '等待质检', 4, 'ACTIVE', NULL, '{"color":"gray"}', NULL, NOW(), NOW());

-- 盘点状态
INSERT INTO sys_dict_item (id, dict_code, item_value, item_label, description, sort_order, status, parent_value, ext_attrs, remark, created_time, updated_time) VALUES
(1501, 'check_status', 'CREATED', '已创建', '盘点单已创建', 1, 'ACTIVE', NULL, '{"color":"blue"}', NULL, NOW(), NOW()),
(1502, 'check_status', 'COUNTING', '盘点中', '正在盘点', 2, 'ACTIVE', NULL, '{"color":"orange"}', NULL, NOW(), NOW()),
(1503, 'check_status', 'COMPLETED', '已完成', '盘点完成', 3, 'ACTIVE', NULL, '{"color":"green"}', NULL, NOW(), NOW()),
(1504, 'check_status', 'ADJUSTED', '已调整', '差异已调整', 4, 'ACTIVE', NULL, '{"color":"teal"}', NULL, NOW(), NOW()),
(1505, 'check_status', 'CANCELLED', '已取消', '盘点已取消', 5, 'ACTIVE', NULL, '{"color":"gray"}', NULL, NOW(), NOW());

COMMIT;
