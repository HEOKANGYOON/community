package com.kangyoon.community.domain.comment.repository;

import com.kangyoon.community.domain.comment.entity.QCommentLike;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class CommentLikeRepositoryImpl implements CommentLikeRepositoryCustom{

    private final QCommentLike commentLike = QCommentLike.commentLike;
    private final JPAQueryFactory queryFactory;

    @Override
    public Map<Long, Integer> countByCommentIds(List<Long> commentIds) {
        return queryFactory
                .select(commentLike.comment.id, commentLike.count())
                .from(commentLike)
                .where(commentLike.comment.id.in(commentIds))
                .groupBy(commentLike.comment.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(commentLike.comment.id),
                        tuple -> {
                            Long count = tuple.get(commentLike.count());
                            return count != null ? count.intValue() : 0;
                        }
                ));
    }

}
