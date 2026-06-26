package com.medicare.controller;

import com.medicare.auth.AuthInterceptor;
import com.medicare.dto.KnowledgeDocumentContentResponse;
import com.medicare.dto.KnowledgeDocumentResponse;
import com.medicare.dto.KnowledgeSystemUploadBatchResponse;
import com.medicare.dto.KnowledgeUploadResponse;
import com.medicare.dto.Result;
import com.medicare.entity.KnowledgeDocument;
import com.medicare.entity.SysUser;
import com.medicare.exception.BusinessException;
import com.medicare.repository.KnowledgeDocumentRepository;
import com.medicare.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库管理", description = "知识库文档上传、管理、索引与预览")
public class KnowledgeController {

    private final RagService ragService;
    private final KnowledgeDocumentRepository documentRepository;

    @GetMapping("/documents")
    @Operation(summary = "查询已上传知识库文档")
    public Result<List<KnowledgeDocumentResponse>> listDocuments(HttpServletRequest request) {
        SysUser currentUser = getCurrentUser(request);
        assertKnowledgeAccess(currentUser);
        return Result.ok(ragService.listDocuments());
    }

    @GetMapping("/documents/all")
    @Operation(summary = "查询所有知识库文档")
    public Result<List<KnowledgeDocumentResponse>> listAllDocuments(HttpServletRequest request) {
        SysUser currentUser = getCurrentUser(request);
        assertKnowledgeAccess(currentUser);
        return Result.ok(ragService.listAllDocuments());
    }

    @GetMapping("/documents/{id}")
    @Operation(summary = "获取知识库文档详情及内容预览")
    public Result<KnowledgeDocumentContentResponse> getDocument(@PathVariable Long id, HttpServletRequest request) {
        SysUser currentUser = getCurrentUser(request);
        assertKnowledgeAccess(currentUser);
        KnowledgeDocumentContentResponse document = ragService.getDocument(id);
        assertDocumentManageable(currentUser, document.getUploadedBy(), document.getIsSystem());
        return Result.ok(document);
    }

    @GetMapping("/documents/{id}/preview")
    @Operation(summary = "获取知识库文档详情及内容预览（登录用户，供 AI 助手引用查看）")
    public Result<KnowledgeDocumentContentResponse> previewDocument(@PathVariable Long id, HttpServletRequest request) {
        SysUser currentUser = getCurrentUser(request);
        assertAuthenticated(currentUser);
        // AI 助手引用预览不区分文档所有者，所有登录用户均可查看，确保 RAG 能检索全部知识库文件
        return Result.ok(ragService.getDocument(id));
    }

    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文档并写入 RAG 知识库")
    public Result<KnowledgeUploadResponse> uploadDocument(@RequestPart("file") MultipartFile file,
                                                          HttpServletRequest request) {
        SysUser currentUser = getCurrentUser(request);
        assertKnowledgeAccess(currentUser);
        return Result.ok(ragService.uploadDocument(file, currentUser.getId()));
    }

    @PostMapping(value = "/documents/assistant-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "AI 助手上传文件并写入 RAG 知识库")
    public Result<KnowledgeUploadResponse> uploadDocumentForAssistant(@RequestPart("file") MultipartFile file,
                                                                      HttpServletRequest request) {
        SysUser currentUser = getCurrentUser(request);
        assertAuthenticated(currentUser);
        return Result.ok(ragService.uploadDocumentForAssistant(file, currentUser.getId()));
    }

    @PutMapping("/documents/{id}")
    @Operation(summary = "更新知识库文档内容并重新索引")
    public Result<KnowledgeDocumentResponse> updateDocument(@PathVariable Long id,
                                                             @RequestBody Map<String, String> body,
                                                             HttpServletRequest request) {
        SysUser currentUser = getCurrentUser(request);
        assertKnowledgeAccess(currentUser);
        KnowledgeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));
        boolean isSystem = ragService.isSystemSourcePath(document.getSourcePath());
        assertDocumentManageable(currentUser, document.getUploadedBy(), isSystem);
        String content = body.get("content");
        return Result.ok(ragService.updateDocument(id, content, isMainAdmin(currentUser)));
    }

    @DeleteMapping("/documents/{id}")
    @Operation(summary = "删除知识库文档及其分块")
    public Result<Void> deleteDocument(@PathVariable Long id, HttpServletRequest request) {
        SysUser currentUser = getCurrentUser(request);
        assertKnowledgeAccess(currentUser);
        KnowledgeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));
        boolean isSystem = ragService.isSystemSourcePath(document.getSourcePath());
        assertDocumentManageable(currentUser, document.getUploadedBy(), isSystem);
        ragService.deleteDocument(id, isMainAdmin(currentUser));
        return Result.ok();
    }

    @DeleteMapping("/documents/system/clear")
    @Operation(summary = "清空所有系统文件及已索引的向量数据（主管理员）")
    public Result<Void> clearSystemDocuments(HttpServletRequest request) {
        SysUser currentUser = getCurrentUser(request);
        assertMainAdmin(currentUser);
        ragService.clearSystemDocuments();
        return Result.ok();
    }

    @PostMapping(value = "/documents/system/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传系统文件并写入 RAG 知识库（支持单个/多个文件和文件夹批量上传，主管理员）")
    public Result<KnowledgeSystemUploadBatchResponse> uploadSystemDocument(HttpServletRequest request) {
        SysUser currentUser = getCurrentUser(request);
        assertMainAdmin(currentUser);
        if (!(request instanceof MultipartHttpServletRequest multipartRequest)) {
            throw new BusinessException(400, "请选择要上传的系统文件");
        }
        List<MultipartFile> files = new ArrayList<>(multipartRequest.getFiles("files"));
        if (files.isEmpty()) {
            files.addAll(multipartRequest.getFiles("file"));
        }
        return Result.ok(ragService.uploadSystemDocuments(files, resolveProjectRoot(), currentUser.getId()));
    }

    private SysUser getCurrentUser(HttpServletRequest request) {
        return AuthInterceptor.getCurrentUser(request);
    }

    private void assertAuthenticated(SysUser currentUser) {
        if (currentUser == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }
    }

    private void assertKnowledgeAccess(SysUser currentUser) {
        if (currentUser == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        if (!currentUser.isAdmin() && !currentUser.isDoctor() && !currentUser.isPharmacist()) {
            throw new BusinessException(403, "无权访问知识库");
        }
    }

    private void assertMainAdmin(SysUser currentUser) {
        if (currentUser == null) {
            throw new BusinessException(401, "未登录，请先登录");
        }
        if (!isMainAdmin(currentUser)) {
            throw new BusinessException(403, "只有主管理员可以操作系统文件");
        }
    }

    private void assertDocumentManageable(SysUser currentUser, Long uploadedBy, boolean isSystem) {
        if (isMainAdmin(currentUser)) {
            return;
        }
        if (currentUser.isAdmin()) {
            if (isSystem) {
                throw new BusinessException(403, "系统文件不可编辑或删除");
            }
            return;
        }
        if (isSystem || !currentUser.getId().equals(uploadedBy)) {
            throw new BusinessException(403, "只能操作自己上传的文档");
        }
    }

    private boolean isMainAdmin(SysUser user) {
        return user != null && user.getId() != null && user.getId() == 1L;
    }

    private Path resolveProjectRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if (current.getFileName() != null && "medicare-server".equals(current.getFileName().toString())) {
            Path parent = current.getParent();
            return parent == null ? current : parent;
        }
        if (current.resolve("medicare-server").toFile().exists()) {
            return current;
        }
        Path parent = current.getParent();
        return parent == null ? current : parent;
    }
}
