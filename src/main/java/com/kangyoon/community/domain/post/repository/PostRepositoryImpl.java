package com.kangyoon.community.domain.post.repository;

import com.kangyoon.community.domain.post.entity.Post;
import com.kangyoon.community.domain.post.entity.QPost;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Post> findPostsByBoard(Long boardId, String keyword, String searchType, Pageable pageable) {
        return findPostsByBoardInternal(boardId, pageable, searchFilter(keyword, searchType));
    }

    //특수문자를 포함한 경우 LIKE검색으로 FALLBACK
    @Override
    public Page<Post> findPostsByBoardLike(Long boardId, String keyword, String searchType, Pageable pageable) {
        return findPostsByBoardInternal(boardId, pageable, searchFilterLike(keyword, searchType));
    }

    private Page<Post> findPostsByBoardInternal(Long boardId, Pageable pageable, BooleanExpression searchCondition) {
        List<Post> content = queryFactory
                .selectFrom(QPost.post)
                .join(QPost.post.member).fetchJoin()
                .where(
                        QPost.post.board.id.eq(boardId),
                        QPost.post.deletedAt.isNull(),
                        searchCondition
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
                        searchCondition
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression searchFilter(String booleanQuery, String searchType) {
        if (booleanQuery == null || booleanQuery.isBlank()) {
            return null;
        }
        if ("title".equals(searchType)) {
            return Expressions.booleanTemplate(
                    "match_against({0}, {1})",
                    QPost.post.title,
                    booleanQuery
            );
        }
        if ("title_content".equals(searchType)) {
            return Expressions.booleanTemplate(
                    "match_against_multi({0}, {1}, {2})",
                    QPost.post.title,
                    QPost.post.content,
                    booleanQuery
            );
        }
        return null;
    }

    private BooleanExpression searchFilterLike(String keyword, String searchType) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        if ("title".equals(searchType)) {
            return QPost.post.title.contains(keyword);
        }
        if ("title_content".equals(searchType)) {
            return QPost.post.title.contains(keyword).or(QPost.post.content.contains(keyword));
        }
        return null;    //searchType이 없거나 이상한 값이면 null
    }
}
