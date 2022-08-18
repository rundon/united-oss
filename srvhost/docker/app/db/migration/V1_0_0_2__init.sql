-- 文件上传
CREATE TABLE sys_oss
(
    id          bigint NOT NULL COMMENT 'id',
    file_name   varchar(255) COMMENT 'URL地址',
    url         varchar(200) COMMENT 'URL地址',
    size        bigint COMMENT '文件大小（k）',
    creator     bigint COMMENT '创建者',
    create_date datetime COMMENT '创建时间',
    PRIMARY KEY (id),
    key         idx_create_date (create_date)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COMMENT='文件上传';