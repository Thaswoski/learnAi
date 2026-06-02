package com.griya.learn.agent;

import org.apache.poi.xslf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class PPTAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(PPTAgent.class);

    private static final String STORAGE_DIR = "downloadData/ppt";
    private static final Color PRIMARY = new Color(0x16, 0x5D, 0xFF);
    private static final Color BG_TITLE = new Color(0x16, 0x5D, 0xFF);
    private static final Color BG_SECTION = new Color(0xF2, 0xF3, 0xF5);
    private static final Color TEXT_DARK = new Color(0x1D, 0x21, 0x29);
    private static final Color TEXT_LIGHT = Color.WHITE;

    @Override
    public String getName() {
        return "PPTAgent";
    }

    @Override
    public String getRole() {
        return "PPT生成器";
    }

    @Override
    public AgentResult execute(AgentContext context, Consumer<String> onStep) {
        try {
            if (onStep != null) onStep.accept("正在生成PPT课件...");

            Map<String, Object> content = context.get("content");
            if (content == null) {
                return AgentResult.fail("没有内容可生成PPT");
            }

            String title = (String) content.getOrDefault("title", context.getCourseName());
            String summary = (String) content.getOrDefault("summary", "");
            List<Map<String, Object>> sections = context.get("sections");
            List<Map<String, Object>> exercises = context.get("exercises");

            XMLSlideShow ppt = new XMLSlideShow();
            ppt.setPageSize(new java.awt.Dimension(960, 540));

            // Slide 1: 封面
            addTitleSlide(ppt, title, summary);

            // Slide 2: 目录
            addTocSlide(ppt, sections);

            // Slides: 各章节内容
            if (sections != null) {
                for (Map<String, Object> section : sections) {
                    String heading = (String) section.getOrDefault("heading", "章节");
                    String sectionContent = (String) section.getOrDefault("content", "");
                    addContentSlide(ppt, heading, sectionContent);
                }
            }

            // Slides: 练习题
            if (exercises != null && !exercises.isEmpty()) {
                addExerciseSlides(ppt, exercises);
            }

            // Slide: 总结
            addEndSlide(ppt, title);

            // 保存到文件
            Path dir = Paths.get(STORAGE_DIR);
            Files.createDirectories(dir);

            String safeTitle = title.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_");
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String fileName = safeTitle + "_" + ts + ".pptx";
            Path filePath = dir.resolve(fileName);

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                ppt.write(bos);
                Files.write(filePath, bos.toByteArray());

                byte[] pptBytes = bos.toByteArray();
                String base64 = Base64.getEncoder().encodeToString(pptBytes);

                log.info("[PPTAgent] PPT已生成: {} ({} bytes)", filePath, pptBytes.length);

                Map<String, Object> pptData = Map.of(
                    "fileName", fileName,
                    "base64", base64,
                    "mimeType", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "downloadUrl", "/api/resource/download/ppt/" + fileName
                );
                context.put("ppt", pptData);

                return AgentResult.ok("PPT课件已生成: " + fileName, pptData);
            }

        } catch (Exception e) {
            log.error("[PPTAgent] PPT生成失败", e);
            return AgentResult.fail("PPT生成失败: " + e.getMessage());
        }
    }

    private void addTitleSlide(XMLSlideShow ppt, String title, String summary) {
        XSLFSlide slide = ppt.createSlide();

        // 蓝色背景
        XSLFBackground bg = slide.getBackground();
        bg.setFillColor(BG_TITLE);

        // 标题
        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new java.awt.Rectangle(60, 120, 840, 120));
        XSLFTextRun titleRun = titleBox.addNewTextParagraph().addNewTextRun();
        titleRun.setText(title);
        titleRun.setFontSize(40.0);
        titleRun.setFontColor(TEXT_LIGHT);
        titleRun.setBold(true);
        titleRun.setFontFamily("Microsoft YaHei");

        // 摘要
        if (summary != null && !summary.isEmpty()) {
            XSLFTextBox summaryBox = slide.createTextBox();
            summaryBox.setAnchor(new java.awt.Rectangle(60, 280, 840, 100));
            XSLFTextRun summaryRun = summaryBox.addNewTextParagraph().addNewTextRun();
            summaryRun.setText(summary);
            summaryRun.setFontSize(18.0);
            summaryRun.setFontColor(new Color(0xE8, 0xE8, 0xFF));
            summaryRun.setFontFamily("Microsoft YaHei");
        }

        // 底部标注
        XSLFTextBox footer = slide.createTextBox();
        footer.setAnchor(new java.awt.Rectangle(60, 450, 840, 40));
        XSLFTextRun footerRun = footer.addNewTextParagraph().addNewTextRun();
        footerRun.setText("智多星 · 多智能体协同生成");
        footerRun.setFontSize(12.0);
        footerRun.setFontColor(new Color(0xAA, 0xBB, 0xEE));
        footerRun.setFontFamily("Microsoft YaHei");
    }

    private void addTocSlide(XMLSlideShow ppt, List<Map<String, Object>> sections) {
        XSLFSlide slide = ppt.createSlide();

        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new java.awt.Rectangle(60, 40, 840, 60));
        XSLFTextRun titleRun = titleBox.addNewTextParagraph().addNewTextRun();
        titleRun.setText("课程目录");
        titleRun.setFontSize(32.0);
        titleRun.setBold(true);
        titleRun.setFontColor(PRIMARY);
        titleRun.setFontFamily("Microsoft YaHei");

        // 分隔线
        XSLFAutoShape line = slide.createAutoShape();
        line.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        line.setAnchor(new java.awt.Rectangle(60, 95, 200, 3));
        line.setFillColor(PRIMARY);

        if (sections != null) {
            int y = 130;
            for (int i = 0; i < sections.size(); i++) {
                String heading = (String) sections.get(i).getOrDefault("heading", "");

                XSLFTextBox itemBox = slide.createTextBox();
                itemBox.setAnchor(new java.awt.Rectangle(80, y, 800, 40));
                XSLFTextParagraph p = itemBox.addNewTextParagraph();
                XSLFTextRun numRun = p.addNewTextRun();
                numRun.setText(String.format("%02d  ", i + 1));
                numRun.setFontSize(20.0);
                numRun.setBold(true);
                numRun.setFontColor(PRIMARY);
                XSLFTextRun textRun = p.addNewTextRun();
                textRun.setText(heading);
                textRun.setFontSize(20.0);
                textRun.setFontColor(TEXT_DARK);
                numRun.setFontFamily("Microsoft YaHei");
                textRun.setFontFamily("Microsoft YaHei");

                y += 55;
            }
        }
    }

    private void addContentSlide(XMLSlideShow ppt, String heading, String content) {
        XSLFSlide slide = ppt.createSlide();

        // 顶部色条
        XSLFAutoShape topBar = slide.createAutoShape();
        topBar.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        topBar.setAnchor(new java.awt.Rectangle(0, 0, 960, 6));
        topBar.setFillColor(PRIMARY);

        // 标题
        XSLFTextBox titleBox = slide.createTextBox();
        titleBox.setAnchor(new java.awt.Rectangle(50, 30, 860, 50));
        XSLFTextRun titleRun = titleBox.addNewTextParagraph().addNewTextRun();
        titleRun.setText(heading);
        titleRun.setFontSize(28.0);
        titleRun.setBold(true);
        titleRun.setFontColor(TEXT_DARK);
        titleRun.setFontFamily("Microsoft YaHei");

        // 内容
        if (content != null && !content.isEmpty()) {
            XSLFTextBox contentBox = slide.createTextBox();
            contentBox.setAnchor(new java.awt.Rectangle(50, 100, 860, 410));
            contentBox.setWordWrap(true);

            String[] paragraphs = content.split("\n");
            for (String paraText : paragraphs) {
                if (paraText.trim().isEmpty()) continue;

                XSLFTextParagraph p = contentBox.addNewTextParagraph();
                p.setLineSpacing(150.0);
                XSLFTextRun run = p.addNewTextRun();

                if (paraText.startsWith("```")) {
                    run.setText(paraText.replaceAll("```", "").trim());
                    run.setFontSize(11.0);
                    run.setFontFamily("Consolas");
                    run.setFontColor(new Color(0x33, 0x33, 0x33));
                } else if (paraText.startsWith("#")) {
                    run.setText(paraText.replaceAll("^#+\\s*", ""));
                    run.setFontSize(20.0);
                    run.setBold(true);
                    run.setFontColor(PRIMARY);
                    run.setFontFamily("Microsoft YaHei");
                } else {
                    run.setText(paraText);
                    run.setFontSize(16.0);
                    run.setFontColor(TEXT_DARK);
                    run.setFontFamily("Microsoft YaHei");
                }
            }
        }

        // 页码
        XSLFTextBox pageNum = slide.createTextBox();
        pageNum.setAnchor(new java.awt.Rectangle(880, 500, 60, 30));
        XSLFTextRun pageRun = pageNum.addNewTextParagraph().addNewTextRun();
        pageRun.setText(String.valueOf(ppt.getSlides().size()));
        pageRun.setFontSize(10.0);
        pageRun.setFontColor(new Color(0xAA, 0xAA, 0xAA));
    }

    private void addExerciseSlides(XMLSlideShow ppt, List<Map<String, Object>> exercises) {
        // 练习题标题页
        XSLFSlide titleSlide = ppt.createSlide();

        XSLFAutoShape topBar = titleSlide.createAutoShape();
        topBar.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        topBar.setAnchor(new java.awt.Rectangle(0, 0, 960, 6));
        topBar.setFillColor(PRIMARY);

        XSLFTextBox titleBox = titleSlide.createTextBox();
        titleBox.setAnchor(new java.awt.Rectangle(50, 200, 860, 100));
        XSLFTextParagraph tp = titleBox.addNewTextParagraph();
        tp.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER);
        XSLFTextRun titleRun = tp.addNewTextRun();
        titleRun.setText("课后练习");
        titleRun.setFontSize(40.0);
        titleRun.setBold(true);
        titleRun.setFontColor(PRIMARY);
        titleRun.setFontFamily("Microsoft YaHei");

        XSLFTextParagraph cp = titleBox.addNewTextParagraph();
        cp.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER);
        XSLFTextRun countRun = cp.addNewTextRun();
        countRun.setText("共 " + exercises.size() + " 道题");
        countRun.setFontSize(18.0);
        countRun.setFontColor(new Color(0x86, 0x9A, 0xAA));
        countRun.setFontFamily("Microsoft YaHei");

        // 每题一页
        int qNum = 0;
        for (Map<String, Object> ex : exercises) {
            qNum++;
            XSLFSlide qSlide = ppt.createSlide();

            XSLFAutoShape bar = qSlide.createAutoShape();
            bar.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
            bar.setAnchor(new java.awt.Rectangle(0, 0, 960, 6));
            bar.setFillColor(PRIMARY);

            // 题号
            XSLFTextBox numBox = qSlide.createTextBox();
            numBox.setAnchor(new java.awt.Rectangle(50, 30, 860, 40));
            XSLFTextRun numRun = numBox.addNewTextParagraph().addNewTextRun();
            numRun.setText("第 " + qNum + " 题");
            numRun.setFontSize(14.0);
            numRun.setFontColor(new Color(0x86, 0x9A, 0xAA));
            numRun.setFontFamily("Microsoft YaHei");

            // 题目
            String question = (String) ex.getOrDefault("question", "");
            XSLFTextBox qBox = qSlide.createTextBox();
            qBox.setAnchor(new java.awt.Rectangle(50, 80, 860, 200));
            XSLFTextRun qRun = qBox.addNewTextParagraph().addNewTextRun();
            qRun.setText(question);
            qRun.setFontSize(22.0);
            qRun.setFontColor(TEXT_DARK);
            qRun.setFontFamily("Microsoft YaHei");

            // 难度
            String difficulty = (String) ex.getOrDefault("difficulty", "medium");
            XSLFTextBox diffBox = qSlide.createTextBox();
            diffBox.setAnchor(new java.awt.Rectangle(50, 250, 200, 30));
            XSLFTextRun diffRun = diffBox.addNewTextParagraph().addNewTextRun();
            String diffLabel = "medium".equals(difficulty) ? "⭐⭐ 中等" : "hard".equals(difficulty) ? "⭐⭐⭐ 困难" : "⭐ 简单";
            diffRun.setText(diffLabel);
            diffRun.setFontSize(12.0);
            diffRun.setFontColor(new Color(0xFF, 0x7D, 0x00));
            diffRun.setFontFamily("Microsoft YaHei");

            // 答案页
            XSLFSlide aSlide = ppt.createSlide();

            XSLFAutoShape abar = aSlide.createAutoShape();
            abar.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
            abar.setAnchor(new java.awt.Rectangle(0, 0, 960, 6));
            abar.setFillColor(new Color(0x00, 0xB4, 0x2A));

            XSLFTextBox aLabel = aSlide.createTextBox();
            aLabel.setAnchor(new java.awt.Rectangle(50, 30, 860, 40));
            XSLFTextRun aLabelRun = aLabel.addNewTextParagraph().addNewTextRun();
            aLabelRun.setText("第 " + qNum + " 题 · 参考答案");
            aLabelRun.setFontSize(14.0);
            aLabelRun.setFontColor(new Color(0x00, 0xB4, 0x2A));
            aLabelRun.setFontFamily("Microsoft YaHei");

            XSLFTextBox qBox2 = aSlide.createTextBox();
            qBox2.setAnchor(new java.awt.Rectangle(50, 80, 860, 140));
            XSLFTextRun qRun2 = qBox2.addNewTextParagraph().addNewTextRun();
            qRun2.setText(question);
            qRun2.setFontSize(18.0);
            qRun2.setFontColor(TEXT_DARK);
            qRun2.setFontFamily("Microsoft YaHei");

            String answer = (String) ex.getOrDefault("answer", "");
            XSLFTextBox aBox = aSlide.createTextBox();
            aBox.setAnchor(new java.awt.Rectangle(50, 240, 860, 240));
            XSLFTextRun aRun = aBox.addNewTextParagraph().addNewTextRun();
            aRun.setText("参考答案：\n" + answer);
            aRun.setFontSize(18.0);
            aRun.setFontColor(new Color(0x00, 0xB4, 0x2A));
            aRun.setFontFamily("Microsoft YaHei");
        }
    }

    private void addEndSlide(XMLSlideShow ppt, String title) {
        XSLFSlide slide = ppt.createSlide();

        XSLFBackground bg = slide.getBackground();
        bg.setFillColor(BG_TITLE);

        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new java.awt.Rectangle(60, 200, 840, 200));
        XSLFTextParagraph tp = box.addNewTextParagraph();
        tp.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER);
        XSLFTextRun endRun = tp.addNewTextRun();
        endRun.setText("感谢学习");
        endRun.setFontSize(36.0);
        endRun.setFontColor(TEXT_LIGHT);
        endRun.setBold(true);
        endRun.setFontFamily("Microsoft YaHei");

        XSLFTextParagraph cp = box.addNewTextParagraph();
        cp.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER);
        XSLFTextRun courseRun = cp.addNewTextRun();
        courseRun.setText(title);
        courseRun.setFontSize(18.0);
        courseRun.setFontColor(new Color(0xAA, 0xBB, 0xEE));
        courseRun.setFontFamily("Microsoft YaHei");

        XSLFTextParagraph bp = box.addNewTextParagraph();
        bp.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER);
        XSLFTextRun bottomRun = bp.addNewTextRun();
        bottomRun.setText("智多星 · 多智能体协同生成");
        bottomRun.setFontSize(14.0);
        bottomRun.setFontColor(new Color(0x88, 0x99, 0xCC));
        bottomRun.setFontFamily("Microsoft YaHei");
    }
}
