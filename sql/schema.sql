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
  CONSTRAINT ck_session_channel CHECK (channel IN ('WEB', 'ADMIN_TEST')),
  CONSTRAINT ck_session_status CHECK (status IN ('ACTIVE', 'CLOSED')),
  INDEX idx_session_user (user_id),
  INDEX idx_session_order (order_id),
  INDEX idx_session_status (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
