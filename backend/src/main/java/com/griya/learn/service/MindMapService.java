package com.griya.learn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.griya.learn.entity.ResourceRecord;
import com.griya.learn.mapper.ResourceRecordMapper;
import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static guru.nidi.graphviz.attribute.Attributes.attr;
import static guru.nidi.graphviz.model.Factory.*;

@Service
public class MindMapService {

    private static final Logger log = LoggerFactory.getLogger(MindMapService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiService deepseekAiService;
    private final AiService sparkAiService;
    private final XfSearchService xfSearchService;
    private final ResourceRecordMapper resourceRecordMapper;

    private static final String STORAGE_DIR = "downloadData/xmind";

    private static final String MINDMAP_PROMPT =
        "你是一个思维导图生成专家。请根据用户输入的专业背景、课程内容、知识短板和学习需求，生成一个结构化的教学思维导图。" +
        "重点围绕知识短板展开，同时兼顾学习需求，确保内容有针对性和实用性。" +
        "严格按照以下JSON格式输出，不要输出任何其他内容：\n" +
        "{\"root\":\"核心主题\",\"children\":[" +
        "{\"name\":\"分支1\",\"children\":[" +
        "{\"name\":\"子节点1\"},{\"name\":\"子节点2\"}" +
        "]}," +
        "{\"name\":\"分支2\",\"children\":[{\"name\":\"子节点3\"}]}" +
        "]}\n" +
        "要求：1) 深度3-4层 2) 总节点15-25个 3) 每个节点名称简洁(不超过8字) 4) 只输出JSON";

    public MindMapService(@Qualifier("deepseekAiService") AiService deepseekAiService,
                           @Qualifier("sparkAiService") AiService sparkAiService,
                           XfSearchService xfSearchService,
                           ResourceRecordMapper resourceRecordMapper) {
        this.deepseekAiService = deepseekAiService;
        this.sparkAiService = sparkAiService;
        this.xfSearchService = xfSearchService;
        this.resourceRecordMapper = resourceRecordMapper;
    }

    public Map<String, Object> generateMindMap(Long userId, String major, String courseName,
                                                String knowledgeGaps, String learningNeeds,
                                                String model, String searchModel) {
        Map<String, Object> result = new HashMap<>();

        try {
            String prompt = "专业：" + (major != null ? major : "") +
                "，课程内容：" + (courseName != null ? courseName : "") +
                "，知识短板：" + (knowledgeGaps != null ? knowledgeGaps : "") +
                "，学习需求：" + (learningNeeds != null ? learningNeeds : "");

            String searchText = doSearch(courseName, searchModel);
            if (!searchText.isEmpty()) {
                prompt += searchText;
            }

            log.info("开始生成思维导图: model={}, searchModel={}, promptLen={}", model, searchModel, prompt.length());

            AiService aiService = "spark".equals(model) ? sparkAiService : deepseekAiService;
            String aiResponse = aiService.chat(MINDMAP_PROMPT, prompt, null);
            log.info("AI响应长度: {}", aiResponse != null ? aiResponse.length() : 0);

            String json = extractJson(aiResponse);
            Map<String, Object> tree = objectMapper.readValue(json, Map.class);

            String topic = (courseName != null && !courseName.isEmpty()) ? courseName : "思维导图";
            String pngUrl = renderTreeToPng(tree, topic);

            ResourceRecord record = new ResourceRecord();
            record.setUserId(userId);
            record.setCourseName(courseName);
            record.setKnowledgePoint(topic);
            record.setResourceType("mindmap");
            record.setImageUrl(pngUrl);
            record.setFileName(topic + "_思维导图.png");
            resourceRecordMapper.insert(record);

            result.put("code", 200);
            result.put("data", Map.of(
                "imageUrl", pngUrl,
                "tree", tree,
                "courseName", courseName,
                "knowledgePoint", topic
            ));
        } catch (Exception e) {
            log.error("思维导图生成失败", e);
            result.put("code", 500);
            result.put("message", "生成失败: " + e.getMessage());
        }

        return result;
    }

    private String doSearch(String query, String searchModel) {
        if (query == null || query.isEmpty()) return "";

        if ("xfsearch".equals(searchModel)) {
            String text = xfSearchService.searchToText(query, 6);
            if (!text.isEmpty()) {
                log.info("[MINDMAP] ONE SEARCH 返回结果, query={}", query);
                return text;
            }
            log.info("[MINDMAP] ONE SEARCH 无结果或未配置, query={}", query);
            return "";
        }

        // deepseek search: ask AI to generate knowledge summary
        try {
            String searchPrompt = "请就以下主题进行知识搜索，列出5-8条相关的核心概念、定义和要点。"
                + "每条用一句话概括，不要编造不存在的链接。\n\n"
                + "主题：" + query;
            String aiResult = deepseekAiService.chat(
                "你是一个专业的知识检索助手，请根据主题提供相关知识点摘要。",
                searchPrompt, null);
            if (aiResult != null && !aiResult.isEmpty()) {
                log.info("[MINDMAP] DeepSeek 搜索返回 {} 字, query={}", aiResult.length(), query);
                return "\n\n以下是通过AI检索到的相关知识点：\n" + aiResult;
            }
        } catch (Exception e) {
            log.error("[MINDMAP] DeepSeek 搜索异常: {}", e.getMessage());
        }
        return "";
    }

    private String extractJson(String text) {
        if (text == null) return "{\"root\":\"生成失败\",\"children\":[]}";
        text = text.trim();
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            String jsonStr = text.substring(start, end + 1);
            return sanitizeJson(jsonStr);
        }
        return text;
    }

    private String sanitizeJson(String json) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == '"' || next == '\\' || next == '/' || next == 'b'
                    || next == 'f' || next == 'n' || next == 'r' || next == 't' || next == 'u') {
                    sb.append(c);
                } else {
                    sb.append("\\\\");
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String renderTreeToPng(Map<String, Object> tree, String topic) {
        try {
            Path dir = Paths.get(STORAGE_DIR);
            Files.createDirectories(dir);

            String fileName = "xmind_" + topic.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_") + "_" + System.currentTimeMillis() + ".png";
            Path pngPath = dir.resolve(fileName);

            MutableGraph g = mutGraph(topic).setDirected(true);
            g.graphAttrs().add(attr("rankdir", "TB"));
            g.graphAttrs().add(attr("splines", "polyline"));
            g.graphAttrs().add(attr("fontname", "SimHei"));
            g.nodeAttrs().add(attr("fontname", "SimHei"));
            g.nodeAttrs().add(attr("shape", "box"));
            g.nodeAttrs().add(attr("style", "rounded,filled"));
            g.nodeAttrs().add(attr("fillcolor", "#E8F0FE"));
            g.nodeAttrs().add(attr("fontsize", "12"));

            String rootName = (String) tree.getOrDefault("root", topic);
            MutableNode root = mutNode(sanitize(rootName));
            root.add(attr("fillcolor", "#165DFF"));
            root.add(attr("fontcolor", "white"));
            root.add(attr("fontsize", "14"));

            List<Map<String, Object>> children = (List<Map<String, Object>>) tree.getOrDefault("children", Collections.emptyList());
            addChildren(root, children, 1);

            g.add(root);

            Graphviz.fromGraph(g).render(Format.PNG).toFile(pngPath.toFile());

            log.info("思维导图PNG已生成: {}", pngPath.toAbsolutePath());
            return "/api/resource/mindmap-image/" + fileName;
        } catch (Exception e) {
            log.error("渲染PNG失败", e);
            return null;
        }
    }

    private void addChildren(MutableNode parent, List<Map<String, Object>> children, int depth) {
        if (children == null || children.isEmpty()) return;

        String[] colors = {"#E8F5E9", "#FFF3E0", "#F3E5F5", "#E0F7FA", "#FFF9C4", "#FCE4EC"};

        for (Map<String, Object> child : children) {
            String name = (String) child.get("name");
            if (name == null) continue;

            MutableNode node = mutNode(sanitize(name));
            node.add(attr("fillcolor", colors[depth % colors.length]));

            List<Map<String, Object>> subChildren = (List<Map<String, Object>>) child.getOrDefault("children", Collections.emptyList());
            addChildren(node, subChildren, depth + 1);

            parent.addLink(node);
        }
    }

    private String sanitize(String name) {
        return name.replace("\"", "'").replace("\n", " ").trim();
    }
}
