package com.kangyoon.community.domain.post.entity;

import com.kangyoon.community.domain.board.entity.Board;
import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.global.config.BaseEntity;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    private Post(Member member, Board board, String title, String content) {
        this.member = member;
        this.board = board;
        this.title = title;
        this.content = content;
        this.viewCount = 0;
        this.recommendationCount = 0;
        this.disrecommendationCount = 0;
        this. deletedAt = null;
    }

    public static Post createPost(Member member, Board board, String title, String content) {
        return new Post(member, board, title, content);
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "board_id", nullable = false) private Board board;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "member_id", updatable = false) private Member member;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String content;
    @Column(nullable = false) private int viewCount;
    @Column(nullable = false) private int recommendationCount;
    @Column(nullable = false) private int disrecommendationCount;
    private LocalDateTime deletedAt;



    public void editPost(String title, String content) {
        this.title = title;
        this.content = content;
    }


    public void deletePost() {
        if (this.deletedAt != null) {
            throw new CustomException(ErrorCode.POST_ALREADY_DELETED);
        }

        this.deletedAt = LocalDateTime.now();
    }


}
