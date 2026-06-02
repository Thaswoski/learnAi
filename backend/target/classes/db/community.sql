CREATE TABLE IF NOT EXISTS `post` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`          VARCHAR(256) NOT NULL                COMMENT '标题',
    `content`        TEXT         NOT NULL                COMMENT '内容',
    `author_id`      BIGINT       NOT NULL                COMMENT '作者ID',
    `author_name`    VARCHAR(128) DEFAULT NULL            COMMENT '作者名',
    `view_count`     INT          NOT NULL DEFAULT 0      COMMENT '浏览数',
    `agree_count`    INT          NOT NULL DEFAULT 0      COMMENT '赞同数',
    `disagree_count` INT          NOT NULL DEFAULT 0      COMMENT '反对数',
    `status`         INT          NOT NULL DEFAULT 1      COMMENT '状态: 1正常 0删除',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_author_id` (`author_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区帖子表';

CREATE TABLE IF NOT EXISTS `post_attitude` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `post_id`    BIGINT   NOT NULL                COMMENT '帖子ID',
    `user_id`    BIGINT   NOT NULL                COMMENT '用户ID',
    `attitude`   INT      NOT NULL DEFAULT 0      COMMENT '态度: 1赞同 -1反对 0取消',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子态度表';

CREATE TABLE IF NOT EXISTS `comment` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `post_id`     BIGINT       DEFAULT NULL            COMMENT '帖子ID',
    `heritage_id` BIGINT       DEFAULT NULL            COMMENT '遗产ID(扩展)',
    `parent_id`   BIGINT       DEFAULT NULL            COMMENT '父评论ID(楼中楼)',
    `author_id`   BIGINT       NOT NULL                COMMENT '作者ID',
    `author_name` VARCHAR(128) DEFAULT NULL            COMMENT '作者名',
    `to_user_id`  BIGINT       DEFAULT NULL            COMMENT '回复目标用户ID',
    `to_username` VARCHAR(128) DEFAULT NULL            COMMENT '回复目标用户名',
    `content`     TEXT         NOT NULL                COMMENT '评论内容',
    `status`      INT          NOT NULL DEFAULT 1      COMMENT '状态: 1正常 0删除',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_post_id` (`post_id`),
    KEY `idx_author_id` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

CREATE TABLE IF NOT EXISTS `article` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`       VARCHAR(256) NOT NULL                COMMENT '标题',
    `summary`     VARCHAR(512) DEFAULT NULL            COMMENT '摘要',
    `content`     LONGTEXT     NOT NULL                COMMENT '内容',
    `cover_image` VARCHAR(512) DEFAULT NULL            COMMENT '封面图',
    `category`    VARCHAR(64)  DEFAULT NULL            COMMENT '分类',
    `author_id`   BIGINT       DEFAULT NULL            COMMENT '作者ID',
    `view_count`  INT          NOT NULL DEFAULT 0      COMMENT '浏览数',
    `like_count`  INT          NOT NULL DEFAULT 0      COMMENT '点赞数',
    `status`      INT          NOT NULL DEFAULT 1      COMMENT '状态: 1正常 0删除',
    `is_top`      INT          NOT NULL DEFAULT 0      COMMENT '是否置顶',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`),
    KEY `idx_author_id` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='百科文章表';
