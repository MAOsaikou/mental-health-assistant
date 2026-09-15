package com.itmao.aispringboot.service;

import com.itmao.aispringboot.entity.SysFileInfo;
import com.itmao.aispringboot.exception.BusinessException;
import com.itmao.aispringboot.mapper.SysFileInfoMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {
    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp"
    );
    private static final Set<String> ALLOWED_BUSINESS_TYPES = Set.of("user_avatar", "article", "common");

    @Resource
    private SysFileInfoMapper sysFileInfoMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public Map<String, Object> upload(MultipartFile file, String businessType, String businessId, String businessField, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择文件");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!IMAGE_EXT.contains(ext)) {
            throw new BusinessException("只支持上传图片文件（jpg/jpeg/png/gif/webp/bmp）");
        }
        String contentType = file.getContentType();
        if (contentType == null || !IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("文件类型不合法，只支持图片");
        }
        String safeType = sanitizeBusinessType(businessType);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = resolveUnderUpload("bussiness/" + safeType);
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) {
            throw new BusinessException("文件名不合法");
        }
        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), target);
        } catch (IOException e) {
            throw new BusinessException("文件保存失败");
        }
        String filePath = "/files/bussiness/" + safeType + "/" + filename;
        SysFileInfo info = SysFileInfo.builder()
                .originalName(original)
                .filePath(filePath)
                .fileSize(file.getSize())
                .fileType("IMG")
                .businessType(safeType)
                .businessId(businessId)
                .businessField(businessField)
                .uploadUserId(userId)
                .isTemp(0)
                .status(1)
                .createTime(LocalDateTime.now())
                .build();
        sysFileInfoMapper.insert(info);
        Map<String, Object> result = new HashMap<>();
        result.put("id", info.getId());
        result.put("filePath", filePath);
        result.put("originalName", original);
        result.put("fileSize", file.getSize());
        return result;
    }

    public Path resolveReadableFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new BusinessException("文件不存在");
        }
        String normalized = relativePath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.contains("..") || normalized.contains(":")) {
            throw new BusinessException("文件不存在");
        }
        Path file = resolveUnderUpload(normalized);
        if (!Files.isRegularFile(file)) {
            throw new BusinessException("文件不存在");
        }
        return file;
    }

    public String probeContentType(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".gif")) {
            return "image/gif";
        }
        if (name.endsWith(".webp")) {
            return "image/webp";
        }
        if (name.endsWith(".bmp")) {
            return "image/bmp";
        }
        return "image/jpeg";
    }

    private String sanitizeBusinessType(String businessType) {
        if (businessType == null || businessType.isBlank()) {
            return "common";
        }
        String normalized = businessType.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_BUSINESS_TYPES.contains(normalized)) {
            throw new BusinessException("不支持的上传类型");
        }
        return normalized;
    }

    private Path resolveUnderUpload(String relative) {
        Path root = Path.of(uploadDir).toAbsolutePath().normalize();
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            throw new BusinessException("文件路径不合法");
        }
        return resolved;
    }
}
