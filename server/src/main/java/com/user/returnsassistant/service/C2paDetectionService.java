package com.user.returnsassistant.service;

import com.user.returnsassistant.pojo.C2paDetectionResult;

public interface C2paDetectionService {
    C2paDetectionResult detect(String fileUrl);
}
