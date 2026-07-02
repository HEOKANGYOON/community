package com.kangyoon.community.domain.post.repository;

import com.kangyoon.community.domain.post.entity.Post;
import com.kangyoon.community.domain.post.entity.QPost;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Post> findPostsByBoard(Long boardId, String keyword, String searchType, Pageable pageable) {

        List<Post> content = queryFactory
                .selectFrom(QPost.post)
                .where(
                        QPost.post.board.id.eq(boardId),
                        QPost.post.deletedAt.isNull(),
                        searchFilter(keyword, searchType)
                )
                .orderBy(QPost.post.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(QPost.post.count())
                .from(QPost.post)
                .where(
                        QPost.post.board.id.eq(boardId),
                        QPost.post.deletedAt.isNull(),
                        searchFilter(keyword, searchType)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
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

    private BooleanExpression searchFilter(String keyword, String searchType) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        if ("title".equals(searchType)) {
            return QPost.post.title.contains(keyword);
        }
        if ("all".equals(searchType)) {
            return QPost.post.title.contains(keyword).or(QPost.post.content.contains(keyword));
        }

        return null;    //searchType이 없거나 이상한 값이면 null
    }

}
