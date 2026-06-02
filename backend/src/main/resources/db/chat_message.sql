CREATE TABLE IF NOT EXISTS `chat_message` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    BIGINT       NOT NULL                COMMENT '用户ID',
    `session_id` VARCHAR(64)  NOT NULL                COMMENT '会话ID(时间戳)',
    `role`       VARCHAR(16)  NOT NULL                COMMENT '角色: user/assistant',
    `content`    TEXT         DEFAULT NULL            COMMENT '消息正文',
    `image_url`  VARCHAR(512) DEFAULT NULL            COMMENT '图片存储路径',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_session` (`user_id`, `session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='辅导对话消息表';
