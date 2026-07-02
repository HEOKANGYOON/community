package com.kangyoon.community.domain.comment.repository;

import java.util.List;
import java.util.Map;

public interface CommentLikeRepositoryCustom {
    Map<Long, Integer> countByCommentIds(List<Long> commentIds);
}
