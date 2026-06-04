package com.kangyoon.community.domain.post.repository;

import com.kangyoon.community.domain.post.entity.VoteType;

import java.util.List;
import java.util.Map;

public interface PostVoteRepositoryCustom {
    Map<Long, Integer> countByPostIdsAndVoteType(List<Long> postIds, VoteType voteType);
}
