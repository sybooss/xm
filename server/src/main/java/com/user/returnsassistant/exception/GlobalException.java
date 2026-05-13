package com.user.returnsassistant.exception;

import com.user.returnsassistant.pojo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalException {
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e) {
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public Result handleDuplicateKeyException(DuplicateKeyException e) {
        return Result.error("数据已经存在");
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public Result handleMultipartException(Exception e) {
        log.warn("文件上传失败：{}", e.getMessage());
        return Result.error("图片上传失败，请上传 5MB 以内的 jpg、png、webp 或 gif 图片");
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统异常");
    }
}
