-- ============================================================
-- WMS AI Platform - MySQL 初始化脚本
-- 数据库: wms_ai
-- 引擎: InnoDB / 字符集: utf8mb4
-- 对应 PRD V2.0 第 6.2 节数据库设计
-- ============================================================

CREATE DATABASE IF NOT EXISTS wms_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE wms_ai;

-- ------------------------------------------------------------
-- 1. 知识库文档表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kb_document (
    doc_id        VARCHAR(64)   NOT NULL COMMENT '文档ID（业务ID）',
    title         VARCHAR(500)  NOT NULL COMMENT '文档标题',
    category      VARCHAR(100)  NOT NULL DEFAULT 'general' COMMENT '分类',
    warehouse     VARCHAR(50)   DEFAULT NULL COMMENT '所属仓库',
    tags          JSON          DEFAULT NULL COMMENT '标签列表',
    source_file   VARCHAR(500)  DEFAULT NULL COMMENT '源文件名',
    file_type     VARCHAR(20)   DEFAULT NULL COMMENT '文件类型(pdf/docx/xlsx/md/img)',
    file_size     BIGINT        DEFAULT 0 COMMENT '文件大小(字节)',
    minio_object  VARCHAR(500)  DEFAULT NULL COMMENT 'MinIO对象路径',
    chunk_count   INT           DEFAULT 0 COMMENT '分块数量',
    status        VARCHAR(20)   NOT NULL DEFAULT 'pending' COMMENT '处理状态: pending/processing/completed/failed',
    error_msg     TEXT          DEFAULT NULL COMMENT '处理失败原因',
    created_by    VARCHAR(64)   DEFAULT NULL COMMENT '上传人',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (doc_id),
    KEY idx_kb_doc_category (category),
    KEY idx_kb_doc_status (status),
    KEY idx_kb_doc_warehouse (warehouse),
    KEY idx_kb_doc_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档表';

-- ------------------------------------------------------------
-- 2. 文档分块表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kb_chunk (
    chunk_id      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '分块ID',
    doc_id        VARCHAR(64)   NOT NULL COMMENT '所属文档ID',
    chunk_index   INT           NOT NULL COMMENT '分块序号(从0开始)',
    content       TEXT          NOT NULL COMMENT '分块文本内容',
    chunk_type    VARCHAR(30)   NOT NULL DEFAULT 'text' COMMENT '分块类型: text/heading/table',
    position      INT           DEFAULT 0 COMMENT '在文档中的位置序号',
    token_count   INT           DEFAULT 0 COMMENT 'Token数量(近似)',
    metadata      JSON          DEFAULT NULL COMMENT '元数据(页码/坐标等)',
    embedding_id  VARCHAR(128)  DEFAULT NULL COMMENT 'Milvus中的向量ID',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (chunk_id),
    KEY idx_kb_chunk_doc (doc_id),
    KEY idx_kb_chunk_type (chunk_type),
    FULLTEXT INDEX ft_kb_chunk_content (content) WITH PARSER ngram,  -- ngram全文索引供关键词召回
    CONSTRAINT fk_kb_chunk_doc FOREIGN KEY (doc_id) REFERENCES kb_document(doc_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档分块表';

-- ------------------------------------------------------------
-- 3. 对话会话表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_session (
    session_id    VARCHAR(64)   NOT NULL COMMENT '会话ID',
    user_id       VARCHAR(64)   DEFAULT NULL COMMENT '用户ID',
    module        VARCHAR(30)   NOT NULL DEFAULT 'rag' COMMENT '来源模块: rag/voice/ocr',
    title         VARCHAR(500)  DEFAULT NULL COMMENT '会话标题',
    warehouse     VARCHAR(50)   DEFAULT NULL COMMENT '关联仓库',
    message_count INT           DEFAULT 0 COMMENT '消息数量',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (session_id),
    KEY idx_chat_session_user (user_id),
    KEY idx_chat_session_module (module),
    KEY idx_chat_session_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话会话表';

-- ------------------------------------------------------------
-- 4. 对话消息表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_message (
    message_id    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    session_id    VARCHAR(64)   NOT NULL COMMENT '所属会话ID',
    role          VARCHAR(20)   NOT NULL COMMENT '角色: user/assistant',
    content       TEXT          NOT NULL COMMENT '消息内容',
    sources       JSON          DEFAULT NULL COMMENT '引用来源(文档片段列表)',
    confidence    DECIMAL(5,4)  DEFAULT NULL COMMENT '答案置信度',
    feedback      VARCHAR(10)   DEFAULT NULL COMMENT '用户反馈: like/dislike',
    feedback_note TEXT          DEFAULT NULL COMMENT '反馈备注',
    model         VARCHAR(100)  DEFAULT NULL COMMENT '使用的模型名',
    latency_ms    INT           DEFAULT NULL COMMENT '响应延迟(毫秒)',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (message_id),
    KEY idx_chat_msg_session (session_id),
    KEY idx_chat_msg_role (role),
    KEY idx_chat_msg_feedback (feedback),
    KEY idx_chat_msg_created (created_at),
    CONSTRAINT fk_chat_msg_session FOREIGN KEY (session_id) REFERENCES chat_session(session_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话消息表';

-- ------------------------------------------------------------
-- 5. OCR识别结果表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ocr_result (
    result_id     VARCHAR(64)   NOT NULL COMMENT '结果ID(业务ID)',
    doc_type      VARCHAR(30)   NOT NULL COMMENT '单据类型: delivery_note/po/receipt/return/quality',
    doc_no        VARCHAR(100)  DEFAULT NULL COMMENT '单据编号(识别提取)',
    warehouse     VARCHAR(50)   DEFAULT NULL COMMENT '关联仓库',
    image_path    VARCHAR(500)  DEFAULT NULL COMMENT 'MinIO图片路径',
    extracted     JSON          DEFAULT NULL COMMENT '结构化提取结果',
    raw_text      TEXT          DEFAULT NULL COMMENT '原始OCR文本',
    confidence    DECIMAL(5,4)  DEFAULT NULL COMMENT '识别置信度',
    diff_result   JSON          DEFAULT NULL COMMENT '差异比对结果',
    status        VARCHAR(20)   NOT NULL DEFAULT 'pending' COMMENT '状态: pending/confirmed/rejected',
    confirmed_by  VARCHAR(64)   DEFAULT NULL COMMENT '确认人',
    confirmed_at  DATETIME      DEFAULT NULL COMMENT '确认时间',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (result_id),
    KEY idx_ocr_result_type (doc_type),
    KEY idx_ocr_result_status (status),
    KEY idx_ocr_result_doc_no (doc_no),
    KEY idx_ocr_result_type_status (doc_type, status),
    KEY idx_ocr_result_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OCR识别结果表';

-- ------------------------------------------------------------
-- 6. 知识图谱三元组表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS kg_triple (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    subject       VARCHAR(200)  NOT NULL COMMENT '主体实体',
    subject_type  VARCHAR(50)   NOT NULL COMMENT '主体类型',
    predicate     VARCHAR(100)  NOT NULL COMMENT '关系',
    object        VARCHAR(200)  NOT NULL COMMENT '客体实体',
    object_type   VARCHAR(50)   NOT NULL COMMENT '客体类型',
    confidence    DECIMAL(5,4)  NOT NULL DEFAULT 1.0000 COMMENT '置信度',
    doc_id        VARCHAR(64)   NOT NULL COMMENT '来源文档',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_kg_triple_subject (subject),
    KEY idx_kg_triple_object (object),
    KEY idx_kg_triple_subject_type (subject, subject_type),
    KEY idx_kg_triple_doc (doc_id),
    CONSTRAINT fk_kg_triple_doc FOREIGN KEY (doc_id) REFERENCES kb_document(doc_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识图谱三元组表';
