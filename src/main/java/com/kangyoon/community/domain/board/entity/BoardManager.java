package com.kangyoon.community.domain.board.entity;

import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.global.config.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class BoardManager extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "member_id", updatable = false) Member member;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "board_id", updatable = false) Board board;

}
