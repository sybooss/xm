package com.user.returnsassistant.pojo;

import lombok.Data;

import java.util.List;

@Data
public class ChatImageRisk {
    private String auditStatus;
    private String sufficiencyLevel;
    private String authenticityRisk;
    private String aiGeneratedRisk;
    private String tamperRisk;
    private String metadataSignal;
    private String visualSignal;
    private String watermarkSignal;
    private String c2paStatus;
    private String c2paProvider;
    private String c2paGenerator;
    private String c2paSignal;
    private String visionStatus;
    private String visionModel;
    private String visionSignal;
    private String requiredEvidence;
    private List<String> requiredEvidenceList;
    private String summary;
}
