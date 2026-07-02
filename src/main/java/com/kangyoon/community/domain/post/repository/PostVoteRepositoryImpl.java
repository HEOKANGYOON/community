package com.kangyoon.community.domain.post.repository;

import com.kangyoon.community.domain.post.entity.QPostVote;
import com.kangyoon.community.domain.post.entity.VoteType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PostVoteRepositoryImpl implements PostVoteRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Map<Long, Integer> countByPostIdsAndVoteType(List<Long> postIds, VoteType voteType) {
        return queryFactory
                .select(QPostVote.postVote.post.id, QPostVote.postVote.count())
                .from(QPostVote.postVote)
                .where(
                        QPostVote.postVote.post.id.in(postIds),
                        QPostVote.postVote.voteType.eq(voteType)
                )
                .groupBy(QPostVote.postVote.post.id)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(QPostVote.postVote.post.id),
                        tuple -> {
                            Long count = tuple.get(QPostVote.postVote.count());
                            return count != null ? count.intValue() : 0;
                        }
                ));
    }


}
