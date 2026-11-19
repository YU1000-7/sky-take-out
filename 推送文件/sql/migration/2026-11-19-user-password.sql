-- 客户登录改为「手机号 + 密码」，历史库执行一次即可（新库由 init.sql 直接建列）。
ALTER TABLE `user`
    ADD COLUMN password VARCHAR(100) NOT NULL DEFAULT '' COMMENT 'BCrypt 密码' AFTER phone;

-- 历史账号统一补默认密码 123456（BCrypt 存储），登录后可自行修改。
UPDATE `user`
SET password = '$2a$10$Hem.JukUDAd5tFAkOFZQMeWtQGz99aueSz2bbzPxg7bLtmbqNTwwu'
WHERE password IS NULL OR password = '';
