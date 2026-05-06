USE test3;

ALTER TABLE chat_session
  DROP CHECK ck_session_channel;

ALTER TABLE chat_session
  ADD CONSTRAINT ck_session_channel CHECK (channel IN ('WEB', 'APP', 'MINI_PROGRAM', 'ADMIN_TEST'));
