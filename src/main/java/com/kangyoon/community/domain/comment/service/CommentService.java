package com.kangyoon.community.domain.comment.service;

import com.kangyoon.community.domain.Role;
import com.kangyoon.community.domain.board.repository.BoardManagerRepository;
import com.kangyoon.community.domain.comment.dto.CommentDetailResponse;
import com.kangyoon.community.domain.comment.dto.CommentsResponse;
import com.kangyoon.community.domain.comment.entity.Comment;
import com.kangyoon.community.domain.comment.entity.CommentLike;
import com.kangyoon.community.domain.comment.repository.CommentLikeRepository;
import com.kangyoon.community.domain.comment.repository.CommentRepository;
import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.domain.member.repository.MemberRepository;
import com.kangyoon.community.domain.post.entity.Post;
import com.kangyoon.community.domain.post.repository.PostRepository;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import com.kangyoon.community.infrastructure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final BoardManagerRepository boardManagerRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final RedisService redisService;

    //게시글의 전체 댓글 조회
    @Transactional(readOnly = true)
    public Page<CommentsResponse> getComments(Long postId, Pageable pageable) {

        Page<Comment> comments = commentRepository.findCommentsByPostId(postId, pageable);

        List<Long> commentIds = comments.getContent().stream()
                .map(Comment::getId)
                .toList();

        Map<Long, Integer> commnetLikeMap = redisService.getCommentLikeList(commentIds);

        return comments.map(comment -> CommentsResponse.from(
                comment,
                commnetLikeMap.getOrDefault(comment.getId(), comment.getLikeCount())
        ));
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

    public void commentLikeToggle(Long commentId, Long memberId) {

        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        Optional<CommentLike> existing = commentLikeRepository.findByMemberIdAndCommentId(memberId, commentId);

//      현재는 flush 비용보다 단순성이 더 중요
//      향후 AFTER_COMMIT 이벤트로 정합성 개선 예정
        if (existing.isPresent()) {
            commentLikeRepository.delete(existing.get());
            redisService.decreaseCommentLike(commentId);
        } else {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            CommentLike commentLike = new CommentLike(member, comment);

            try {
                commentLikeRepository.save(commentLike);
            } catch (DataIntegrityViolationException e) {
                return;
            }
            redisService.increaseCommentLike(commentId);
        }
    }


}
