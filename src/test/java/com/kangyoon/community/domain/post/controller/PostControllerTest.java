package com.kangyoon.community.domain.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangyoon.community.domain.board.entity.Board;
import com.kangyoon.community.domain.board.entity.BoardCategory;
import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.domain.post.dto.PostCreateRequest;
import com.kangyoon.community.domain.post.dto.PostResponse;
import com.kangyoon.community.domain.post.dto.PostUpdateRequest;
import com.kangyoon.community.domain.post.entity.Post;
import com.kangyoon.community.domain.post.service.PostService;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
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

@WebMvcTest(PostController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
public class PostControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private PostService postService;
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
    void 목록_조회_성공() throws Exception{
        //given
        given(postService.getAllPost(any(), any(), any(), any())).willReturn(Page.empty());

        //when & then
        mockMvc.perform(get("/api/boards/1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("리스트 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }
    @Test
    void 단건_조회_성공() throws Exception {
        //조회수, 추천수, 비추천수 임시로 0넣음
        PostResponse postResponse = PostResponse.from(post, 0 ,0, 0);
        given(postService.getPost(any())).willReturn(postResponse);

        //when
        mockMvc.perform(get("/api/boards/1/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("게시글 조회 성공"))
                .andExpect(jsonPath("$.data.postId").value(postResponse.postId()))
                .andExpect(jsonPath("$.data.boardId").value(postResponse.boardId()))
                .andExpect(jsonPath("$.data.title").value(postResponse.title()))
                .andExpect(jsonPath("$.data.content").value(postResponse.content()));
    }

    @Test
    void USER_게시글_작성_성공() throws Exception {
        //given
        CustomUserDetails userDetails = new CustomUserDetails(member);
        PostCreateRequest postCreateRequest = new PostCreateRequest(post.getTitle(), post.getContent());    // BeforeEach에 생성한 객체 값과 동일하게 넣어줌
        //게시글 작성 직후에는 Serviced에서도 똑같이 0으로 내려줌
        PostResponse response = PostResponse.from(post, 0 ,0 ,0);

        given(postService.writePost(any(), any(), any(), any())).willReturn(response);

        //when & then
        mockMvc.perform(post("/api/boards/1/posts")
                .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails))   // SecurityContext에 주입
                        .content(objectMapper.writeValueAsString(postCreateRequest)))
                        .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("게시글 작성 성공"))
                .andExpect(jsonPath("$.data.title").value("테스트 게시글"))
                .andExpect(jsonPath("$.data.content").value("테스트 게시글입니다."));
    }

    @Test
    void 미인증_게시글_작성_실패() throws Exception {
        //given
        PostCreateRequest postCreateRequest = new PostCreateRequest(post.getTitle(), post.getContent());    // BeforeEach에 생성한 객체 값과 동일하게 넣어줌

        //when & then
        mockMvc.perform(post("/api/boards/1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postCreateRequest)))
                .andExpect(status().isUnauthorized())  // 401
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    void 게시글_수정_성공() throws Exception {
        //given
        CustomUserDetails userDetails = new CustomUserDetails(member);

        PostUpdateRequest postUpdateRequest = new PostUpdateRequest("수정한 게시글", "수정한 게시글입니다.");

        ReflectionTestUtils.setField(post, "title", postUpdateRequest.title());
        ReflectionTestUtils.setField(post, "content", postUpdateRequest.content());
        //실제는 redis에서 받아온 조회수, 추천수, 게시글 수를 내려줌
        PostResponse postResponse = PostResponse.from(post, 0, 0, 0);

        given(postService.editPost(any(), any(), any(), any())).willReturn(postResponse);


        //when
        mockMvc.perform(put("/api/boards/1/posts/1")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails))   // SecurityContext에 주입
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("게시글 수정 성공"))
                .andExpect(jsonPath("$.data.title").value("수정한 게시글"))
                .andExpect(jsonPath("$.data.content").value("수정한 게시글입니다."));
    }

    @Test
    void 미인증_게시글_수정_실패() throws Exception {
        //given
        PostUpdateRequest postUpdateRequest = new PostUpdateRequest("수정한 게시글", "수정한 게시글입니다.");

        //when
        mockMvc.perform(put("/api/boards/1/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postUpdateRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));

    }

    @Test
    void USER_작성자가_게시글_삭제_성공() throws Exception {
        //given
        CustomUserDetails userDetails = new CustomUserDetails(member);

        //when
        mockMvc.perform(delete("/api/boards/1/posts/1")
                        .with(SecurityMockMvcRequestPostProcessors.user(userDetails))   // SecurityContext에 주입
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("게시글 삭제 성공"));
    }

    @Test
    void 미인증_게시글_삭제_실패() throws Exception {
        //when
        mockMvc.perform(delete("/api/boards/1/posts/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));

    }


}
