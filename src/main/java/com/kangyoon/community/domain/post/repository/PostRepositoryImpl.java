package com.kangyoon.community.domain.post.repository;

import com.kangyoon.community.domain.post.entity.Post;
import com.kangyoon.community.domain.post.entity.QPost;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Post> findPostsByBoard(Long boardId, Pageable pageable) {
        return queryFactory
                .selectFrom(QPost.post)
                .where(
                        QPost.post.board.id.eq(boardId),
                        QPost.post.deletedAt.isNull()
                )
                .orderBy(QPost.post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public Optional<Post> findActivePostById(Long postId) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(QPost.post)
                        .where(
                                QPost.post.id.eq(postId),
                                QPost.post.deletedAt.isNull()
                        )
                        .fetchOne()
        );
    }

}
