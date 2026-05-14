package com.kangyoon.community.domain.post.entity;

import com.kangyoon.community.domain.board.entity.Board;
import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.global.config.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)      //게시글들이니까 조회하려면 eager로 다 땡겨와야하나.. 아님 lazy해놓고 나중에 필요한것만 eager하거나 fetch 조인해야하나
    @JoinColumn(name = "board_id", nullable = false)
    private Board board;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", updatable = false)
    private Member member;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private String content;
    @Column(nullable = false)
    private int viewCount;
    @Column(nullable = false)
    private int recommendationCount;
    @Column(nullable = false)
    private int disrecommendationCount;
    private LocalDateTime deletedAt;
}
