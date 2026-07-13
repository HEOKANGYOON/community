package com.kangyoon.community.domain.comment.repository;

import com.kangyoon.community.domain.comment.entity.Comment;
import com.kangyoon.community.domain.comment.entity.QComment;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom{

    private final QComment comment = QComment.comment;
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Comment> findCommentsByPostId(Long postId, Pageable pageable) {
        List<Comment> content = queryFactory
                .selectFrom(comment)
                .join(comment.member).fetchJoin()   //member를 fetch join으로 가져와서 한번의 쿼리로 게시글 목록에서의 게시글 작성자를 표시
                .where(comment.post.id.eq(postId))
                .orderBy(
                        comment.parent.id.coalesce(comment.id).asc(),   // SQL 쿼리는 COALESCE(parentId, commentId)
                        comment.id.asc()    // id ASC
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        //게시글에 포함된 댓글 수 카운트
        JPAQuery<Long> total = queryFactory
                .select(comment.count())
                .from(comment)
                .where(comment.post.id.eq(postId));

        return PageableExecutionUtils.getPage(content, pageable, total::fetchOne);
    }
}
