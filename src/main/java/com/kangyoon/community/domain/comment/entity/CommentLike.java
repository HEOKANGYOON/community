package com.kangyoon.community.domain.comment.entity;

import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "comment_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_comment_like_memberId_commentId",
                columnNames = {"member_id, comment_id"}
        )
)
@Getter
@NoArgsConstructor
public class CommentLike extends BaseTimeEntity {

    public CommentLike(Member member, Comment comment) {
        this.member = member;
        this.comment = comment;
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    //굳이 연관을 써야하나? 이걸로 객체 그래프 탐색을 할 건가?
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "member_id", nullable = false) private Member member;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "comment_id", nullable = false)private Comment comment;

}
