package com.kangyoon.community.domain.comment.service;

import com.kangyoon.community.domain.board.entity.Board;
import com.kangyoon.community.domain.board.entity.BoardCategory;
import com.kangyoon.community.domain.board.repository.BoardManagerRepository;
import com.kangyoon.community.domain.comment.dto.CommentsResponse;
import com.kangyoon.community.domain.comment.entity.Comment;
import com.kangyoon.community.domain.comment.repository.CommentRepository;
import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.domain.member.repository.MemberRepository;
import com.kangyoon.community.domain.post.entity.Post;
import com.kangyoon.community.domain.post.repository.PostRepository;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import com.kangyoon.community.infrastructure.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PostRepository postRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private BoardManagerRepository boardManagerRepository;
    @Mock private RedisService redisService;

    @InjectMocks private CommentService commentService;

    private Member member;
    private Board board;
    private BoardCategory boardCategory;
    private Post post;

    @BeforeEach
    void setUp() {
        member = Member.createLocal("test@test.com", "enccoded-test1234", "테스트사용자");
        ReflectionTestUtils.setField(member, "id", 1L);
        boardCategory = new BoardCategory();
        ReflectionTestUtils.setField(boardCategory, "id", 1L);
        ReflectionTestUtils.setField(boardCategory, "name", "테스트");

        board = Board.create("테스트게시판", "테스트게시판입니다.", boardCategory);
        ReflectionTestUtils.setField(board, "id", 1L);

        post = Post.createPost(member, board, "테스트 게시글", "테스트 게시글입니다.");
        ReflectionTestUtils.setField(post, "id", 1L);
    }


    @Test
    void 댓글_목록_정성_반환() {
        // given
        List<Comment> commentList = List.of(
                Comment.createComment(post, member, "테스트 댓글")
        );

        Page<Comment> comments = new PageImpl<>(commentList);

        Pageable pageable = PageRequest.of(0, 20);

        given(commentRepository.findCommentsByPostId(anyLong(), any(Pageable.class))).willReturn(comments);

        // when
        Page<CommentsResponse> result = commentService.getComments(post.getId(), pageable);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("테스트 댓글");

    }

    @Test
    void 댓글_작성_성공() {
        //given
        Comment comment = Comment.createComment(post, member, "테스트 댓글");

        given(postRepository.findByIdAndDeletedAtIsNull(anyLong())).willReturn(Optional.of(post));
        given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));

        //when
        commentService.commentWrite(post.getId(), member.getId(), comment.getContent());

        //then
        then(commentRepository).should().save(any(Comment.class));

    }

    @Test
    void 없는_게시글에_댓글_작성시_실패() {
        //given
        Comment comment = Comment.createComment(post, member, "테스트 댓글");

        given(postRepository.findByIdAndDeletedAtIsNull(anyLong())).willReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> commentService.commentWrite(post.getId(), member.getId(), comment.getContent()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 대댓글_작성_성공() {
        //given
        Comment parentComment = Comment.createComment(post, member, "테스트 댓글");
        ReflectionTestUtils.setField(parentComment, "id", 1L);

        Comment childComment = Comment.createReply(post, member, parentComment, "테스트 대댓글");


        given(postRepository.findByIdAndDeletedAtIsNull(anyLong())).willReturn(Optional.of(post));
        given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));
        given(commentRepository.findByIdAndDeletedAtIsNull(anyLong())).willReturn(Optional.of(parentComment));

        //when
        commentService.replyWrite(post.getId(), member.getId(), parentComment.getId(), childComment.getContent());

        //then
        then(commentRepository).should().save(any(Comment.class));
    }

    @Test
    void 대댓글에_대댓글을_달려고하면_실패() {
        //given
        Comment parentComment = Comment.createComment(post, member, "테스트 댓글");
        ReflectionTestUtils.setField(parentComment, "id", 1L);

        Comment childComment_A = Comment.createReply(post, member, parentComment, "테스트 대댓글A");
        ReflectionTestUtils.setField(childComment_A, "id", 2L);

        Comment childComment_B = Comment.createReply(post, member, childComment_A, "테스트 대댓글B");


        given(postRepository.findByIdAndDeletedAtIsNull(anyLong())).willReturn(Optional.of(post));
        given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));
        given(commentRepository.findByIdAndDeletedAtIsNull(anyLong())).willReturn(Optional.of(childComment_A));

        //when & then
        assertThatThrownBy(() -> commentService.replyWrite(post.getId(), member.getId(), childComment_A.getId(), childComment_B.getContent()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REPLY_DEPTH_EXCEEDED);

    }

    @Test
    void 없는_게시글에_대댓글_작성시_실패() {
        //given
        Comment parentComment = Comment.createComment(post, member, "테스트 댓글");
        ReflectionTestUtils.setField(parentComment, "id", 1L);

        Comment childComment = Comment.createReply(post, member, parentComment, "테스트 대댓글");

        given(postRepository.findByIdAndDeletedAtIsNull(anyLong())).willReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> commentService.replyWrite(post.getId(), member.getId(), parentComment.getId(), childComment.getContent()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 댓글_수정_성공() {
        //given
        Comment comment = Comment.createComment(post, member, "테스트 댓글");
        ReflectionTestUtils.setField(comment, "id", 1L);

        given(commentRepository.findByIdAndDeletedAtIsNull(anyLong())).willReturn(Optional.of(comment));

        //when
        commentService.commentEdit(comment.getId(), comment.getMember().getId(), "수정된 댓글");

        //then
        assertThat(comment.getContent()).isEqualTo("수정된 댓글");
    }

    @Test
    void 다른사용자의_댓글_수정_실패() {
        //given
        Comment comment = Comment.createComment(post, member, "테스트 댓글");
        ReflectionTestUtils.setField(comment, "id", 1L);

        given(commentRepository.findByIdAndDeletedAtIsNull(anyLong())).willReturn(Optional.of(comment));

        //when & then
        assertThatThrownBy(() -> commentService.commentEdit(comment.getId(), 2L, "수정된 댓글"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_AUTHOR_MISMATCH);
    }

    @Test
    void 댓글작성자의_댓글_삭제_성공() {
        //given
        Comment comment = Comment.createComment(post, member, "테스트 댓글");
        ReflectionTestUtils.setField(comment, "id", 1L);

        given(commentRepository.findByIdAndDeletedAtIsNull(anyLong())).willReturn(Optional.of(comment));
        given(boardManagerRepository.existsByBoardIdAndMemberId(anyLong(), anyLong())).willReturn(false);

        //when
        commentService.commentDelete(comment.getId(), member.getId(), comment.getPost().getBoard().getId(), "USER");

        //then
        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    void 게시판관리자의_댓글_삭제_성공() {
        //given
        Comment comment = Comment.createComment(post, member, "테스트 댓글");
        ReflectionTestUtils.setField(comment, "id", 1L);

        given(commentRepository.findByIdAndDeletedAtIsNull(anyLong())).willReturn(Optional.of(comment));
        given(boardManagerRepository.existsByBoardIdAndMemberId(anyLong(), anyLong())).willReturn(true);

        //when
        commentService.commentDelete(comment.getId(), 30L, comment.getPost().getBoard().getId(), "USER");

        //then
        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    void ADMIN의_댓글_삭제_성공() {
        //given
        Comment comment = Comment.createComment(post, member, "테스트 댓글");
        ReflectionTestUtils.setField(comment, "id", 1L);

        given(commentRepository.findByIdAndDeletedAtIsNull(anyLong())).willReturn(Optional.of(comment));
        given(boardManagerRepository.existsByBoardIdAndMemberId(anyLong(), anyLong())).willReturn(false);

        //when
        commentService.commentDelete(comment.getId(), 30L, comment.getPost().getBoard().getId(), "ADMIN");

        //then
        assertThat(comment.getDeletedAt()).isNotNull();
    }

    @Test
    void 다르사용자의_댓글_삭제_실패() {
        //given
        Comment comment = Comment.createComment(post, member, "테스트 댓글");
        ReflectionTestUtils.setField(comment, "id", 1L);

        given(commentRepository.findByIdAndDeletedAtIsNull(anyLong())).willReturn(Optional.of(comment));
        given(boardManagerRepository.existsByBoardIdAndMemberId(anyLong(), anyLong())).willReturn(false);

        //when & then
        assertThatThrownBy(() -> commentService.commentDelete(comment.getId(), 30L, comment.getPost().getBoard().getId(), "USER"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.COMMENT_AUTHOR_MISMATCH);
    }

}
