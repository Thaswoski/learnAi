CREATE TABLE IF NOT EXISTS `student_profile` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`              BIGINT       NOT NULL                COMMENT '用户ID',
    `knowledge_mastery`    JSON         DEFAULT NULL            COMMENT '知识点掌握 [{"name":"Java","status":"良好","score":85}]',
    `overall_level`        VARCHAR(64)  DEFAULT NULL            COMMENT '整体掌握度',
    `diagnosis_report`     TEXT         DEFAULT NULL            COMMENT '学习诊断报告',
    `study_rhythm`         JSON         DEFAULT NULL            COMMENT '学习节奏 {"studySlot":"晚间","focusDuration":"45分钟","habit":"碎片化"}',
    `cognitive_style`      JSON         DEFAULT NULL            COMMENT '认知偏好 {"mediaPreference":["视频"],"understanding":"先实例后理论"}',
    `learning_goal`        JSON         DEFAULT NULL            COMMENT '学习目标 {"purpose":"就业","weeklyHours":"10小时"}',
    `error_pattern`        JSON         DEFAULT NULL            COMMENT '易错类型 [{"type":"概念混淆","frequency":"高","cause":"基础不扎实"}]',
    `resource_preference`  JSON         DEFAULT NULL            COMMENT '资源偏好 {"difficulty":"基础","contentLength":"中等","acceptExtension":true}',
    `feedback_preference`  JSON         DEFAULT NULL            COMMENT '反馈偏好 {"answerStyle":"引导思考","feedbackFrequency":"即时"}',
    `completed_dimensions` TINYINT      NOT NULL DEFAULT 0      COMMENT '已完成维度数',
    `status`               VARCHAR(32)  NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '状态: IN_PROGRESS/COMPLETED',
    `created_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生画像表';
