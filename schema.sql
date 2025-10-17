use posthere;

CREATE TABLE user_info (
  user_info_pk       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  email              VARCHAR(255) NOT NULL,
  login_pw           VARCHAR(255) NOT NULL,
  nickname           VARCHAR(50)  NOT NULL,
  profile_photo_url  VARCHAR(500),
  created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  fcm_token 		 VARCHAR(512) NULL,
  UNIQUE KEY uk_nickname_unique (nickname),
  UNIQUE KEY uk_user_email    (email)
);

CREATE TABLE following (
  following_pk BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  follower_id  BIGINT UNSIGNED NOT NULL,
  followed_id  BIGINT UNSIGNED NOT NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_following_follower
    FOREIGN KEY (follower_id) REFERENCES user_info(user_info_pk)
    ON DELETE CASCADE,
  CONSTRAINT fk_following_followed
    FOREIGN KEY (followed_id) REFERENCES user_info(user_info_pk)
    ON DELETE CASCADE,
  UNIQUE KEY uk_follow_pair (follower_id, followed_id),
  INDEX ix_followers_feed (followed_id, created_at DESC, following_pk DESC),
  INDEX ix_followings_feed (follower_id, created_at DESC, following_pk DESC)
);

CREATE TABLE push_subscription (
  push_subscription_pk BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id              BIGINT UNSIGNED NOT NULL,
  endpoint             VARCHAR(1000) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  p256dh               VARCHAR(255)  CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  auth                 VARCHAR(255)  CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT pk_push_subscription PRIMARY KEY (push_subscription_pk),
  CONSTRAINT uq_push_sub_user_endpoint UNIQUE KEY (user_id, endpoint),
  CONSTRAINT fk_push_subscription_user
    FOREIGN KEY (user_id) REFERENCES user_info(user_info_pk)
    ON DELETE CASCADE,
  INDEX idx_push_subscription_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE forum_area (
                            forum_area_pk BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                            address       VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE forum (
  forum_pk       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  writer_id      BIGINT UNSIGNED NOT NULL,
  location       BIGINT UNSIGNED NOT NULL,
  contents_text  TEXT NOT NULL,
  CONSTRAINT fk_forum_writer
    FOREIGN KEY (writer_id) REFERENCES user_info(user_info_pk)
    ON DELETE CASCADE,
  CONSTRAINT fk_forum_location
	FOREIGN KEY (location) REFERENCES forum_area(forum_area_pk)
);

CREATE TABLE forum_img (
  img_pk    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  forum_id  BIGINT UNSIGNED NOT NULL,
  img_url   VARCHAR(500) NOT NULL,
  CONSTRAINT fk_forum_img_forum
    FOREIGN KEY (forum_id) REFERENCES forum(forum_pk)
    ON DELETE CASCADE,
  KEY ix_forum_img_forum (forum_id)
);

CREATE TABLE forum_like (
  forum_like_pk BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  forum_id      BIGINT UNSIGNED NOT NULL,
  forum_liker_id BIGINT UNSIGNED NOT NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_like_forum
    FOREIGN KEY (forum_id) REFERENCES forum(forum_pk)
    ON DELETE CASCADE,
  CONSTRAINT fk_like_user
    FOREIGN KEY (forum_liker_id) REFERENCES user_info(user_info_pk)
    ON DELETE CASCADE,
  UNIQUE KEY uk_like_unique (forum_id, forum_liker_id),
  KEY ix_like_user (forum_liker_id)
);


CREATE TABLE forum_comment (
  forum_comment_pk BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  forum_id         BIGINT UNSIGNED NOT NULL,
  writer_id        BIGINT UNSIGNED NOT NULL,
  contents_text    TEXT NOT NULL,
  CONSTRAINT fk_comment_forum
    FOREIGN KEY (forum_id) REFERENCES forum(forum_pk)
    ON DELETE CASCADE,
  CONSTRAINT fk_comment_writer
    FOREIGN KEY (writer_id) REFERENCES user_info(user_info_pk)
    ON DELETE CASCADE,
  KEY ix_comment_forum (forum_id)
);

CREATE TABLE find (
  find_pk               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  writer_id             BIGINT UNSIGNED NOT NULL,
  coordinates           POINT NOT NULL SRID 4326,
  content_capture_url   VARCHAR(500) NOT NULL,
  created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expiration_date       TIMESTAMP NOT NULL,
  content_overwrite_url VARCHAR(500) NOT NULL,
  CONSTRAINT fk_find_writer
    FOREIGN KEY (writer_id) REFERENCES user_info(user_info_pk)
    ON DELETE CASCADE,
  KEY ix_find_writer (writer_id),
  SPATIAL INDEX ix_find_geo (coordinates)
);


CREATE TABLE park (
  park_pk             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  owner_id            BIGINT UNSIGNED NOT NULL,
  content_capture_url VARCHAR(500) NOT NULL,
  updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_park_owner
    FOREIGN KEY (owner_id) REFERENCES user_info(user_info_pk)
    ON DELETE CASCADE,
  KEY ix_park_owner (owner_id)
);

CREATE TABLE notification (
  notification_pk BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  notification_code VARCHAR(20) NOT NULL, 
  following_id    BIGINT UNSIGNED,        
  comment_id      BIGINT UNSIGNED,           
  park_id         BIGINT UNSIGNED,        
  target_user_id  BIGINT UNSIGNED NOT NULL, 
  check_status    TINYINT NOT NULL DEFAULT 0, 
  message_for_find varchar(100),
  CONSTRAINT fk_notif_followingfind
    FOREIGN KEY (following_id) REFERENCES following(following_pk)
    ON DELETE SET NULL,
  CONSTRAINT fk_notif_comment
    FOREIGN KEY (comment_id) REFERENCES forum_comment(forum_comment_pk)
    ON DELETE SET NULL,
  CONSTRAINT fk_notif_park
    FOREIGN KEY (park_id) REFERENCES park(park_pk)
    ON DELETE SET NULL,
  CONSTRAINT fk_notif_target_user
    FOREIGN KEY (target_user_id) REFERENCES user_info(user_info_pk)
    ON DELETE CASCADE,
  KEY ix_notif_target (target_user_id),
  INDEX ix_notif_target_unread (target_user_id, check_status, created_at DESC, notification_pk DESC),
  INDEX ix_notif_target_created (target_user_id, created_at DESC, notification_pk DESC)
);
