package com.itmao.aispringboot.controller;

import com.itmao.aispringboot.common.Result;
import com.itmao.aispringboot.exception.BusinessException;
import com.itmao.aispringboot.service.FileService;
import com.itmao.aispringboot.util.JwtTokenUtil;
import com.itmao.aispringboot.util.RequestRateLimiter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.time.Duration;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileService fileService;
    private final RequestRateLimiter requestRateLimiter;

    public FileController(FileService fileService, RequestRateLimiter requestRateLimiter) {
        this.fileService = fileService;
        this.requestRateLimiter = requestRateLimiter;
    }

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "businessType", required = false) String businessType,
            @RequestParam(value = "businessId", required = false) String businessId,
            @RequestParam(value = "businessField", required = false) String businessField) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        requestRateLimiter.assertAllowed("upload-user:" + userId, 60, Duration.ofHours(1));
        if ("article".equalsIgnoreCase(businessType) && !JwtTokenUtil.isCurrentAdmin()) {
            throw new BusinessException("只有管理员可以上传文章图片");
        }
        return Result.success(fileService.upload(
                file,
                businessType,
                businessId,
                businessField,
                userId
        ));
    }
}
