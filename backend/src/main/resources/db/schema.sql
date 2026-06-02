CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `name`        VARCHAR(64)  NOT NULL                COMMENT '姓名',
    `email`       VARCHAR(128) NOT NULL                COMMENT '邮箱',
    `password`    VARCHAR(256) NOT NULL                COMMENT '密码(BCrypt加密)',
    `role`        VARCHAR(32)  NOT NULL DEFAULT 'student' COMMENT '角色: student/teacher/researcher',
    `avatar`      VARCHAR(512) DEFAULT NULL            COMMENT '头像地址',
    `token`       VARCHAR(256) DEFAULT NULL            COMMENT '登录令牌',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `c_questions` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL,
    `problem` TEXT NOT NULL,
    `difficulty` ENUM('easy', 'medium', 'hard') NOT NULL DEFAULT 'medium',
    `knowledge_point` VARCHAR(100) NOT NULL,
    `input_example` TEXT,
    `output_example` TEXT,
    `code_template` TEXT,
    `answer_hint` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_knowledge_point` (`knowledge_point`),
    INDEX `idx_difficulty` (`difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quiz_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `question_id` INT NOT NULL,
    `question_title` VARCHAR(200) NOT NULL,
    `user_code` TEXT NOT NULL,
    `result` VARCHAR(20) NOT NULL COMMENT 'correct / wrong / compile_error / runtime_error',
    `expected_output` TEXT,
    `actual_output` TEXT,
    `error_message` TEXT,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答题历史记录';

CREATE TABLE IF NOT EXISTS `evaluation_cache` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `quiz_total` INT NOT NULL DEFAULT 0 COMMENT '缓存时的答题总数',
    `data_json` MEDIUMTEXT NOT NULL COMMENT '评估结果JSON',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评估缓存';

CREATE TABLE IF NOT EXISTS `learning_path` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `quiz_total` INT NOT NULL DEFAULT 0 COMMENT '缓存时的答题总数',
    `data_json` MEDIUMTEXT NOT NULL COMMENT '学习路径JSON',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习路径';

CREATE TABLE IF NOT EXISTS `dashboard_cache` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `quiz_total` INT NOT NULL DEFAULT 0 COMMENT '缓存时的答题总数',
    `resource_total` INT NOT NULL DEFAULT 0 COMMENT '缓存时的资源总数',
    `data_json` MEDIUMTEXT NOT NULL COMMENT '仪表盘JSON',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='仪表盘缓存';

CREATE TABLE IF NOT EXISTS `resource_record` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`         BIGINT       NOT NULL                COMMENT '用户ID',
    `course_name`     VARCHAR(128) NOT NULL                COMMENT '课程名称',
    `knowledge_point` VARCHAR(128) NOT NULL                COMMENT '知识点',
    `resource_type`   VARCHAR(32)  NOT NULL                COMMENT '资源类型',
    `image_url`       VARCHAR(512) DEFAULT NULL            COMMENT '生成结果的图片URL',
    `file_name`       VARCHAR(256) DEFAULT NULL            COMMENT '下载文件名',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源生成记录表';
