package com.xwms.base.master.controller;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.master.entity.Owner;
import com.xwms.base.master.mapper.OwnerMapper;
import com.xwms.common.core.PageResult;
import com.xwms.common.core.Result;

import lombok.RequiredArgsConstructor;

/** 货主档案Controller 3PL模式下多货主管理 */
@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerMapper ownerMapper;

    @PostMapping
    public Result<Owner> create(@RequestBody Owner owner) {
        ownerMapper.insert(owner);
        return Result.success(owner);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Owner owner) {
        owner.setId(id);
        ownerMapper.updateById(owner);
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageResult<Owner>> page(
            @RequestParam(required = false) String ownerCode,
            @RequestParam(required = false) String ownerName,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<Owner> page =
                ownerMapper.selectPage(
                        new Page<>(pageNum, pageSize),
                        new LambdaQueryWrapper<Owner>()
                                .like(ownerCode != null, Owner::getOwnerCode, ownerCode)
                                .like(ownerName != null, Owner::getOwnerName, ownerName)
                                .eq(industry != null, Owner::getIndustry, industry)
                                .eq(status != null, Owner::getStatus, status)
                                .orderByDesc(Owner::getCreatedAt));
        return Result.success(
                PageResult.of(
                        page.getRecords(),
                        page.getTotal(),
                        (int) page.getCurrent(),
                        (int) page.getSize()));
    }

    @GetMapping("/list")
    public Result<java.util.List<Owner>> listAll() {
        return Result.success(
                ownerMapper.selectList(
                        new LambdaQueryWrapper<Owner>().eq(Owner::getStatus, "ACTIVE")));
    }
}
