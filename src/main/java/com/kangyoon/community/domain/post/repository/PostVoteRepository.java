package com.kangyoon.community.domain.post.repository;

import com.kangyoon.community.domain.post.entity.PostVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostVoteRepository extends JpaRepository<PostVote, Long>, PostVoteRepositoryCustom{
    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    @Query("SELECT DISTINCT pv.post.id FROM PostVote pv WHERE pv.createdAt >= :since")
    List<Long> findDistinctPostIdsSince(@Param("since") LocalDateTime since);
}
