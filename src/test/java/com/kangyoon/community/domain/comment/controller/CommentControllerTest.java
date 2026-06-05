package com.kangyoon.community.domain.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangyoon.community.domain.board.entity.Board;
import com.kangyoon.community.domain.board.entity.BoardCategory;
import com.kangyoon.community.domain.comment.dto.CommentDetailResponse;
import com.kangyoon.community.domain.comment.dto.CommentWriteRequest;
import com.kangyoon.community.domain.comment.dto.CommentsResponse;
import com.kangyoon.community.domain.comment.entity.Comment;
import com.kangyoon.community.domain.comment.service.CommentService;
import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.domain.post.entity.Post;
import com.kangyoon.community.global.exception.GlobalExceptionHandler;
import com.kangyoon.community.global.security.CustomUserDetails;
import com.kangyoon.community.global.security.CustomUserDetailsService;
import com.kangyoon.community.global.security.JwtProvider;
import com.kangyoon.community.global.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

@WebMvcTest(CommentController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
public class CommentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CommentService commentService;
    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    private Member member;
    private Board board;
    private Post post;
    private BoardCategory boardCategory;

    @BeforeEach
    void setUp() {
        member = Member.createLocal("test@test.com", "encoded-test1234", "테스트사용자");
        ReflectionTestUtils.setField(member, "id", 1L);

        boardCategory = new BoardCategory();
        ReflectionTestUtils.setField(boardCategory, "id", 1L);

        board = Board.create("테스트게시판", "테스트게시판입니다.", boardCategory);
        ReflectionTestUtils.setField(board, "id", 1L);

        post = Post.createPost(member, board, "테스트 게시글", "테스트 게시글입니다.");
        ReflectionTestUtils.setField(post, "id", 1L);
    }

    @Test
    void 전체댓글_조회_200_PageResponse반환() throws Exception{
        //given
        Comment comment = Comment.createComment(post, member, "테스트 댓글");
        ReflectionTestUtils.setField(comment, "id", 1L);

        Page<Comment> page = new PageImpl<>(List.of(comment));
        given(commentService.getComments(anyLong(), any(Pageable.class))).willReturn(page.map(comment1 -> CommentsResponse.from(
                comment,
                0       //댓글 좋아요 0 추천/비추천/좋아요 기능 추가로 인한 구조 변경
        )));


        //when & then
        mockMvc.perform(get("/api/boards/1/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("댓글 조회 성공"))
                .andExpect(jsonPath("$.data.content[0].content").value("테스트 댓글"));
    }

    @Test
    void 댓글_상세_조회_200_CommentDetailRespone_반환() throws Exception{
        Comment comment = Comment.createComment(post, member, "테스트 댓글");
        ReflectionTestUtils.setField(comment, "id", 1L);

        CommentDetailResponse commentDetailResponse = CommentDetailResponse.from(comment);
        given(commentService.commentDetail(anyLong())).willReturn(commentDetailResponse);

        //when & then
        mockMvc.perform(get("/api/boards/1/posts/1/comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("댓글 상세 조회 성공"))
                .andExpect(jsonPath("$.data.postId").value(comment.getPost().getId()))
                .andExpect(jsonPath("$.data.memberId").value(comment.getMember().getId()))
                .andExpect(jsonPath("$.data.content").value(comment.getContent()));
    }


    @Test
    @WithMockUser("USER")
    void 댓글_작성_성공시_201반환() throws Exception{
        CustomUserDetails userDetails = new CustomUserDetails(member);

        CommentWriteRequest commentWriteRequest = new CommentWriteRequest("작성할 댓글");

        //when & then
        mockMvc.perform(post("/api/boards/1/posts/1/comments")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentWriteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("댓글 작성 성공"));
    }

    @Test
    @WithMockUser("USER")
    void content가_빈값이면_400반환() throws Exception{
        CustomUserDetails userDetails = new CustomUserDetails(member);

        CommentWriteRequest commentWriteRequest = new CommentWriteRequest("");

        //when & then
        mockMvc.perform(post("/api/boards/1/posts/1/comments")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentWriteRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser("USER")
    void 대댓글_작성_성공시_201반환() throws Exception{
        CustomUserDetails userDetails = new CustomUserDetails(member);

        CommentWriteRequest commentWriteRequest = new CommentWriteRequest("작성할 대댓글");

        //when & then
        mockMvc.perform(post("/api/boards/1/posts/1/comments/1/replies")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentWriteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("대댓글 작성 성공"));
    }

    @Test
    @WithMockUser("USER")
    void 댓글_수정_성공시_200반환() throws Exception{
        CustomUserDetails userDetails = new CustomUserDetails(member);

        CommentWriteRequest commentWriteRequest = new CommentWriteRequest("수정할 댓글");

        //when & then
        mockMvc.perform(patch("/api/boards/1/posts/1/comments/1")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentWriteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("댓글 수정 성공"));
    }

    @Test
    void 비로그인_수정_시도시_401반환() throws Exception{
        CommentWriteRequest commentWriteRequest = new CommentWriteRequest("수정할 댓글");

        //when & then
        mockMvc.perform(patch("/api/boards/1/posts/1/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentWriteRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser("USER")
    void 댓글_삭제_성공시_200반환() throws Exception{
        CustomUserDetails userDetails = new CustomUserDetails(member);

        //when & then
        mockMvc.perform(delete("/api/boards/1/posts/1/comments/1")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("댓글 삭제 성공"));
    }

    @Test
    void 비로그인_삭제_시도시_401반환() throws Exception{
        //when & then
        mockMvc.perform(delete("/api/boards/1/posts/1/comments/1"))
                .andExpect(status().isUnauthorized());
    }

}
