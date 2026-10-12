-- Reference DDL for the Phase 1 friendship table on MySQL 8.x.
-- The application currently uses spring.jpa.hibernate.ddl-auto=update; review this file
-- against staging before production deployment instead of running it blindly.

CREATE TABLE friend_relationships (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_low_id BIGINT NOT NULL,
    user_high_id BIGINT NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_friend_relationship_pair UNIQUE (user_low_id, user_high_id),
    CONSTRAINT fk_friend_relationship_low
        FOREIGN KEY (user_low_id) REFERENCES users (id),
    CONSTRAINT fk_friend_relationship_high
        FOREIGN KEY (user_high_id) REFERENCES users (id),
    CONSTRAINT fk_friend_relationship_requester
        FOREIGN KEY (requested_by_user_id) REFERENCES users (id),
    CONSTRAINT ck_friend_relationship_order CHECK (user_low_id < user_high_id),
    CONSTRAINT ck_friend_relationship_requester CHECK (
        requested_by_user_id = user_low_id OR requested_by_user_id = user_high_id
    ),
    INDEX ix_friend_relationship_low_status (user_low_id, status),
    INDEX ix_friend_relationship_high_status (user_high_id, status)
) ENGINE=InnoDB;
