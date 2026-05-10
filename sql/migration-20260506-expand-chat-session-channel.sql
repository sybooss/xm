USE test3;

ALTER TABLE chat_session
  DROP CHECK ck_session_channel;

UPDATE chat_session
SET channel = 'WEB'
WHERE channel IN ('APP', 'MINI_PROGRAM');

ALTER TABLE chat_session
  ADD CONSTRAINT ck_session_channel CHECK (channel IN ('WEB', 'ADMIN_TEST'));
