CREATE DATABASE IF NOT EXISTS test3
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE test3;

CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  display_name VARCHAR(80) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
  phone VARCHAR(20) NULL,
  password_hash VARCHAR(128) NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_user_username UNIQUE (username),
  CONSTRAINT ck_user_role CHECK (role IN ('CUSTOMER', 'ADMIN')),
  CONSTRAINT ck_user_status CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS demo_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(40) NOT NULL,
  user_id BIGINT NULL,
  product_name VARCHAR(120) NOT NULL,
  sku_name VARCHAR(120) NULL,
  order_amount DECIMAL(10,2) NOT NULL,
  pay_status VARCHAR(20) NOT NULL,
  order_status VARCHAR(30) NOT NULL,
  logistics_status VARCHAR(30) NOT NULL,
  after_sale_status VARCHAR(30) NOT NULL DEFAULT 'NONE',
  paid_at DATETIME NULL,
  shipped_at DATETIME NULL,
  signed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_order_no UNIQUE (order_no),
  CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT ck_order_pay_status CHECK (pay_status IN ('UNPAID', 'PAID', 'REFUNDING', 'REFUNDED')),
  CONSTRAINT ck_order_status CHECK (order_status IN ('PENDING_PAY', 'PAID', 'SHIPPED', 'SIGNED', 'COMPLETED', 'CLOSED')),
  CONSTRAINT ck_order_logistics_status CHECK (logistics_status IN ('NOT_SHIPPED', 'IN_TRANSIT', 'DELIVERED', 'ABNORMAL')),
  CONSTRAINT ck_order_after_sale_status CHECK (after_sale_status IN ('NONE', 'RETURN_APPLYING', 'RETURNING', 'EXCHANGE_APPLYING', 'REFUNDING', 'FINISHED', 'REJECTED')),
  INDEX idx_order_user (user_id),
  INDEX idx_order_status (order_status, after_sale_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS after_sale_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  after_sale_no VARCHAR(40) NOT NULL,
  order_id BIGINT NOT NULL,
  service_type VARCHAR(30) NOT NULL,
  reason VARCHAR(200) NULL,
  status VARCHAR(30) NOT NULL,
  refund_amount DECIMAL(10,2) NULL,
  apply_at DATETIME NOT NULL,
  handle_at DATETIME NULL,
  remark VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_after_sale_no UNIQUE (after_sale_no),
  CONSTRAINT fk_after_sale_order FOREIGN KEY (order_id) REFERENCES demo_order(id) ON DELETE CASCADE,
  CONSTRAINT ck_after_sale_service_type CHECK (service_type IN ('RETURN', 'EXCHANGE', 'REFUND', 'COMPLAINT')),
  CONSTRAINT ck_after_sale_status CHECK (status IN ('APPLIED', 'APPROVED', 'REJECTED', 'WAIT_BUYER_SEND', 'WAIT_SELLER_CONFIRM', 'REFUNDING', 'FINISHED')),
  INDEX idx_after_sale_order (order_id),
  INDEX idx_after_sale_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS after_sale_application (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  application_no VARCHAR(40) NOT NULL,
  order_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  service_type VARCHAR(30) NOT NULL,
  reason_code VARCHAR(50) NOT NULL DEFAULT 'OTHER',
  reason_text VARCHAR(500) NOT NULL,
  status VARCHAR(30) NOT NULL,
  refund_amount DECIMAL(10,2) NULL,
  approved_amount DECIMAL(10,2) NULL,
  priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  sla_deadline DATETIME NULL,
  assigned_to BIGINT NULL,
  ticket_id BIGINT NULL,
  ai_summary TEXT NULL,
  risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  closed_at DATETIME NULL,
  CONSTRAINT uk_after_sale_application_no UNIQUE (application_no),
  CONSTRAINT fk_after_sale_application_order FOREIGN KEY (order_id) REFERENCES demo_order(id) ON DELETE CASCADE,
  CONSTRAINT fk_after_sale_application_user FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT fk_after_sale_application_assignee FOREIGN KEY (assigned_to) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT ck_after_sale_application_type CHECK (service_type IN ('RETURN', 'EXCHANGE', 'REFUND', 'COMPLAINT')),
  CONSTRAINT ck_after_sale_application_status CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'NEED_MORE_EVIDENCE', 'APPROVED', 'WAIT_BUYER_SEND', 'WAIT_SELLER_RECEIVE', 'REFUNDING', 'EXCHANGING', 'REJECTED', 'COMPLETED', 'CANCELLED')),
  CONSTRAINT ck_after_sale_application_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
  CONSTRAINT ck_after_sale_application_risk CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
  INDEX idx_after_sale_application_user (user_id, status),
  INDEX idx_after_sale_application_order (order_id),
  INDEX idx_after_sale_application_ticket (ticket_id),
  INDEX idx_after_sale_application_status (status, priority, updated_at),
  INDEX idx_after_sale_application_sla (sla_deadline, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @after_sale_application_ticket_column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'after_sale_application'
    AND column_name = 'ticket_id'
);
SET @after_sale_application_ticket_column_sql = IF(
  @after_sale_application_ticket_column_exists = 0,
  'ALTER TABLE after_sale_application ADD COLUMN ticket_id BIGINT NULL AFTER assigned_to',
  'SELECT 1'
);
PREPARE after_sale_application_ticket_column_stmt FROM @after_sale_application_ticket_column_sql;
EXECUTE after_sale_application_ticket_column_stmt;
DEALLOCATE PREPARE after_sale_application_ticket_column_stmt;

SET @after_sale_application_ticket_index_exists = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'after_sale_application'
    AND index_name = 'idx_after_sale_application_ticket'
);
SET @after_sale_application_ticket_index_sql = IF(
  @after_sale_application_ticket_index_exists = 0,
  'ALTER TABLE after_sale_application ADD INDEX idx_after_sale_application_ticket (ticket_id)',
  'SELECT 1'
);
PREPARE after_sale_application_ticket_index_stmt FROM @after_sale_application_ticket_index_sql;
EXECUTE after_sale_application_ticket_index_stmt;
DEALLOCATE PREPARE after_sale_application_ticket_index_stmt;

CREATE TABLE IF NOT EXISTS after_sale_process_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  application_id BIGINT NOT NULL,
  operator_id BIGINT NULL,
  operator_name VARCHAR(80) NULL,
  operator_role VARCHAR(20) NOT NULL,
  action VARCHAR(40) NOT NULL,
  from_status VARCHAR(30) NULL,
  to_status VARCHAR(30) NULL,
  remark VARCHAR(500) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_after_sale_process_log_application FOREIGN KEY (application_id) REFERENCES after_sale_application(id) ON DELETE CASCADE,
  CONSTRAINT fk_after_sale_process_log_operator FOREIGN KEY (operator_id) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT ck_after_sale_process_log_role CHECK (operator_role IN ('CUSTOMER', 'ADMIN', 'SYSTEM', 'AI')),
  CONSTRAINT ck_after_sale_process_log_action CHECK (action IN ('SUBMIT', 'APPROVE', 'REJECT', 'REQUEST_MORE_EVIDENCE', 'SUPPLEMENT_EVIDENCE', 'CREATE_TICKET', 'UPDATE_TICKET', 'GENERATE_REPLY_DRAFT', 'USE_REPLY_DRAFT', 'DISCARD_REPLY_DRAFT', 'SUBMIT_REVIEW', 'CANCEL', 'CONFIRM', 'SYSTEM_MARK')),
  INDEX idx_after_sale_process_log_application (application_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE after_sale_process_log DROP CHECK ck_after_sale_process_log_action;
ALTER TABLE after_sale_process_log
  ADD CONSTRAINT ck_after_sale_process_log_action CHECK (action IN ('SUBMIT', 'APPROVE', 'REJECT', 'REQUEST_MORE_EVIDENCE', 'SUPPLEMENT_EVIDENCE', 'CREATE_TICKET', 'UPDATE_TICKET', 'GENERATE_REPLY_DRAFT', 'USE_REPLY_DRAFT', 'DISCARD_REPLY_DRAFT', 'SUBMIT_REVIEW', 'CANCEL', 'CONFIRM', 'SYSTEM_MARK'));

CREATE TABLE IF NOT EXISTS after_sale_evidence (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  application_id BIGINT NOT NULL,
  evidence_type VARCHAR(30) NOT NULL,
  file_url VARCHAR(500) NULL,
  content VARCHAR(1000) NOT NULL,
  uploaded_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_after_sale_evidence_application FOREIGN KEY (application_id) REFERENCES after_sale_application(id) ON DELETE CASCADE,
  CONSTRAINT fk_after_sale_evidence_user FOREIGN KEY (uploaded_by) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT ck_after_sale_evidence_type CHECK (evidence_type IN ('IMAGE', 'VIDEO', 'TEXT', 'LOGISTICS_NO')),
  INDEX idx_after_sale_evidence_application (application_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS knowledge_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT NOT NULL DEFAULT 0,
  category_code VARCHAR(50) NOT NULL,
  category_name VARCHAR(80) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_knowledge_category_code UNIQUE (category_code),
  CONSTRAINT ck_knowledge_category_enabled CHECK (enabled IN (0, 1)),
  INDEX idx_knowledge_category_parent (parent_id),
  INDEX idx_knowledge_category_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS knowledge_doc (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT NOT NULL,
  title VARCHAR(150) NOT NULL,
  doc_type VARCHAR(30) NOT NULL,
  intent_code VARCHAR(50) NULL,
  scenario VARCHAR(80) NULL,
  question VARCHAR(300) NULL,
  answer TEXT NULL,
  content TEXT NOT NULL,
  keywords VARCHAR(300) NULL,
  priority INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  version_no INT NOT NULL DEFAULT 1,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_doc_category FOREIGN KEY (category_id) REFERENCES knowledge_category(id),
  CONSTRAINT fk_doc_created_by FOREIGN KEY (created_by) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT fk_doc_updated_by FOREIGN KEY (updated_by) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT ck_doc_type CHECK (doc_type IN ('FAQ', 'POLICY', 'SCRIPT', 'NOTICE')),
  CONSTRAINT ck_doc_status CHECK (status IN ('ENABLED', 'DISABLED')),
  CONSTRAINT ck_doc_deleted CHECK (deleted IN (0, 1)),
  INDEX idx_doc_category (category_id),
  INDEX idx_doc_intent (intent_code),
  INDEX idx_doc_status (status, deleted),
  INDEX idx_doc_priority (priority),
  FULLTEXT INDEX ft_doc_search (title, question, content, keywords)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS chat_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_no VARCHAR(40) NOT NULL,
  user_id BIGINT NULL,
  order_id BIGINT NULL,
  title VARCHAR(120) NULL,
  channel VARCHAR(30) NOT NULL DEFAULT 'WEB',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  current_intent VARCHAR(50) NULL,
  summary VARCHAR(1000) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  closed_at DATETIME NULL,
  CONSTRAINT uk_session_no UNIQUE (session_no),
  CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT fk_session_order FOREIGN KEY (order_id) REFERENCES demo_order(id) ON DELETE SET NULL,
  CONSTRAINT ck_session_channel CHECK (channel IN ('WEB', 'APP', 'MINI_PROGRAM', 'ADMIN_TEST')),
  CONSTRAINT ck_session_status CHECK (status IN ('ACTIVE', 'CLOSED')),
  INDEX idx_session_user (user_id),
  INDEX idx_session_order (order_id),
  INDEX idx_session_status (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @chat_session_channel_check_exists = (
  SELECT COUNT(*)
  FROM information_schema.check_constraints
  WHERE constraint_schema = DATABASE()
    AND constraint_name = 'ck_session_channel'
);
SET @chat_session_channel_drop_sql = IF(
  @chat_session_channel_check_exists > 0,
  'ALTER TABLE chat_session DROP CHECK ck_session_channel',
  'SELECT 1'
);
PREPARE chat_session_channel_drop_stmt FROM @chat_session_channel_drop_sql;
EXECUTE chat_session_channel_drop_stmt;
DEALLOCATE PREPARE chat_session_channel_drop_stmt;

ALTER TABLE chat_session
  ADD CONSTRAINT ck_session_channel CHECK (channel IN ('WEB', 'APP', 'MINI_PROGRAM', 'ADMIN_TEST'));

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL,
  content TEXT NOT NULL,
  message_type VARCHAR(30) NOT NULL DEFAULT 'TEXT',
  seq_no INT NOT NULL,
  reply_to_id BIGINT NULL,
  intent_code VARCHAR(50) NULL,
  source_type VARCHAR(30) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_message_session FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE,
  CONSTRAINT fk_message_reply_to FOREIGN KEY (reply_to_id) REFERENCES chat_message(id) ON DELETE SET NULL,
  CONSTRAINT ck_message_role CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),
  CONSTRAINT ck_message_type CHECK (message_type IN ('TEXT', 'TIP', 'ERROR')),
  CONSTRAINT ck_message_source_type CHECK (source_type IS NULL OR source_type IN ('RULE_TEMPLATE', 'AI_ENHANCED', 'FALLBACK')),
  CONSTRAINT uk_message_session_seq UNIQUE (session_id, seq_no),
  INDEX idx_message_session (session_id, seq_no),
  INDEX idx_message_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS intent_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  message_id BIGINT NOT NULL,
  intent_code VARCHAR(50) NOT NULL,
  intent_name VARCHAR(80) NOT NULL,
  confidence DECIMAL(5,4) NULL,
  method VARCHAR(30) NOT NULL,
  slots_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_intent_session FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE,
  CONSTRAINT fk_intent_message FOREIGN KEY (message_id) REFERENCES chat_message(id) ON DELETE CASCADE,
  CONSTRAINT ck_intent_code CHECK (intent_code IN ('PRE_SALE', 'RETURN_APPLY', 'EXCHANGE_APPLY', 'REFUND_PROGRESS', 'LOGISTICS_QUERY', 'RULE_EXPLAIN', 'COMPLAINT_TRANSFER')),
  CONSTRAINT ck_intent_method CHECK (method IN ('RULE', 'AI', 'HYBRID')),
  INDEX idx_intent_message (message_id),
  INDEX idx_intent_session (session_id),
  INDEX idx_intent_code (intent_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS retrieval_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  message_id BIGINT NOT NULL,
  query_text VARCHAR(500) NOT NULL,
  doc_id BIGINT NULL,
  rank_no INT NOT NULL,
  score DECIMAL(8,4) NULL,
  hit_reason VARCHAR(300) NULL,
  doc_title_snapshot VARCHAR(150) NULL,
  doc_content_snapshot TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_retrieval_session FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE,
  CONSTRAINT fk_retrieval_message FOREIGN KEY (message_id) REFERENCES chat_message(id) ON DELETE CASCADE,
  CONSTRAINT fk_retrieval_doc FOREIGN KEY (doc_id) REFERENCES knowledge_doc(id) ON DELETE SET NULL,
  INDEX idx_retrieval_message (message_id),
  INDEX idx_retrieval_doc (doc_id),
  INDEX idx_retrieval_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ai_call_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  message_id BIGINT NOT NULL,
  provider VARCHAR(50) NULL,
  model_name VARCHAR(80) NULL,
  request_summary TEXT NULL,
  response_summary TEXT NULL,
  status VARCHAR(20) NOT NULL,
  prompt_tokens INT NULL,
  completion_tokens INT NULL,
  latency_ms INT NULL,
  error_message VARCHAR(1000) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_session FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE,
  CONSTRAINT fk_ai_message FOREIGN KEY (message_id) REFERENCES chat_message(id) ON DELETE CASCADE,
  CONSTRAINT ck_ai_status CHECK (status IN ('SUCCESS', 'FAILED', 'SKIPPED')),
  INDEX idx_ai_message (message_id),
  INDEX idx_ai_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS service_ticket (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_no VARCHAR(40) NOT NULL,
  session_id BIGINT NOT NULL,
  message_id BIGINT NULL,
  order_id BIGINT NULL,
  user_id BIGINT NULL,
  intent_code VARCHAR(50) NULL,
  priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  customer_issue VARCHAR(500) NOT NULL,
  ai_summary VARCHAR(1000) NULL,
  suggested_action VARCHAR(1000) NULL,
  assigned_to VARCHAR(80) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  resolved_at DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  CONSTRAINT uk_ticket_no UNIQUE (ticket_no),
  CONSTRAINT fk_ticket_session FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE,
  CONSTRAINT fk_ticket_message FOREIGN KEY (message_id) REFERENCES chat_message(id) ON DELETE SET NULL,
  CONSTRAINT fk_ticket_order FOREIGN KEY (order_id) REFERENCES demo_order(id) ON DELETE SET NULL,
  CONSTRAINT fk_ticket_user FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT ck_ticket_intent CHECK (intent_code IS NULL OR intent_code IN ('PRE_SALE', 'RETURN_APPLY', 'EXCHANGE_APPLY', 'REFUND_PROGRESS', 'LOGISTICS_QUERY', 'RULE_EXPLAIN', 'COMPLAINT_TRANSFER')),
  CONSTRAINT ck_ticket_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
  CONSTRAINT ck_ticket_status CHECK (status IN ('PENDING', 'PROCESSING', 'RESOLVED', 'CLOSED')),
  CONSTRAINT ck_ticket_deleted CHECK (deleted IN (0, 1)),
  INDEX idx_ticket_session (session_id, status),
  INDEX idx_ticket_order (order_id),
  INDEX idx_ticket_status (status, priority, created_at),
  INDEX idx_ticket_intent (intent_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS reply_draft (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  application_id BIGINT NOT NULL,
  ticket_id BIGINT NULL,
  draft_content TEXT NOT NULL,
  source_type VARCHAR(30) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
  risk_level VARCHAR(20) NOT NULL DEFAULT 'LOW',
  knowledge_refs VARCHAR(1000) NULL,
  ai_status VARCHAR(20) NULL,
  ai_provider VARCHAR(50) NULL,
  ai_model_name VARCHAR(80) NULL,
  audit_remark VARCHAR(500) NULL,
  created_by BIGINT NULL,
  used_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_reply_draft_application FOREIGN KEY (application_id) REFERENCES after_sale_application(id) ON DELETE CASCADE,
  CONSTRAINT fk_reply_draft_ticket FOREIGN KEY (ticket_id) REFERENCES service_ticket(id) ON DELETE SET NULL,
  CONSTRAINT fk_reply_draft_creator FOREIGN KEY (created_by) REFERENCES user_account(id) ON DELETE SET NULL,
  CONSTRAINT ck_reply_draft_source CHECK (source_type IN ('AI', 'TEMPLATE', 'MANUAL')),
  CONSTRAINT ck_reply_draft_status CHECK (status IN ('DRAFT', 'USED', 'DISCARDED')),
  CONSTRAINT ck_reply_draft_risk CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
  CONSTRAINT ck_reply_draft_ai_status CHECK (ai_status IS NULL OR ai_status IN ('SUCCESS', 'FAILED', 'SKIPPED')),
  INDEX idx_reply_draft_application (application_id, status, created_at),
  INDEX idx_reply_draft_ticket (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS service_review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  application_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  rating INT NOT NULL,
  tags VARCHAR(200) NULL,
  comment VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_service_review_application UNIQUE (application_id),
  CONSTRAINT fk_service_review_application FOREIGN KEY (application_id) REFERENCES after_sale_application(id) ON DELETE CASCADE,
  CONSTRAINT fk_service_review_user FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE CASCADE,
  CONSTRAINT ck_service_review_rating CHECK (rating BETWEEN 1 AND 5),
  INDEX idx_service_review_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS process_trace (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  message_id BIGINT NOT NULL,
  step_name VARCHAR(50) NOT NULL,
  step_status VARCHAR(20) NOT NULL,
  detail_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_trace_session FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE,
  CONSTRAINT fk_trace_message FOREIGN KEY (message_id) REFERENCES chat_message(id) ON DELETE CASCADE,
  CONSTRAINT ck_trace_step_status CHECK (step_status IN ('SUCCESS', 'FAILED', 'SKIPPED')),
  INDEX idx_trace_message (message_id),
  INDEX idx_trace_session (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
