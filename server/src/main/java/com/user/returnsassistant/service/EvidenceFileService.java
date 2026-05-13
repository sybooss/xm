package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.UserAccount;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface EvidenceFileService {
    Map<String, Object> upload(MultipartFile file, UserAccount customer);
}
