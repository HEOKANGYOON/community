package com.kangyoon.community.domain.comment.entity;

import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.domain.post.entity.Post;
import com.kangyoon.community.global.config.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Builder
    private Comment(Post post, Member member, Comment parent, String content) {
        this.post = post;
        this.member = member;
        this.parent = parent;
        this.content = content;
    }

    public static Comment createComment(Post post, Member member, String content) {
        return Comment.builder()
                .post(post)
                .member(member)
                .content(content)
                .build();
    }

    public static Comment createReply(Post post, Member member, Comment parent, String content) {
        return Comment.builder()
                .post(post)
                .member(member)
                .parent(parent)
                .content(content)
                .build();
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "post_id", nullable = false) private Post post;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "member_id", nullable = false) private Member member;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id") private Comment parent;
    @Column(nullable = false) private String content;
    private LocalDateTime deletedAt;

    public void commentEdit(String content) {
        this.content = content;
    }

    public void commentDelete() {
        this.deletedAt = LocalDateTime.now();
    }

}
