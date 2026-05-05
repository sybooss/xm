USE test3;

SET @column_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'user_account'
    AND column_name = 'password_hash'
);

SET @ddl = IF(
  @column_exists = 0,
  'ALTER TABLE user_account ADD COLUMN password_hash VARCHAR(128) NULL AFTER phone',
  'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
