CREATE TABLE IF NOT EXISTS member (
    id          BIGINT          NOT NULL    AUTO_INCREMENT,
    email       VARCHAR(100)    NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    provider    VARCHAR(20),
    provider_id VARCHAR(100),
    nickname    VARCHAR(50)     NOT NULL,
    role        VARCHAR(20)     NOT NULL,
    created_at  DATETIME        NOT NULL,
    updated_at  DATETIME,
    deleted_at  DATETIME,

    PRIMARY KEY (id),
    UNIQUE KEY uk_member_email (email),
    UNIQUE KEY uk_member_nickname(nickname)
);

CREATE TABLE IF NOT EXISTS board_category (
    id          BIGINT          NOT NULL    AUTO_INCREMENT,
    name        VARCHAR(20)     NOT NULL,

    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS board (
    id          BIGINT          NOT NULL    AUTO_INCREMENT,
    category_id BIGINT          NOT NULL,
    name        VARCHAR(50)     NOT NULL,
    description VARCHAR(200)    NOT NULL,
    created_at  DATETIME        NOT NULL,
    updated_at  DATETIME,
    deleted_at  DATETIME,

    PRIMARY KEY (id),
    UNIQUE  KEY uk_board_name (name)
);

CREATE TABLE IF NOT EXISTS board_manager (
    id          BIGINT          NOT NULL    AUTO_INCREMENT,
    member_id   BIGINT          NOT NULL,
    board_id    BIGINT          NOT NULL,
    created_at  DATETIME        NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY us_board_manager (member_id, board_id)
);

CREATE TABLE IF NOT EXISTS post (
    id          BIGINT          NOT NULL    AUTO_INCREMENT,
    board_id    BIGINT          NOT NULL,
    member_id   BIGINT          NOT NULL,
    title       VARCHAR(200)    NOT NULL,
    content     TEXT            NOT NULL,
    view_count  INT             NOT NULL        DEFAULT 0,
    recommendation_count    INT     NOT NULL    DEFAULT 0,
    disrecommendation_count INT     NOT NULL    DEFAULT 0,
    created_at  DATETIME        NOT NULL,
    updated_at  DATETIME,
    deleted_at  DATETIME,

    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS post_vote (
    id          BIGINT          NOT NULL    AUTO_INCREMENT,
    member_id   BIGINT          NOT NULL,
    post_id     BIGINT          NOT NULL,
    vote_type   VARCHAR(20)     NOT NULL,
    created_at  DATETIME        NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_postvote_memberId_postId (member_id, post_id)
);

CREATE TABLE IF NOT EXISTS comment (
    id          BIGINT          NOT NULL    AUTO_INCREMENT,
    post_id     BIGINT          NOT NULL,
    member_id   BIGINT          NOT NULL,
    parent_id   BIGINT,
    content     VARCHAR(500)    NOT NULL,
    like_count  INT             NOT NULL    DEFAULT 0,
    created_at  DATETIME        NOT NULL,
    updated_at  DATETIME,
    deleted_at  DATETIME,

    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS comment_like (
    id          BIGINT          NOT NULL    AUTO_INCREMENT,
    member_id   BIGINT          NOT NULL,
    comment_id  BIGINT          NOT NULL,
    created_at  DATETIME        NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_comment_like_memberId_commentId (member_id, comment_id)
);


CREATE TABLE IF NOT EXISTS notification (
    id          BIGINT          NOT NULL    AUTO_INCREMENT,
    member_id   BIGINT          NOT NULL,
    notification_type   VARCHAR(20)     NOT NULL,
    target_type VARCHAR(20)     NOT NULL,
    target_id   BIGINT          NOT NULL,
    content     VARCHAR(50)     NOT NULL,
    is_read     TINYINT(1)      NOT NULL    DEFAULT 0,
    created_at  DATETIME        NOT NULL,
    deleted_at  DATETIME,

    PRIMARY KEY (id)
);

ALTER TABLE board ADD FOREIGN KEY (category_id) REFERENCES board_category(id);

ALTER TABLE board_manager ADD FOREIGN KEY (member_id) REFERENCES member(id);

ALTER TABLE board_manager ADD FOREIGN KEY (board_id) REFERENCES board(id);

ALTER TABLE post ADD FOREIGN KEY (board_id) REFERENCES board(id);

ALTER TABLE post ADD FOREIGN KEY (member_id) REFERENCES member(id);

ALTER TABLE post_vote ADD FOREIGN KEY (member_id) REFERENCES member(id);

ALTER TABLE post_vote ADD FOREIGN KEY (post_id) REFERENCES post(id);

ALTER TABLE comment ADD FOREIGN KEY (post_id) REFERENCES post(id);

ALTER TABLE comment ADD FOREIGN KEY (member_id) REFERENCES member(id);

ALTER TABLE comment ADD FOREIGN KEY (parent_id) REFERENCES comment(id);

ALTER TABLE comment_like ADD FOREIGN KEY (member_id) REFERENCES member(id);

ALTER TABLE comment_like ADD FOREIGN KEY (comment_id) REFERENCES comment(id);

ALTER TABLE notification ADD FOREIGN KEY (member_id) REFERENCES member(id);