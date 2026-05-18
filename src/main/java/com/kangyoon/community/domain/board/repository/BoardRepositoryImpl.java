package com.kangyoon.community.domain.board.repository;

import com.kangyoon.community.domain.board.entity.Board;
import com.kangyoon.community.domain.board.entity.QBoard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class BoardRepositoryImpl implements BoardRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Board> findAllActiveBoards() {
        return queryFactory
                .selectFrom(QBoard.board)
                .where(QBoard.board.deletedAt.isNull())
                .orderBy(QBoard.board.name.asc())
                .fetch();
    }



}
