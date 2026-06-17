package com.medicare.controller;

import com.medicare.auth.RequireRole;
import com.medicare.dto.Result;
import com.medicare.entity.Department;
import com.medicare.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 科室控制器 — 科室 CRUD
 * <p>
 * 科室是医生和排班的顶层依赖，名称唯一；删除前需确认无关联医生
 */
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @RequireRole({"admin", "doctor"})
    public Result<List<Department>> list() {
        return Result.ok(departmentService.findAll());
    }

    @GetMapping("/{id}")
    @RequireRole({"admin", "doctor"})
    public Result<Department> detail(@PathVariable Long id) {
        return Result.ok(departmentService.findById(id));
    }

    @PostMapping
    @RequireRole("admin")
    public Result<Department> create(@Valid @RequestBody Department department) {
        return Result.ok(departmentService.create(department));
    }

    @PutMapping("/{id}")
    @RequireRole("admin")
    public Result<Department> update(@PathVariable Long id, @Valid @RequestBody Department department) {
        return Result.ok(departmentService.update(id, department));
    }

    @DeleteMapping("/{id}")
    @RequireRole("admin")
    public Result<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return Result.ok();
    }
}
