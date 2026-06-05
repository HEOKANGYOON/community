package com.kangyoon.community.domain.post.repository;

import com.kangyoon.community.domain.post.entity.PostVote;
import com.kangyoon.community.domain.post.entity.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public interface PostVoteRepository extends JpaRepository<PostVote, Long>, PostVoteRepositoryCustom{
    Optional<PostVote> findByMemberIdAndPostId(Long memberId, Long postId);
    boolean existsByMemberIdAndPostId(Long memberId, Long postId);
    int countByPostIdAndVoteType(Long postId, VoteType voteType);
}
