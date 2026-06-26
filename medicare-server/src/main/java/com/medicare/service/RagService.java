package com.medicare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.medicare.config.AiProperties;
import com.medicare.config.AiTaskExecutorConfig.AiTaskExecutors;
import com.medicare.dto.AiChatResponse;
import com.medicare.dto.KnowledgeDocumentContentResponse;
import org.springframework.util.StringUtils;
import com.medicare.dto.KnowledgeDocumentResponse;
import com.medicare.dto.KnowledgeSystemUploadBatchResponse;
import com.medicare.dto.KnowledgeUploadResponse;
import com.medicare.dto.RagQueryRequest;
import com.medicare.dto.RagReindexResponse;
import com.medicare.dto.VectorRecord;
import com.medicare.entity.KnowledgeChunk;
import com.medicare.entity.KnowledgeDocument;
import com.medicare.exception.BusinessException;
import com.medicare.repository.KnowledgeChunkRepository;
import com.medicare.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private static final int CHUNK_SIZE = 1800;
    private static final int CHUNK_OVERLAP = 240;
    private static final String INDEX_VERSION = "rag-v3-bailian-embedding-v4";
    private static final String USER_UPLOAD_PREFIX = "uploads/";
    private static final String ASSISTANT_UPLOAD_PREFIX = "assistant-uploads/";
    private static final String SYSTEM_UPLOAD_PREFIX = "system-uploads/";
    private static final Pattern SYSTEM_UPLOAD_TITLE_PATTERN = Pattern.compile("^SYS-?(\\d{3})\\s+-\\s+.*");

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final AiProperties aiProperties;
    private final RestClient.Builder restClientBuilder;
    private final DocumentTextExtractionService documentTextExtractionService;
    private final LiveBusinessContextService liveBusinessContextService;
    private final AiTaskExecutors aiTaskExecutors;
    private final VectorStoreService vectorStoreService;
    private final TransactionTemplate transactionTemplate;

    @Transactional(readOnly = true)
    public List<KnowledgeDocumentResponse> listDocuments() {
        return documentRepository.findTop20ByOrderByUpdateTimeDesc().stream()
                .map(this::toDocumentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocumentResponse> listAllDocuments() {
        return documentRepository.findByStatus(1).stream()
                .map(this::toDocumentResponse)
                .toList();
    }

    private KnowledgeDocumentResponse toDocumentResponse(KnowledgeDocument document) {
        return KnowledgeDocumentResponse.builder()
                .id(document.getId())
                .filename(document.getTitle())
                .sourcePath(document.getSourcePath())
                .sourceType(document.getSourceType())
                .chunkCount(document.getChunkCount())
                .status(document.getStatus())
                .isSystem(isSystemSourcePath(document.getSourcePath()))
                .uploadedBy(document.getUploadedBy())
                .createTime(document.getCreateTime())
                .updateTime(document.getUpdateTime())
                .build();
    }

    @Transactional(readOnly = true)
    public KnowledgeDocumentContentResponse getDocument(Long id) {
        KnowledgeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));
        String content = buildDocumentContent(document.getId());
        boolean isSystem = isSystemSourcePath(document.getSourcePath());
        return KnowledgeDocumentContentResponse.builder()
                .id(document.getId())
                .filename(document.getTitle())
                .sourcePath(document.getSourcePath())
                .sourceType(document.getSourceType())
                .content(content)
                .chunkCount(document.getChunkCount())
                .status(document.getStatus())
                .isSystem(isSystem)
                .uploadedBy(document.getUploadedBy())
                .createTime(document.getCreateTime())
                .updateTime(document.getUpdateTime())
                .build();
    }

    @Transactional
    public void deleteDocument(Long id, boolean allowSystem) {
        KnowledgeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));
        boolean isSystem = isSystemSourcePath(document.getSourcePath());
        if (isSystem && !allowSystem) {
            throw new BusinessException(403, "系统内置文档不可删除");
        }
        List<KnowledgeChunk> chunks = chunkRepository.findByDocumentId(document.getId());
        List<String> vectorIds = chunks.stream().map(KnowledgeChunk::getVectorId).filter(Objects::nonNull).toList();
        chunkRepository.deleteByDocumentId(document.getId());
        documentRepository.delete(document);
        if (!vectorIds.isEmpty()) {
            try {
                vectorStoreService.deleteVectors(vectorIds);
            } catch (Exception e) {
                log.warn("删除文档向量失败，已保留关系库数据: id={}, {}", id, e.getMessage());
            }
        }
        log.info("知识库文档已删除: id={}, title={}", id, document.getTitle());
    }

    public KnowledgeDocumentResponse updateDocument(Long id, String content, boolean allowSystem) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(400, "文档内容不能为空");
        }
        KnowledgeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "文档不存在"));
        String sourcePath = document.getSourcePath();
        boolean isSystem = isSystemSourcePath(sourcePath);
        if (isSystem && !allowSystem) {
            throw new BusinessException(403, "系统内置文档不可编辑");
        }
        String sourceType = document.getSourceType();
        String title = document.getTitle();
        try {
            if (isSystem) {
                writeSystemSourceFile(sourcePath, content);
            }
            IndexResult result = indexContent(title, sourcePath, sourceType, content, true, document.getUploadedBy());
            return KnowledgeDocumentResponse.builder()
                    .id(document.getId())
                    .filename(title)
                    .sourcePath(sourcePath)
                    .sourceType(sourceType)
                    .chunkCount(result.chunkCount())
                    .status(1)
                    .updateTime(document.getUpdateTime())
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "文档更新失败：" + e.getMessage());
        }
    }

    private void writeSystemSourceFile(String sourcePath, String content) {
        Path projectRoot = resolveProjectRoot();
        Path file = projectRoot.resolve(sourcePath);
        try {
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException(500, "写入系统源文件失败：" + e.getMessage());
        }
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

    private String buildDocumentContent(Long documentId) {
        List<KnowledgeChunk> chunks = chunkRepository.findByDocumentId(documentId);
        if (chunks.isEmpty()) {
            return "";
        }
        chunks.sort(Comparator.comparingInt(KnowledgeChunk::getChunkIndex));
        StringBuilder sb = new StringBuilder();
        for (KnowledgeChunk chunk : chunks) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append(chunk.getContent());
        }
        return sb.toString();
    }

    /**
     * 清空所有系统文件（含已索引的向量数据），保留用户上传文档。
     */
    public void clearSystemDocuments() {
        List<KnowledgeChunk> systemChunks;
        Set<String> systemSourcePaths = new LinkedHashSet<>();
        try {
            systemChunks = chunkRepository.findAllSystemChunks();
            systemSourcePaths.addAll(chunkRepository.findAllSystemSourcePaths());
            systemSourcePaths.addAll(documentRepository.findAllSystemSourcePaths());
        } catch (Exception e) {
            log.error("读取系统文档索引失败: {}", e.getMessage(), e);
            throw new BusinessException(500, "读取系统文件索引失败：" + e.getMessage());
        }

        List<String> vectorIds = systemChunks.stream()
                .map(KnowledgeChunk::getVectorId)
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .distinct()
                .toList();
        deleteSystemVectors(vectorIds, systemSourcePaths);

        try {
            transactionTemplate.executeWithoutResult(status -> {
                int chunks = chunkRepository.deleteAllSystemChunks();
                int docs = documentRepository.deleteAllSystemDocuments();
                log.info("已删除系统文档 {} 条、分块 {} 条", docs, chunks);
            });
        } catch (Exception e) {
            log.error("清空关系库系统文档失败: {}", e.getMessage(), e);
            throw new BusinessException(500, "清空系统文件失败：" + e.getMessage());
        }
        log.info("已清空所有系统知识库文档及向量索引");
    }

    private void deleteSystemVectors(List<String> vectorIds, Collection<String> sourcePaths) {
        if (!vectorIds.isEmpty()) {
            try {
                vectorStoreService.deleteVectors(vectorIds);
            } catch (Exception e) {
                log.warn("按 vectorId 删除系统向量失败，继续按 sourcePath 清理: {}", e.getMessage());
            }
        }
        List<String> cleanedSourcePaths = sourcePaths.stream()
                .filter(Objects::nonNull)
                .filter(path -> !path.isBlank())
                .distinct()
                .toList();
        if (!cleanedSourcePaths.isEmpty()) {
            try {
                vectorStoreService.deleteVectorsBySourcePaths(cleanedSourcePaths);
            } catch (Exception e) {
                log.warn("按 sourcePath 删除系统向量失败，继续清空关系库系统索引: {}", e.getMessage());
            }
        }
    }

    public RagReindexResponse reindex(Path projectRoot, Long uploadedBy) {
        // 1. 清空向量数据库与关系库中的旧系统文档
        clearSystemDocuments();

        List<Path> files = collectKnowledgeFiles(projectRoot);

        // 2. 文件级索引保持顺序执行，避免占满 vectorExecutor 后内部 embedding 任务无可用线程。
        // 系统文件统一编号 SYS-001 起，与上传系统文件格式保持一致。
        int documents = 0;
        int chunks = 0;
        int sequence = 1;
        List<String> failures = new ArrayList<>();
        for (Path file : files) {
            try {
                IndexResult result = indexSystemFile(projectRoot, file, sequence++, uploadedBy);
                if (result.updated()) documents++;
                chunks += result.chunkCount();
            } catch (Exception e) {
                failures.add(file.getFileName() + ": " + e.getMessage());
                log.warn("知识库文档处理失败: {}", file, e);
            }
        }

        if (!failures.isEmpty()) {
            throw new BusinessException(500, "知识库重建失败，失败文件数 " + failures.size()
                    + "：" + String.join("；", failures.stream().limit(3).toList()));
        }

        return RagReindexResponse.builder()
                .documentCount(documents)
                .chunkCount(chunks)
                .message("知识库重建完成")
                .build();
    }

    private IndexResult indexSystemFile(Path projectRoot, Path file, int sequence, Long uploadedBy) {
        try {
            String content = normalizeContent(file, Files.readString(file, StandardCharsets.UTF_8));
            if (content.isBlank()) {
                return new IndexResult(false, 0);
            }
            String sourcePath = projectRoot.relativize(file).toString();
            String basename = file.getFileName().toString();
            String title = String.format("SYS-%03d - %s", sequence, basename);
            return indexContent(title, sourcePath, resolveSourceType(file), content, true, uploadedBy);
        } catch (Exception e) {
            throw new RuntimeException("知识库文档处理失败: " + file, e);
        }
    }

    public KnowledgeUploadResponse uploadDocument(MultipartFile file, Long uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的知识库文档");
        }
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("document").trim();
        if (filename.isBlank()) {
            filename = "document";
        }
        String content = normalizeUploadedContent(documentTextExtractionService.extractText(file));
        if (content.isBlank()) {
            throw new BusinessException(400, "文档中未解析到可用于知识库的文本内容");
        }
        String sourceType = documentTextExtractionService.resolveSourceType(filename);
        String sourcePath = "uploads/" + System.currentTimeMillis() + "-" + sanitizeFilename(filename);
        try {
            IndexResult result = indexContent(filename, sourcePath, sourceType, content, true, uploadedBy);
            return KnowledgeUploadResponse.builder()
                    .filename(filename)
                    .sourcePath(sourcePath)
                    .sourceType(sourceType)
                    .chunkCount(result.chunkCount())
                    .message("文档上传并写入知识库成功")
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "文档写入知识库失败：" + e.getMessage());
        }
    }

    public KnowledgeUploadResponse uploadDocumentForAssistant(MultipartFile file, Long uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的文件");
        }
        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("document").trim();
        if (filename.isBlank()) {
            filename = "document";
        }
        String content = normalizeUploadedContent(documentTextExtractionService.extractText(file));
        if (content.isBlank()) {
            throw new BusinessException(400, "文件中未解析到可用于检索的文本内容");
        }
        String sourceType = documentTextExtractionService.resolveSourceType(filename);
        String sourcePath = "assistant-uploads/" + System.currentTimeMillis() + "-" + sanitizeFilename(filename);
        try {
            IndexResult result = indexContent(filename, sourcePath, sourceType, content, true, uploadedBy);
            return KnowledgeUploadResponse.builder()
                    .filename(filename)
                    .sourcePath(sourcePath)
                    .sourceType(sourceType)
                    .chunkCount(result.chunkCount())
                    .message("文件解析完成，已可用于 AI 问答")
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "文件写入知识库失败：" + e.getMessage());
        }
    }

    public KnowledgeUploadResponse uploadSystemDocument(MultipartFile file, Path projectRoot, Long uploadedBy) {
        return processSystemFile(file, projectRoot, nextSystemUploadSequence(), uploadedBy);
    }

    public KnowledgeSystemUploadBatchResponse uploadSystemDocuments(List<MultipartFile> files, Path projectRoot, Long uploadedBy) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的系统文件");
        }
        int sequence = nextSystemUploadSequence();
        List<KnowledgeUploadResponse> results = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        int totalChunkCount = 0;

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("document").trim();
            try {
                KnowledgeUploadResponse response = processSystemFile(file, projectRoot, sequence++, uploadedBy);
                results.add(response);
                successCount++;
                totalChunkCount += Optional.ofNullable(response.getChunkCount()).orElse(0);
            } catch (BusinessException e) {
                failCount++;
                results.add(KnowledgeUploadResponse.builder()
                        .filename(filename)
                        .message("上传失败：" + e.getMessage())
                        .build());
            } catch (Exception e) {
                failCount++;
                results.add(KnowledgeUploadResponse.builder()
                        .filename(filename)
                        .message("上传失败：" + e.getMessage())
                        .build());
            }
        }

        if (successCount == 0) {
            throw new BusinessException(400, "所有文件上传失败：" + results.get(0).getMessage());
        }

        String message = String.format("成功上传 %d 个系统文件，共 %d 个分块", successCount, totalChunkCount);
        if (failCount > 0) {
            message += String.format("，%d 个文件上传失败", failCount);
        }

        return KnowledgeSystemUploadBatchResponse.builder()
                .totalCount(results.size())
                .successCount(successCount)
                .failCount(failCount)
                .totalChunkCount(totalChunkCount)
                .message(message)
                .files(results)
                .build();
    }

    private KnowledgeUploadResponse processSystemFile(MultipartFile file, Path projectRoot, int sequence, Long uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的系统文件");
        }
        String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("document").trim();
        if (originalFilename.isBlank()) {
            originalFilename = "document";
        }
        // 文件夹上传时 originalFilename 可能包含相对路径，取 basename 作为展示标题
        String basename = Path.of(originalFilename).getFileName().toString();
        String content = normalizeUploadedContent(documentTextExtractionService.extractText(file));
        if (content.isBlank()) {
            throw new BusinessException(400, "文件中未解析到可用于检索的文本内容");
        }
        String sourceType = documentTextExtractionService.resolveSourceType(basename);

        Path uploadDir = projectRoot.resolve("system-uploads");
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new BusinessException(500, "创建系统文件目录失败：" + e.getMessage());
        }
        // 使用完整相对路径构造唯一保存名，避免不同文件夹下同名文件冲突
        String savedName = System.currentTimeMillis() + "-" + sanitizeFilename(originalFilename);
        Path savedFile = uploadDir.resolve(savedName);
        try {
            file.transferTo(savedFile);
        } catch (IOException e) {
            throw new BusinessException(500, "保存系统文件失败：" + e.getMessage());
        }

        String title = String.format("SYS-%03d - %s", sequence, basename);
        String sourcePath = projectRoot.relativize(savedFile).toString();
        try {
            IndexResult result = indexContent(title, sourcePath, sourceType, content, true, uploadedBy);
            return KnowledgeUploadResponse.builder()
                    .filename(title)
                    .sourcePath(sourcePath)
                    .sourceType(sourceType)
                    .chunkCount(result.chunkCount())
                    .message("系统文件上传并写入知识库成功")
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "系统文件写入知识库失败：" + e.getMessage());
        }
    }

    private int nextSystemUploadSequence() {
        int maxSequence = documentRepository.findByStatus(1).stream()
                .filter(document -> isSystemUploadDocument(document))
                .mapToInt(document -> extractSystemUploadSequence(document.getTitle()))
                .max()
                .orElse(0);
        return maxSequence + 1;
    }

    private boolean isSystemUploadDocument(KnowledgeDocument document) {
        String sourcePath = document.getSourcePath();
        String title = document.getTitle();
        return (sourcePath != null && sourcePath.startsWith(SYSTEM_UPLOAD_PREFIX))
                || (title != null && SYSTEM_UPLOAD_TITLE_PATTERN.matcher(title).matches());
    }

    private int extractSystemUploadSequence(String title) {
        if (title == null) {
            return 0;
        }
        Matcher matcher = SYSTEM_UPLOAD_TITLE_PATTERN.matcher(title);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    public boolean isSystemSourcePath(String sourcePath) {
        return sourcePath != null
                && !sourcePath.startsWith(USER_UPLOAD_PREFIX)
                && !sourcePath.startsWith(ASSISTANT_UPLOAD_PREFIX);
    }

    public AiChatResponse query(RagQueryRequest request) {
        int topK = request.getTopK() == null ? 5 : Math.max(1, Math.min(request.getTopK(), 8));
        List<KnowledgeChunk> chunks = retrieve(request.getQuestion(), topK);
        if (chunks.isEmpty()) {
            return AiChatResponse.builder()
                    .answer("知识库中暂未检索到相关向量数据。请先由管理员执行知识库重建，或换一个更具体的问题。")
                    .provider("rag")
                    .model(aiProperties.getEmbeddingModel())
                    .references(List.of())
                    .build();
        }

        String prompt = buildRagPrompt(request.getQuestion(), chunks);
        String answer = callModelAsync(prompt);
        return AiChatResponse.builder()
                .answer(answer)
                .provider(normalizeProvider())
                .model(aiProperties.getModel())
                .references(toReferences(chunks))
                .build();
    }

    /**
     * 判断当前是否已有向量索引数据。
     */
    @Transactional(readOnly = true)
    public boolean hasEmbeddingIndex() {
        return chunkRepository.countByVectorIdIsNotNull() > 0;
    }

    /**
     * 仅检索知识库，返回引用列表（供 AI 助手对话前置检索使用）。
     */
    @Transactional(readOnly = true)
    public List<AiChatResponse.AiReference> retrieveReferences(String question, String sourcePath, int topK) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }
        int limit = Math.max(1, Math.min(topK, 8));
        return toReferences(retrieve(question, sourcePath, limit));
    }

    private List<AiChatResponse.AiReference> toReferences(List<KnowledgeChunk> chunks) {
        return chunks.stream()
                .map(chunk -> AiChatResponse.AiReference.builder()
                        .type("document")
                        .id(String.valueOf(chunk.getDocumentId()))
                        .title(chunk.getTitle() + " #" + chunk.getChunkIndex())
                        .sourcePath(chunk.getSourcePath())
                        .content(abbreviate(chunk.getContent(), 600))
                        .build())
                .toList();
    }

    private String abbreviate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLen) + "…";
    }

    private List<Path> collectKnowledgeFiles(Path projectRoot) {
        // 仅将 systemRAGFiles 目录下的文件作为系统知识库文档
        Path root = projectRoot.resolve("systemRAGFiles");
        List<Path> files = new ArrayList<>();
        if (!Files.exists(root)) {
            return files;
        }
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(this::isSupportedDocument)
                    .forEach(files::add);
        } catch (IOException e) {
            log.warn("扫描知识库目录失败: {}", root, e);
        }
        return files.stream().distinct().sorted().toList();
    }

    private boolean isSupportedDocument(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".md")
                || name.endsWith(".html")
                || name.endsWith(".txt")
                || name.endsWith(".java")
                || name.endsWith(".ts")
                || name.endsWith(".vue")
                || name.endsWith(".yml")
                || name.endsWith(".yaml")
                || name.endsWith(".json");
    }

    private String normalizeContent(Path file, String raw) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        String text = raw;
        if (name.endsWith(".html")) {
            text = text.replaceAll("(?is)<script.*?</script>", " ")
                    .replaceAll("(?is)<style.*?</style>", " ")
                    .replaceAll("(?s)<[^>]+>", " ");
        }
        return text.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll(" {2,}", " ")
                .trim();
    }

    private String normalizeUploadedContent(String raw) {
        return raw == null ? "" : raw
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll(" {2,}", " ")
                .trim();
    }

    /**
     * 索引内容：先写入关系库（TransactionTemplate），再并行计算 embedding 并写入向量数据库。
     * 该方法可在多线程中被调用，每个调用拥有独立事务。
     */
    private IndexResult indexContent(String title, String sourcePath, String sourceType, String content, boolean forceUpdate, Long uploadedBy)
            throws Exception {
        String contentHash = sha256(INDEX_VERSION + "\n" + content);

        IndexContext context = transactionTemplate.execute(status -> {
            KnowledgeDocument document = documentRepository.findBySourcePath(sourcePath)
                    .orElseGet(KnowledgeDocument::new);
            if (!forceUpdate && document.getId() != null && contentHash.equals(document.getContentHash())) {
                return new IndexContext(document.getId(), false, List.of(), List.of(), List.of(), document.getChunkCount(), false);
            }

            boolean newDocument = document.getId() == null;
            document.setTitle(title);
            document.setSourcePath(sourcePath);
            document.setSourceType(sourceType);
            document.setContentHash(contentHash);
            document.setStatus(1);
            if (uploadedBy != null && newDocument) {
                document.setUploadedBy(uploadedBy);
            }
            document = documentRepository.save(document);
            Long documentId = document.getId();

            // 收集旧向量 ID 用于后续清理
            List<KnowledgeChunk> oldChunks = chunkRepository.findByDocumentId(document.getId());
            List<String> oldVectorIds = oldChunks.stream()
                    .map(KnowledgeChunk::getVectorId)
                    .filter(Objects::nonNull)
                    .toList();

            chunkRepository.deleteByDocumentId(document.getId());

            List<String> pieces = splitIntoChunks(content);
            List<ChunkDraft> drafts = new ArrayList<>(pieces.size());
            int index = 0;
            for (String piece : pieces) {
                String vectorId = UUID.randomUUID().toString();
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setDocumentId(document.getId());
                chunk.setChunkIndex(index++);
                chunk.setTitle(document.getTitle());
                chunk.setSourcePath(sourcePath);
                chunk.setContent(piece);
                chunk.setKeywords(extractKeywords(piece));
                chunk.setEmbeddingModel(aiProperties.getEmbeddingModel());
                chunk.setVectorId(vectorId);
                chunkRepository.save(chunk);
                drafts.add(new ChunkDraft(chunk, piece, vectorId));
            }
            document.setChunkCount(pieces.size());
            documentRepository.save(document);
            return new IndexContext(documentId, newDocument, oldChunks, oldVectorIds, drafts, pieces.size(), true);
        });

        if (context == null || !context.updated) {
            return new IndexResult(false, context != null ? context.chunkCount : 0);
        }

        try {
            // 并行计算各分块 embedding
            List<CompletableFuture<VectorRecord>> embeddingFutures = context.drafts.stream()
                    .map(draft -> CompletableFuture.supplyAsync(
                            () -> new VectorRecord(
                                    draft.vectorId,
                                    createEmbedding(draft.content),
                                    String.valueOf(draft.chunk.getDocumentId()),
                                    sourcePath,
                                    title,
                                    draft.chunk.getChunkIndex()
                            ),
                            aiTaskExecutors.vectorExecutor()
                    ))
                    .toList();

            List<VectorRecord> records = embeddingFutures.stream()
                    .map(this::joinFuture)
                    .collect(Collectors.toList());

            // 批量写入向量数据库
            vectorStoreService.upsertVectors(records);
        } catch (Exception e) {
            rollbackFailedIndex(context);
            throw e;
        }

        // 异步清理旧向量，失败不影响新索引
        if (!context.oldVectorIds.isEmpty()) {
            CompletableFuture.runAsync(
                    () -> {
                        try {
                            vectorStoreService.deleteVectors(context.oldVectorIds);
                        } catch (Exception e) {
                            log.warn("清理旧向量失败: {}", e.getMessage());
                        }
                    },
                    aiTaskExecutors.vectorExecutor()
            );
        }

        return new IndexResult(true, context.chunkCount);
    }

    private void rollbackFailedIndex(IndexContext context) {
        List<String> newVectorIds = context.drafts.stream()
                .map(ChunkDraft::vectorId)
                .filter(Objects::nonNull)
                .toList();
        if (!newVectorIds.isEmpty()) {
            try {
                vectorStoreService.deleteVectors(newVectorIds);
            } catch (Exception e) {
                log.warn("回滚失败索引时清理新向量失败: {}", e.getMessage());
            }
        }
        transactionTemplate.executeWithoutResult(status -> {
            chunkRepository.deleteByDocumentId(context.documentId);
            if (context.newDocument) {
                documentRepository.findById(context.documentId).ifPresent(documentRepository::delete);
                return;
            }
            for (KnowledgeChunk oldChunk : context.oldChunks) {
                KnowledgeChunk restored = new KnowledgeChunk();
                restored.setDocumentId(oldChunk.getDocumentId());
                restored.setChunkIndex(oldChunk.getChunkIndex());
                restored.setTitle(oldChunk.getTitle());
                restored.setContent(oldChunk.getContent());
                restored.setKeywords(oldChunk.getKeywords());
                restored.setVectorId(oldChunk.getVectorId());
                restored.setEmbeddingModel(oldChunk.getEmbeddingModel());
                restored.setSourcePath(oldChunk.getSourcePath());
                chunkRepository.save(restored);
            }
            documentRepository.findById(context.documentId).ifPresent(document -> {
                document.setChunkCount(context.oldChunks.size());
                documentRepository.save(document);
            });
        });
    }

    private record IndexResult(boolean updated, int chunkCount) {
    }

    private record ChunkDraft(KnowledgeChunk chunk, String content, String vectorId) {
    }

    private record IndexContext(
            Long documentId,
            boolean newDocument,
            List<KnowledgeChunk> oldChunks,
            List<String> oldVectorIds,
            List<ChunkDraft> drafts,
            int chunkCount,
            boolean updated
    ) {
    }

    private List<String> splitIntoChunks(String content) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(content.length(), start + CHUNK_SIZE);
            if (end < content.length()) {
                int newline = content.lastIndexOf('\n', end);
                if (newline > start + 300) {
                    end = newline;
                }
            }
            String chunk = content.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            if (end >= content.length()) {
                break;
            }
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
        return chunks;
    }

    private List<KnowledgeChunk> retrieve(String question, int topK) {
        return retrieve(question, null, topK);
    }

    private List<KnowledgeChunk> retrieve(String question, String sourcePath, int topK) {
        if (!vectorStoreService.isAvailable()) {
            return keywordRetrieve(question, sourcePath, topK);
        }

        List<Double> queryEmbedding = createEmbedding(question);
        List<String> vectorIds = vectorStoreService.search(queryEmbedding, sourcePath, topK * 2);
        if (!vectorIds.isEmpty()) {
            List<KnowledgeChunk> chunks = chunkRepository.findByVectorIdIn(vectorIds);
            // 按向量搜索返回的顺序排序
            Map<String, Integer> order = new HashMap<>();
            for (int i = 0; i < vectorIds.size(); i++) {
                order.putIfAbsent(vectorIds.get(i), i);
            }
            chunks.sort(Comparator.comparingInt(c -> order.getOrDefault(c.getVectorId(), Integer.MAX_VALUE)));
            return chunks.stream().limit(topK).toList();
        }

        return keywordRetrieve(question, sourcePath, topK);
    }

    private List<KnowledgeChunk> keywordRetrieve(String question, String sourcePath, int topK) {
        Set<String> terms = tokenize(question);
        Map<Long, KnowledgeChunk> chunkMap = new LinkedHashMap<>();
        Map<Long, Integer> scoreMap = new HashMap<>();
        for (String term : terms) {
            List<KnowledgeChunk> matches = chunkRepository.searchByKeyword(term, 20);
            for (KnowledgeChunk chunk : matches) {
                if (StringUtils.hasText(sourcePath) && !sourcePath.equals(chunk.getSourcePath())) {
                    continue;
                }
                chunkMap.put(chunk.getId(), chunk);
                scoreMap.merge(chunk.getId(), scoreChunk(chunk, term, question), Integer::sum);
            }
        }
        return chunkMap.values().stream()
                .sorted((a, b) -> Integer.compare(scoreMap.getOrDefault(b.getId(), 0), scoreMap.getOrDefault(a.getId(), 0)))
                .limit(topK)
                .toList();
    }

    private Set<String> tokenize(String question) {
        String normalized = question.toLowerCase(Locale.ROOT).replaceAll("[^\\p{IsHan}a-z0-9]+", " ");
        Set<String> terms = new LinkedHashSet<>();
        for (String part : normalized.split("\\s+")) {
            if (part.length() >= 2) {
                terms.add(part);
            }
            if (part.matches(".*\\p{IsHan}.*") && part.length() > 4) {
                for (int i = 0; i <= part.length() - 2; i++) {
                    terms.add(part.substring(i, i + 2));
                }
            }
        }
        if (question.contains("患者")) {
            terms.add("Patient");
            terms.add("patients");
        }
        if (question.contains("接口")) {
            terms.add("Controller");
            terms.add("RequestMapping");
            terms.add("GetMapping");
            terms.add("PostMapping");
            terms.add("PutMapping");
            terms.add("DeleteMapping");
        }
        return terms.isEmpty() ? Set.of(question.trim()) : terms;
    }

    private int scoreChunk(KnowledgeChunk chunk, String term, String question) {
        String haystack = (chunk.getTitle() + "\n" + chunk.getKeywords() + "\n" + chunk.getContent()).toLowerCase(Locale.ROOT);
        int score = 0;
        int index = haystack.indexOf(term.toLowerCase(Locale.ROOT));
        while (index >= 0) {
            score++;
            index = haystack.indexOf(term.toLowerCase(Locale.ROOT), index + term.length());
        }
        String sourcePath = Optional.ofNullable(chunk.getSourcePath()).orElse("").toLowerCase(Locale.ROOT);
        String title = Optional.ofNullable(chunk.getTitle()).orElse("").toLowerCase(Locale.ROOT);
        if (question.contains("接口") && (sourcePath.contains("/controller/") || sourcePath.contains("/src/api/"))) {
            score += 20;
        }
        if (question.contains("患者") && (title.contains("patient") || sourcePath.contains("patient"))) {
            score += 30;
        }
        return score;
    }

    private String buildRagPrompt(String question, List<KnowledgeChunk> chunks) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = chunks.get(i);
            context.append("【引用").append(i + 1).append("】")
                    .append(chunk.getTitle()).append(" / ").append(chunk.getSourcePath()).append("\n")
                    .append(chunk.getContent()).append("\n\n");
        }
        return """
                你是 MediCare 项目的文档知识库问答助手。
                只能根据下方检索到的文档片段回答问题；如果片段不足，请明确说明“当前知识库没有足够依据”。
                同时必须结合“实时业务数据快照”中的最新患者、挂号、病历、处方、库存等系统数据；当文档片段与实时数据冲突时，以实时业务数据为准。
                回答要简洁、可执行，并在关键结论后标注引用编号，例如 [引用1]。

                用户问题：
                %s

                实时业务数据快照：
                %s

                检索片段：
                %s
                """.formatted(question, liveBusinessContextService.buildSnapshot(), context);
    }

    private String callModel(String prompt) {
        validateProviderConfig();
        Map<String, Object> body = Map.of(
                "model", aiProperties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", "你是 MediCare 文档知识库 RAG 助手，必须基于引用回答。"),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2
        );

        JsonNode response = createAiClient().post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        JsonNode content = response == null ? null : response.at("/choices/0/message/content");
        if (content == null || content.isMissingNode() || content.asText().isBlank()) {
            throw new BusinessException(502, "百炼 AI 已响应，但没有返回有效内容");
        }
        return content.asText();
    }

    private String callModelAsync(String prompt) {
        try {
            return CompletableFuture.supplyAsync(
                    () -> callModel(prompt),
                    aiTaskExecutors.generationExecutor()
            ).join();
        } catch (CompletionException e) {
            throw unwrapCompletionException(e);
        }
    }

    private <T> T joinFuture(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            throw unwrapCompletionException(e);
        }
    }

    private RuntimeException unwrapCompletionException(CompletionException e) {
        Throwable cause = e.getCause() == null ? e : e.getCause();
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new BusinessException(500, "AI 异步任务异常：" + cause.getMessage());
    }

    private List<Double> createEmbedding(String input) {
        validateProviderConfig();
        Map<String, Object> body = Map.of(
                "model", aiProperties.getEmbeddingModel(),
                "input", input
        );

        JsonNode response = createAiClient().post()
                .uri("/embeddings")
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        JsonNode embeddingNode = response == null ? null : response.at("/data/0/embedding");
        if (embeddingNode == null || !embeddingNode.isArray() || embeddingNode.isEmpty()) {
            throw new BusinessException(502, "百炼 embedding 已响应，但没有返回有效向量");
        }
        List<Double> embedding = new ArrayList<>(embeddingNode.size());
        for (JsonNode value : embeddingNode) {
            embedding.add(value.asDouble());
        }
        return embedding;
    }

    private RestClient createAiClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(aiProperties.getTimeout());
        requestFactory.setReadTimeout(aiProperties.getTimeout());

        return restClientBuilder
                .baseUrl(trimTrailingSlash(aiProperties.getBaseUrl()))
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private void validateProviderConfig() {
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new BusinessException(500, "AI API Key 未配置，请检查 application-secret.yml");
        }
        if (aiProperties.getEmbeddingModel() == null || aiProperties.getEmbeddingModel().isBlank()) {
            throw new BusinessException(500, "AI embedding 模型未配置，请检查 ai.embedding-model");
        }
    }

    private String normalizeProvider() {
        return aiProperties.getProvider() == null ? "rag" : aiProperties.getProvider().trim().toLowerCase(Locale.ROOT);
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "https://api.openai.com/v1";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String resolveSourceType(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".html")) return "html";
        if (name.endsWith(".md")) return "markdown";
        return "text";
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^\\p{IsHan}a-zA-Z0-9._-]+", "_");
    }

    private String extractKeywords(String text) {
        return String.join(" ", tokenize(text).stream().limit(40).toList());
    }

    private String sha256(String content) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
