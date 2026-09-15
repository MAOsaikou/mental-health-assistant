ALTER TABLE `user`
    ADD COLUMN `api_key` varchar(255) NULL COMMENT '用户自带API Key' AFTER `status`,
    ADD COLUMN `api_base_url` varchar(255) NULL COMMENT 'API地址' AFTER `api_key`,
    ADD COLUMN `api_model` varchar(100) NULL COMMENT '模型名' AFTER `api_base_url`;
