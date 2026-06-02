package com.griya.learn.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.griya.learn.entity.StudentProfile;
import com.griya.learn.mapper.StudentProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class StudentProfileService {

    private final StudentProfileMapper mapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StudentProfile getByUserId(Long userId) {
        return mapper.selectByUserId(userId);
    }

    @Transactional
    public StudentProfile getOrCreate(Long userId) {
        StudentProfile profile = mapper.selectByUserId(userId);
        if (profile == null) {
            profile = new StudentProfile();
            profile.setUserId(userId);
            profile.setStatus("IN_PROGRESS");
            profile.setCompletedDimensions(0);
            mapper.insert(profile);
        }
        return profile;
    }

    @Transactional
    public StudentProfile saveProfile(Long userId, Map<String, Object> data) {
        StudentProfile profile = getOrCreate(userId);

        int completed = 0;
        try {
            if (data.containsKey("knowledgeMastery")) {
                profile.setKnowledgeMastery(objectMapper.writeValueAsString(data.get("knowledgeMastery")));
                completed++;
            }
            if (data.containsKey("overallLevel")) {
                profile.setOverallLevel((String) data.get("overallLevel"));
                completed++;
            }
            if (data.containsKey("diagnosisReport")) {
                profile.setDiagnosisReport((String) data.get("diagnosisReport"));
                completed++;
            }
            if (data.containsKey("studyRhythm")) {
                profile.setStudyRhythm(objectMapper.writeValueAsString(data.get("studyRhythm")));
                completed++;
            }
            if (data.containsKey("cognitiveStyle")) {
                profile.setCognitiveStyle(objectMapper.writeValueAsString(data.get("cognitiveStyle")));
                completed++;
            }
            if (data.containsKey("learningGoal")) {
                profile.setLearningGoal(objectMapper.writeValueAsString(data.get("learningGoal")));
                completed++;
            }
            if (data.containsKey("errorPattern")) {
                profile.setErrorPattern(objectMapper.writeValueAsString(data.get("errorPattern")));
                completed++;
            }
            if (data.containsKey("resourcePreference")) {
                profile.setResourcePreference(objectMapper.writeValueAsString(data.get("resourcePreference")));
                completed++;
            }
            if (data.containsKey("feedbackPreference")) {
                profile.setFeedbackPreference(objectMapper.writeValueAsString(data.get("feedbackPreference")));
                completed++;
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON序列化失败", e);
        }

        profile.setCompletedDimensions(Math.max(profile.getCompletedDimensions(), completed));
        profile.setStatus(completed >= 6 ? "COMPLETED" : "IN_PROGRESS");
        mapper.updateById(profile);

        return profile;
    }
}
