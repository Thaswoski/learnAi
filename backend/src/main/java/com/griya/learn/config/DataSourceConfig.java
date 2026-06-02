package com.griya.learn.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource() {
        String dbName = extractDbName(url);
        String baseUrl = url.substring(0, url.indexOf(dbName))
                + "?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";

        try (Connection conn = DriverManager.getConnection(baseUrl, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + dbName
                    + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
            log.info("数据库 {} 已就绪", dbName);
        } catch (Exception e) {
            log.warn("自动建库失败，若库已存在可忽略: {}", e.getMessage());
        }

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");

        ensureAvatarColumn(ds);
        ensureStudentProfileTable(ds);
        ensureChatMessageTable(ds);
        ensureResourceRecordTable(ds);

        return ds;
    }

    private void ensureAvatarColumn(HikariDataSource ds) {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("ALTER TABLE `user` ADD COLUMN `avatar` VARCHAR(512) DEFAULT NULL AFTER `role`");
            log.info("avatar 列已添加");
        } catch (Exception e) {
            log.info("avatar 列可能已存在: {}", e.getMessage());
        }
    }

    private void ensureStudentProfileTable(HikariDataSource ds) {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `student_profile` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`user_id` BIGINT NOT NULL, " +
                "`knowledge_mastery` JSON DEFAULT NULL, " +
                "`overall_level` VARCHAR(64) DEFAULT NULL, " +
                "`diagnosis_report` TEXT DEFAULT NULL, " +
                "`study_rhythm` JSON DEFAULT NULL, " +
                "`cognitive_style` JSON DEFAULT NULL, " +
                "`learning_goal` JSON DEFAULT NULL, " +
                "`error_pattern` JSON DEFAULT NULL, " +
                "`resource_preference` JSON DEFAULT NULL, " +
                "`feedback_preference` JSON DEFAULT NULL, " +
                "`completed_dimensions` TINYINT NOT NULL DEFAULT 0, " +
                "`status` VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS', " +
                "`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "`updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), KEY `idx_user_id` (`user_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生画像表'");
            log.info("student_profile 表已就绪");
        } catch (Exception e) {
            log.info("student_profile 表可能已存在: {}", e.getMessage());
        }
    }

    private void ensureChatMessageTable(HikariDataSource ds) {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `chat_message` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`user_id` BIGINT NOT NULL, " +
                "`session_id` VARCHAR(64) NOT NULL, " +
                "`role` VARCHAR(16) NOT NULL, " +
                "`content` TEXT DEFAULT NULL, " +
                "`image_url` VARCHAR(512) DEFAULT NULL, " +
                "`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), KEY `idx_user_session` (`user_id`, `session_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='辅导对话消息表'");
            stmt.executeUpdate("ALTER TABLE `chat_message` MODIFY COLUMN `image_url` VARCHAR(512) DEFAULT NULL");
            log.info("chat_message 表已就绪");
        } catch (Exception e) {
            log.info("chat_message 表可能已存在: {}", e.getMessage());
        }
    }

    private void ensureResourceRecordTable(HikariDataSource ds) {
        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS `resource_record` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`user_id` BIGINT NOT NULL, " +
                "`course_name` VARCHAR(128) NOT NULL, " +
                "`knowledge_point` VARCHAR(128) NOT NULL, " +
                "`resource_type` VARCHAR(32) NOT NULL, " +
                "`image_url` VARCHAR(512) DEFAULT NULL, " +
                "`file_name` VARCHAR(256) DEFAULT NULL, " +
                "`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), KEY `idx_user_id` (`user_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源生成记录表'");
            log.info("resource_record 表已就绪");
        } catch (Exception e) {
            log.info("resource_record 表可能已存在: {}", e.getMessage());
        }
    }

    private String extractDbName(String jdbcUrl) {
        int start = jdbcUrl.indexOf("3306/") + 5;
        int end = jdbcUrl.indexOf("?", start);
        if (end == -1) end = jdbcUrl.length();
        return jdbcUrl.substring(start, end);
    }
}
