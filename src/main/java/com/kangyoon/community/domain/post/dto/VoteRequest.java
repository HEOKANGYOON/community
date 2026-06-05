package com.kangyoon.community.domain.post.dto;

import com.kangyoon.community.domain.post.entity.VoteType;

public record VoteRequest(
        VoteType voteType
) {
}
