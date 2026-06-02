CREATE TABLE IF NOT EXISTS `resource_record` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT       NOT NULL                COMMENT '用户ID',
    `course_name`     VARCHAR(128) NOT NULL                COMMENT '课程名称',
    `knowledge_point` VARCHAR(128) NOT NULL                COMMENT '知识点',
    `resource_type`   VARCHAR(32)  NOT NULL                COMMENT '资源类型: mindmap/document/exercise/code',
    `image_url`       VARCHAR(512) DEFAULT NULL            COMMENT '生成结果的图片URL',
    `file_name`       VARCHAR(256) DEFAULT NULL            COMMENT '下载文件名',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源生成记录表';
