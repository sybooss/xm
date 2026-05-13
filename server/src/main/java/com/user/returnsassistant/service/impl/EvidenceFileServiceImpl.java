package com.user.returnsassistant.service.impl;

import com.user.returnsassistant.exception.BusinessException;
import com.user.returnsassistant.pojo.UserAccount;
import com.user.returnsassistant.service.EvidenceFileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class EvidenceFileServiceImpl implements EvidenceFileService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    @Value("${app.upload.root:tmp/uploads}")
    private String uploadRoot;

    @Override
    public Map<String, Object> upload(MultipartFile file, UserAccount customer) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择需要上传的图片凭证");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("图片凭证不能超过 5MB");
        }
        String originalFilename = file.getOriginalFilename() == null ? "evidence" : file.getOriginalFilename().trim();
        String extension = extension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 jpg、jpeg、png、webp、gif 图片凭证");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException("上传文件必须是图片格式");
        }

        String day = LocalDate.now().format(DAY_FORMAT);
        String filename = "%s-%s.%s".formatted(customer.getId(), UUID.randomUUID(), extension);
        Path targetDir = Path.of(uploadRoot, "evidences", day).toAbsolutePath().normalize();
        Path targetFile = targetDir.resolve(filename).normalize();
        if (!targetFile.startsWith(targetDir)) {
            throw new BusinessException("图片保存路径不合法");
        }
        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile);
        } catch (IOException e) {
            throw new BusinessException("图片凭证上传失败");
        }
        String fileUrl = "/uploads/evidences/" + day + "/" + filename;
        return Map.of(
                "fileUrl", fileUrl,
                "originalFilename", originalFilename,
                "size", file.getSize(),
                "contentType", contentType
        );
    }

    private String extension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
