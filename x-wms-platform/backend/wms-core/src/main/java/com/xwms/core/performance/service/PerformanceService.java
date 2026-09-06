package com.xwms.core.performance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.exception.BizException;
import com.xwms.core.performance.entity.*;
import com.xwms.core.performance.enums.PerformanceLevel;
import com.xwms.core.performance.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 人员绩效管理核心服务 包含: 人员档案/作业记录/绩效日报计算/绩效考核 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final EmployeeMapper employeeMapper;
    private final WorkRecordMapper workRecordMapper;
    private final PerformanceDailyMapper dailyMapper;
    private final PerformanceAssessMapper assessMapper;

    private static final AtomicInteger RECORD_SEQ = new AtomicInteger(0);
    private static final AtomicInteger ASSESS_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 人员档案管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Employee createEmployee(Employee employee) {
        employee.setStatus("ACTIVE");
        employeeMapper.insert(employee);
        log.info("创建人员: {}", employee.getEmployeeNo());
        return employee;
    }

    public Employee getEmployee(Long id) {
        Employee emp = employeeMapper.selectById(id);
        if (emp == null) throw new BizException("人员不存在: " + id);
        return emp;
    }

    public Page<Employee> pageEmployees(
            Page<Employee> page, String warehouse, String department, String status) {
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        if (warehouse != null) wrapper.eq(Employee::getWarehouseCode, warehouse);
        if (department != null) wrapper.eq(Employee::getDepartment, department);
        if (status != null) wrapper.eq(Employee::getStatus, status);
        wrapper.orderByAsc(Employee::getEmployeeNo);
        return employeeMapper.selectPage(page, wrapper);
    }

    public List<Employee> getActiveEmployees(String warehouse) {
        return employeeMapper.selectActiveByWarehouse(warehouse);
    }

    // ============================================================

    // 2. 作业记录 (作业完成后自动记录)
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public WorkRecord createWorkRecord(WorkRecord record) {
        record.setRecordNo(generateRecordNo());
        if (record.getStartTime() == null) record.setStartTime(LocalDateTime.now());
        if (record.getEndTime() != null && record.getDurationMin() == null) {
            long minutes =
                    java.time.Duration.between(record.getStartTime(), record.getEndTime())
                            .toMinutes();
            record.setDurationMin((int) minutes);
        }
        workRecordMapper.insert(record);
        log.info(
                "记录作业: {}, 人员={}, 类型={}, 数量={}",
                record.getRecordNo(),
                record.getEmployeeNo(),
                record.getWorkType(),
                record.getQuantity());
        return record;
    }

    public List<WorkRecord> getWorkRecords(Long employeeId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return workRecordMapper.selectByEmployeeAndDateRange(employeeId, start, end);
    }

    // ============================================================

    // 3. 绩效日报计算 (PowerJob定时触发, 每日凌晨计算前一天)
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public PerformanceDaily calculateDailyPerformance(Long employeeId, LocalDate date) {
        Employee emp = getEmployee(employeeId);

        // 查询当日作业记录
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        List<WorkRecord> records =
                workRecordMapper.selectByEmployeeAndDateRange(employeeId, start, end);

        // 汇总统计
        BigDecimal receivingQty = BigDecimal.ZERO;
        BigDecimal putawayQty = BigDecimal.ZERO;
        BigDecimal pickingQty = BigDecimal.ZERO;
        BigDecimal checkingQty = BigDecimal.ZERO;
        BigDecimal packingQty = BigDecimal.ZERO;
        BigDecimal countingQty = BigDecimal.ZERO;
        BigDecimal movingQty = BigDecimal.ZERO;
        BigDecimal vasQty = BigDecimal.ZERO;
        BigDecimal replenishQty = BigDecimal.ZERO;
        int totalDuration = 0;
        int totalError = 0;
        BigDecimal totalQuality = BigDecimal.ZERO;
        int qualityCount = 0;

        for (WorkRecord r : records) {
            BigDecimal qty = r.getQuantity() != null ? r.getQuantity() : BigDecimal.ZERO;
            switch (r.getWorkType()) {
                case "RECEIVING" -> receivingQty = receivingQty.add(qty);
                case "PUTAWAY" -> putawayQty = putawayQty.add(qty);
                case "PICKING" -> pickingQty = pickingQty.add(qty);
                case "CHECKING" -> checkingQty = checkingQty.add(qty);
                case "PACKING" -> packingQty = packingQty.add(qty);
                case "COUNTING" -> countingQty = countingQty.add(qty);
                case "MOVING" -> movingQty = movingQty.add(qty);
                case "VAS" -> vasQty = vasQty.add(qty);
                case "REPLENISH" -> replenishQty = replenishQty.add(qty);
                default -> {
                    // 未知作业类型忽略
                }
            }
            if (r.getDurationMin() != null) totalDuration += r.getDurationMin();
            if (r.getErrorCount() != null) totalError += r.getErrorCount();
            if (r.getQualityScore() != null) {
                totalQuality = totalQuality.add(r.getQualityScore());
                qualityCount++;
            }
        }

        BigDecimal totalQty =
                receivingQty
                        .add(putawayQty)
                        .add(pickingQty)
                        .add(checkingQty)
                        .add(packingQty)
                        .add(countingQty)
                        .add(movingQty)
                        .add(vasQty)
                        .add(replenishQty);
        BigDecimal workHours =
                new BigDecimal(totalDuration).divide(new BigDecimal(60), 2, RoundingMode.HALF_UP);
        BigDecimal efficiency =
                workHours.compareTo(BigDecimal.ZERO) > 0
                        ? totalQty.divide(workHours, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
        BigDecimal errorRate =
                totalQty.compareTo(BigDecimal.ZERO) > 0
                        ? new BigDecimal(totalError).divide(totalQty, 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
        BigDecimal avgQuality =
                qualityCount > 0
                        ? totalQuality.divide(new BigDecimal(qualityCount), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

        // 综合绩效分 = 效率分(40%) + 质量分(30%) + 工作量分(30%)
        BigDecimal efficiencyScore = calculateEfficiencyScore(efficiency, emp);
        BigDecimal qualityScore = avgQuality; // 0-100
        BigDecimal workloadScore = calculateWorkloadScore(totalQty, emp);
        BigDecimal performanceScore =
                efficiencyScore
                        .multiply(new BigDecimal("0.4"))
                        .add(qualityScore.multiply(new BigDecimal("0.3")))
                        .add(workloadScore.multiply(new BigDecimal("0.3")))
                        .setScale(2, RoundingMode.HALF_UP);

        PerformanceLevel level = PerformanceLevel.ofScore(performanceScore);

        // 当日工资 = 时薪*工时 + 计件单价*作业量
        BigDecimal salary = BigDecimal.ZERO;
        if (emp.getHourlyRate() != null) {
            salary = salary.add(emp.getHourlyRate().multiply(workHours));
        }
        if (emp.getPieceRate() != null) {
            salary = salary.add(emp.getPieceRate().multiply(totalQty));
        }

        // 保存或更新日报
        PerformanceDaily daily = dailyMapper.selectByEmployeeAndDate(employeeId, date);
        if (daily == null) {
            daily = new PerformanceDaily();
            daily.setEmployeeId(employeeId);
            daily.setEmployeeNo(emp.getEmployeeNo());
            daily.setEmployeeName(emp.getEmployeeName());
            daily.setWarehouseCode(emp.getWarehouseCode());
            daily.setWorkDate(date);
            daily.setOwnerCodeCol(emp.getOwnerCodeCol());
            daily.setWarehouseCodeCol(emp.getWarehouseCodeCol());
        }
        daily.setWorkHours(workHours);
        daily.setReceivingQty(receivingQty);
        daily.setPutawayQty(putawayQty);
        daily.setPickingQty(pickingQty);
        daily.setCheckingQty(checkingQty);
        daily.setPackingQty(packingQty);
        daily.setCountingQty(countingQty);
        daily.setMovingQty(movingQty);
        daily.setVasQty(vasQty);
        daily.setReplenishQty(replenishQty);
        daily.setTotalQty(totalQty);
        daily.setTotalDuration(totalDuration);
        daily.setEfficiency(efficiency);
        daily.setErrorCount(totalError);
        daily.setErrorRate(errorRate);
        daily.setQualityScore(avgQuality);
        daily.setPerformanceScore(performanceScore);
        daily.setPerformanceLevel(level.getCode());
        daily.setSalaryAmount(salary);

        if (daily.getId() == null) {
            dailyMapper.insert(daily);
        } else {
            dailyMapper.updateById(daily);
        }

        log.info(
                "计算绩效日报: 人员={}, 日期={}, 总分={}, 等级={}",
                emp.getEmployeeNo(),
                date,
                performanceScore,
                level);
        return daily;
    }

    /** 效率评分: 与目标效率对比 */
    private BigDecimal calculateEfficiencyScore(BigDecimal efficiency, Employee emp) {
        // 简化: 拣货效率目标100件/小时, 达到目标100分, 每低10件扣10分
        BigDecimal target = new BigDecimal("100");
        if (efficiency.compareTo(target) >= 0) return new BigDecimal("100");
        BigDecimal score =
                efficiency.divide(target, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    /** 工作量评分: 与目标工作量对比 */
    private BigDecimal calculateWorkloadScore(BigDecimal totalQty, Employee emp) {
        Integer target = emp.getTargetPicking() != null ? emp.getTargetPicking() : 500;
        if (totalQty.compareTo(new BigDecimal(target)) >= 0) return new BigDecimal("100");
        BigDecimal score =
                totalQty.divide(new BigDecimal(target), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================

    // 4. 绩效考核 (月度/季度/年度)
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public PerformanceAssess generateAssess(Long employeeId, String period, String type) {
        Employee emp = getEmployee(employeeId);

        // 查询期间内的日报
        LocalDate startDate = parsePeriodStart(period, type);
        LocalDate endDate = parsePeriodEnd(period, type);
        List<PerformanceDaily> dailies =
                dailyMapper.selectList(
                        new LambdaQueryWrapper<PerformanceDaily>()
                                .eq(PerformanceDaily::getEmployeeId, employeeId)
                                .between(PerformanceDaily::getWorkDate, startDate, endDate));

        // 汇总
        int workDays = dailies.size();
        BigDecimal totalHours = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalEfficiency = BigDecimal.ZERO;
        BigDecimal totalErrorRate = BigDecimal.ZERO;
        BigDecimal totalQuality = BigDecimal.ZERO;
        BigDecimal totalSalary = BigDecimal.ZERO;

        for (PerformanceDaily d : dailies) {
            if (d.getWorkHours() != null) totalHours = totalHours.add(d.getWorkHours());
            if (d.getTotalQty() != null) totalQty = totalQty.add(d.getTotalQty());
            if (d.getEfficiency() != null) totalEfficiency = totalEfficiency.add(d.getEfficiency());
            if (d.getErrorRate() != null) totalErrorRate = totalErrorRate.add(d.getErrorRate());
            if (d.getQualityScore() != null) totalQuality = totalQuality.add(d.getQualityScore());
            if (d.getSalaryAmount() != null) totalSalary = totalSalary.add(d.getSalaryAmount());
        }

        BigDecimal avgEfficiency =
                workDays > 0
                        ? totalEfficiency.divide(new BigDecimal(workDays), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
        BigDecimal avgErrorRate =
                workDays > 0
                        ? totalErrorRate.divide(new BigDecimal(workDays), 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
        BigDecimal avgQuality =
                workDays > 0
                        ? totalQuality.divide(new BigDecimal(workDays), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

        // 出勤分(简化: 满勤100, 每缺1天扣5分)
        int expectedDays = calculateExpectedDays(startDate, endDate);
        BigDecimal attendanceScore =
                new BigDecimal(Math.max(0, 100 - (expectedDays - workDays) * 5));

        // 综合绩效分
        BigDecimal efficiencyScore = calculateEfficiencyScore(avgEfficiency, emp);
        BigDecimal performanceScore =
                efficiencyScore
                        .multiply(new BigDecimal("0.35"))
                        .add(avgQuality.multiply(new BigDecimal("0.25")))
                        .add(attendanceScore.multiply(new BigDecimal("0.2")))
                        .add(
                                new BigDecimal("100")
                                        .subtract(avgErrorRate.multiply(new BigDecimal("1000")))
                                        .multiply(new BigDecimal("0.2")))
                        .setScale(2, RoundingMode.HALF_UP);

        PerformanceLevel level = PerformanceLevel.ofScore(performanceScore);

        // 部门排名
        List<Employee> deptEmps = employeeMapper.selectActiveByDepartment(emp.getDepartment());
        int rank = 1;
        // 简化排名逻辑

        PerformanceAssess assess = new PerformanceAssess();
        assess.setAssessNo(generateAssessNo());
        assess.setEmployeeId(employeeId);
        assess.setEmployeeNo(emp.getEmployeeNo());
        assess.setEmployeeName(emp.getEmployeeName());
        assess.setWarehouseCode(emp.getWarehouseCode());
        assess.setAssessPeriod(period);
        assess.setAssessType(type);
        assess.setWorkDays(workDays);
        assess.setTotalHours(totalHours);
        assess.setTotalQty(totalQty);
        assess.setAvgEfficiency(avgEfficiency);
        assess.setErrorRate(avgErrorRate);
        assess.setQualityScore(avgQuality);
        assess.setAttendanceScore(attendanceScore);
        assess.setPerformanceScore(performanceScore);
        assess.setPerformanceLevel(level.getCode());
        assess.setRankInDept(rank);
        assess.setTotalEmployees(deptEmps.size());
        assess.setSalaryAmount(totalSalary);
        assess.setStatus("DRAFT");
        assess.setOwnerCodeCol(emp.getOwnerCodeCol());
        assess.setWarehouseCodeCol(emp.getWarehouseCodeCol());
        assessMapper.insert(assess);

        log.info(
                "生成绩效考核: {}, 人员={}, 期间={}, 总分={}, 等级={}",
                assess.getAssessNo(),
                emp.getEmployeeNo(),
                period,
                performanceScore,
                level);
        return assess;
    }

    @Transactional(rollbackFor = Exception.class)
    public PerformanceAssess confirmAssess(Long assessId, String assessBy) {
        PerformanceAssess assess = assessMapper.selectById(assessId);
        if (assess == null) throw new BizException("考核单不存在");
        if (!"DRAFT".equals(assess.getStatus())) {
            throw new BizException("考核单状态不允许确认: " + assess.getStatus());
        }
        assess.setStatus("CONFIRMED");
        assess.setAssessBy(assessBy);
        assess.setAssessTime(LocalDateTime.now());
        assessMapper.updateById(assess);
        log.info("考核单{}确认", assess.getAssessNo());
        return assess;
    }

    @Transactional(rollbackFor = Exception.class)
    public PerformanceAssess publishAssess(Long assessId) {
        PerformanceAssess assess = assessMapper.selectById(assessId);
        if (assess == null) throw new BizException("考核单不存在");
        if (!"CONFIRMED".equals(assess.getStatus())) {
            throw new BizException("考核单状态不允许发布: " + assess.getStatus());
        }
        assess.setStatus("PUBLISHED");
        assessMapper.updateById(assess);
        log.info("考核单{}发布", assess.getAssessNo());
        return assess;
    }

    // ============================================================

    // 5. 查询
    // ============================================================

    public PerformanceDaily getDailyPerformance(Long employeeId, LocalDate date) {
        return dailyMapper.selectByEmployeeAndDate(employeeId, date);
    }

    public List<PerformanceDaily> getDailyRanking(LocalDate date, String warehouse) {
        return dailyMapper.selectByDateAndWarehouse(date, warehouse);
    }

    public List<PerformanceAssess> getAssessRecords(Long employeeId) {
        return assessMapper.selectByEmployeeId(employeeId);
    }

    public List<PerformanceAssess> getAssessRanking(String period, String warehouse) {
        return assessMapper.selectByPeriodAndWarehouse(period, warehouse);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateRecordNo() {
        return "WR"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", RECORD_SEQ.incrementAndGet() % 1000);
    }

    private String generateAssessNo() {
        return "PA"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", ASSESS_SEQ.incrementAndGet() % 1000);
    }

    private LocalDate parsePeriodStart(String period, String type) {
        if ("MONTHLY".equals(type)) {
            return LocalDate.parse(period + "01", DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        if ("QUARTERLY".equals(type)) {
            int year = Integer.parseInt(period.substring(0, 4));
            int q = Integer.parseInt(period.substring(5));
            return LocalDate.of(year, (q - 1) * 3 + 1, 1);
        }
        return LocalDate.parse(period + "0101", DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private LocalDate parsePeriodEnd(String period, String type) {
        LocalDate start = parsePeriodStart(period, type);
        if ("MONTHLY".equals(type)) return start.plusMonths(1).minusDays(1);
        if ("QUARTERLY".equals(type)) return start.plusMonths(3).minusDays(1);
        return start.plusYears(1).minusDays(1);
    }

    private int calculateExpectedDays(LocalDate start, LocalDate end) {
        // 简化: 期间天数
        return (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
    }
}
