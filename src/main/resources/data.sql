INSERT INTO member(email, password, nickname, provider, provider_id, role, created_at, updated_at, deleted_at)
VALUES (
        'admin@admin.com',
        '$2a$10$ALaeYlcO8jDkyemSi1x/zu1RTt3VY1Piz705XGSEDLi0XlDTrgo3e',
        'ADMIN',
        'LOCAL',
        null,
        'ADMIN',
        NOW(),
        null,
        null
       );

INSERT INTO board_category(name)
VALUES (
        '자유'
       );


