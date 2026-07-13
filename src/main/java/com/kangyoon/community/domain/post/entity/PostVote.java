package com.kangyoon.community.domain.post.entity;

import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "post_vote",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_postvote_memberId_postId",
                columnNames = {"post_id, member_id"}
        )
)
@Getter
@NoArgsConstructor
public class PostVote extends BaseTimeEntity {

    @Builder
    public PostVote(Post post, Member member, VoteType voteType) {
        this.post = post;
        this.member = member;
        this.voteType = voteType;
    }



    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "post_id", nullable = false) private Post post;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "member_id", nullable = false) private Member member;
    private VoteType voteType;

}
