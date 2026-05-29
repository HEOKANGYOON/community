package com.kangyoon.community.domain.comment.service;

import com.kangyoon.community.domain.Role;
import com.kangyoon.community.domain.board.repository.BoardManagerRepository;
import com.kangyoon.community.domain.comment.dto.CommentDetailResponse;
import com.kangyoon.community.domain.comment.dto.CommentsResponse;
import com.kangyoon.community.domain.comment.entity.Comment;
import com.kangyoon.community.domain.comment.repository.CommentRepository;
import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.domain.member.repository.MemberRepository;
import com.kangyoon.community.domain.post.entity.Post;
import com.kangyoon.community.domain.post.repository.PostRepository;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final BoardManagerRepository boardManagerRepository;

    //게시글의 전체 댓글 조회
    @Transactional(readOnly = true)
    public Page<CommentsResponse> getComments(Long postId, Pageable pageable) {
//        // 원댓글 조회
//        List<Comment> parentComments = commentRepository.findByPostIdAndParentIsNull(postId);
//
//        // 원댓글 DTO 변환 + Map으로 만듦
//        Map<Long, CommentsResponse> parentMap = parentComments.stream()
//                .map(CommentsResponse::from)
//                .collect(toMap(CommentsResponse::getId,
//                        dto -> dto,
//                        (a, b) -> a,
//                        LinkedHashMap::new
//                ));
//
//        // 대댓글 조회
//        List<Long> parentIds = parentComments.stream()
//                .map(Comment::getId)
//                .toList();
//        List<Comment> childrenComments = commentRepository.findBydParentIdInOrderByCreateAtAsc(parentIds);
//
//        // 대댓글 DTO 변환 후 원댓글의 child 리스트에 넣어줌
//        childrenComments.forEach(child -> {
//            CommentsResponse childDto = CommentsResponse.from(child);
//            parentMap.get(child.getParent().getId()).addChild(childDto);
//        });
//
//        return new ArrayList<>(parentMap.values());


        return commentRepository.findCommentsByPostId(postId, pageable)
                .map(CommentsResponse::from);
    }

    @Transactional(readOnly = true)
    public CommentDetailResponse commentDetail(Long commentId) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        return CommentDetailResponse.from(comment);
    }

    public void commentWrite(Long postId, Long memberId, String content) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Comment comment = Comment.createComment(post, member, content);

        commentRepository.save(comment);
    }

    public void replyWrite(Long postId, Long memberId, Long parentId, String content) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Comment parent = commentRepository.findByIdAndDeletedAtIsNull(parentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        //대댓글을 다는 대상이 대댓글인 경우 예외처리(1depth만 허용)
        if (parent.getParent() != null) {
            throw new CustomException(ErrorCode.REPLY_DEPTH_EXCEEDED);
        }

        Comment reply = Comment.createReply(post, member, parent, content);
        commentRepository.save(reply);
    }

    public void commentEdit(Long commentId, Long memberId, String content) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.COMMENT_AUTHOR_MISMATCH);
        }

        comment.commentEdit(content);
    }

    public void commentDelete(Long commentId, Long memberId, Long boardId, String role) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        boolean isBoardManager = boardManagerRepository.existsByBoardIdAndMemberId(boardId, memberId);

        if (!comment.getMember().getId().equals(memberId)
            && !role.equals("ADMIN")
            && !isBoardManager) {
            throw new CustomException(ErrorCode.COMMENT_AUTHOR_MISMATCH);
        }
        comment.commentDelete();
    }


}
