-- SCRIPT INFORMATION --
-- Types: mysql mariadb
-- Version: 1
-- Upgrades: 0
-- SCRIPT INFORMATION --

START TRANSACTION;
SET foreign_key_checks = 0;
DROP TABLE IF EXISTS ivory_groups;
DROP TABLE IF EXISTS ivory_group_members;
DROP TABLE IF EXISTS ivory_group_invites;
DROP TABLE IF EXISTS ivory_group_mutes;
DROP TABLE IF EXISTS ivory_player_mutes;
DROP TABLE IF EXISTS ivory_sub_groups;
DROP TABLE IF EXISTS ivory_player_settings;
DROP TABLE IF EXISTS ivory_tokens;

CREATE TABLE ivory_groups (
  group_id      BIGINT      NOT NULL        AUTO_INCREMENT,
  group_name    VARCHAR(32) NOT NULL,
  creator_puuid BINARY(16)  NOT NULL,
  created       TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (group_id),
  FOREIGN KEY (creator_puuid) REFERENCES nucleus_usernames (puuid)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  UNIQUE (group_name)
);

CREATE TABLE ivory_sub_groups (
  parent_id BIGINT NOT NULL,
  child_id  BIGINT NOT NULL,
  UNIQUE (parent_id, child_id),
  UNIQUE (child_id),
  FOREIGN KEY (parent_id) REFERENCES ivory_groups (group_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  FOREIGN KEY (child_id) REFERENCES ivory_groups (group_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);

CREATE TABLE ivory_group_members (
  group_id         BIGINT     NOT NULL,
  puuid            BINARY(16) NOT NULL,
  rank             INTEGER    NOT NULL,
  jointime         TIMESTAMP  NOT NULL        DEFAULT CURRENT_TIMESTAMP,
  invited_by_puuid BINARY(16)                 DEFAULT NULL,
  hidden_bool      BOOLEAN    NOT NULL        DEFAULT FALSE,
  PRIMARY KEY (group_id, puuid),
  FOREIGN KEY (group_id) REFERENCES ivory_groups (group_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  FOREIGN KEY (puuid) REFERENCES nucleus_usernames (puuid)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  FOREIGN KEY (invited_by_puuid) REFERENCES nucleus_usernames (puuid)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);

CREATE TABLE ivory_group_invites (
  group_id      BIGINT     NOT NULL,
  puuid         BINARY(16) NOT NULL,
  rank          INTEGER    NOT NULL,
  inviter_puuid BINARY(16) NOT NULL,
  invitetime    TIMESTAMP  NOT NULL        DEFAULT CURRENT_TIMESTAMP,
  expires       TIMESTAMP  NOT NULL,
  PRIMARY KEY (puuid, group_id),
  FOREIGN KEY (group_id) REFERENCES ivory_groups (group_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  FOREIGN KEY (puuid) REFERENCES nucleus_usernames (puuid)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  FOREIGN KEY (inviter_puuid) REFERENCES nucleus_usernames (puuid)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);

CREATE TABLE ivory_group_mutes (
  group_id BIGINT     NOT NULL,
  puuid    BINARY(16) NOT NULL,
  PRIMARY KEY (puuid, group_id),
  FOREIGN KEY (group_id) REFERENCES ivory_groups (group_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  FOREIGN KEY (puuid) REFERENCES nucleus_usernames (puuid)
    ON UPDATE CASCADE
    ON DELETE CASCADE
);

CREATE TABLE ivory_player_mutes (
  muted_puuid BINARY(16) NOT NULL,
  puuid       BINARY(16) NOT NULL,
  PRIMARY KEY (puuid, muted_puuid),
  FOREIGN KEY (muted_puuid) REFERENCES nucleus_usernames (puuid),
  FOREIGN KEY (puuid) REFERENCES nucleus_usernames (puuid)
);

CREATE TABLE ivory_player_settings (
  puuid           BINARY(16) NOT NULL,
  autoaccept_bool BOOLEAN    NOT NULL DEFAULT FALSE,
  PRIMARY KEY (puuid),
  FOREIGN KEY (puuid) REFERENCES nucleus_usernames (puuid)
);

CREATE TABLE ivory_tokens (
  group_id BIGINT       NOT NULL,
  token    VARCHAR(128) NOT NULL,
  rank     INTEGER      NOT NULL,
  usages   INTEGER      NOT NULL,
  creator  BINARY(16)   NOT NULL,
  FOREIGN KEY (group_id) REFERENCES ivory_groups (group_id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  FOREIGN KEY (creator) REFERENCES nucleus_usernames (puuid)
    ON UPDATE CASCADE
    ON DELETE CASCADE,
  PRIMARY KEY (group_id, token)
);

INSERT INTO ivory_groups (group_name, creator_puuid, created) VALUES ('ORPHANED', 0x0, CURRENT_TIMESTAMP);

SET foreign_key_checks = 1;
COMMIT;