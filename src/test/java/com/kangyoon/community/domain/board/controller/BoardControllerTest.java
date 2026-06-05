package com.kangyoon.community.domain.board.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangyoon.community.domain.board.dto.BoardCreateRequest;
import com.kangyoon.community.domain.board.dto.BoardResponse;
import com.kangyoon.community.domain.board.dto.BoardUpdateRequest;
import com.kangyoon.community.domain.board.entity.Board;
import com.kangyoon.community.domain.board.entity.BoardCategory;
import com.kangyoon.community.domain.board.service.BoardService;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import com.kangyoon.community.global.exception.GlobalExceptionHandler;
import com.kangyoon.community.global.security.CustomUserDetailsService;
import com.kangyoon.community.global.security.JwtProvider;
import com.kangyoon.community.global.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

@WebMvcTest(BoardController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
public class BoardControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private BoardService boardService;
    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    //"api/admin/boards/**"

    @Test
    @WithMockUser(roles = "ADMIN")
    void ADMIN_게시판_생성_성공() throws Exception {
        //given
        BoardCreateRequest boardCreateRequest = new BoardCreateRequest("테스트게시판", "테스트게시판입니다.", 1L);

        //when
        mockMvc.perform(post("/api/admin/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boardCreateRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("게시판 생성 성공"));
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void ADMIN_없는_카테고리로_게시판_생성_실패() throws Exception {
        //given
        BoardCreateRequest boardCreateRequest = new BoardCreateRequest("테스트게시판", "테스트게시판입니다.", 1L);
        willThrow(new CustomException(ErrorCode.BOARD_CATEGORY_NOT_FOUND)).given(boardService).createBoard(any(), any(), any());

        //when
        mockMvc.perform(post("/api/admin/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boardCreateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("카테고리를 찾을 수 없습니다."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ADMIN_게시판명이_중복되면_게시판_생성_실패() throws Exception {
        //given
        BoardCreateRequest boardCreateRequest = new BoardCreateRequest("테스트게시판", "테스트게시판입니다.", 1L);
        willThrow(new CustomException(ErrorCode.DUPLICATE_BOARD_NAME)).given(boardService).createBoard(any(), any(), any());

        //when
        mockMvc.perform(post("/api/admin/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boardCreateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 존재하는 게시판 이름입니다."));
    }

    @Test
    @WithMockUser(roles = "USER")
    void 일반사용자의_게시판생성_접근시_403반환() throws Exception {
        //given
        BoardCreateRequest boardCreateRequest = new BoardCreateRequest("테스트게시판", "테스트게시판입니다.", 1L);

        //when
        mockMvc.perform(post("/api/admin/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boardCreateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ADMIN_게시판_수정_성공() throws Exception {
        //given
        BoardUpdateRequest request = new BoardUpdateRequest("수정한게시판", "수정한게시판입니다.", 1L);

        //when
        mockMvc.perform(patch("/api/admin/boards/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ADMIN_없는게시판_수정_실패() throws Exception {
        //given
        BoardUpdateRequest request = new BoardUpdateRequest("수정한게시판", "수정한게시판입니다.", 1L);
        willThrow(new CustomException(ErrorCode.BOARD_NOT_FOUND)).given(boardService).updateBoard(any(), any(), any(), any());

        //when
        mockMvc.perform(patch("/api/admin/boards/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("존재하지 않는 게시판입니다."));
    }

    @Test
    @WithMockUser(roles = "USER")
    void 일반사용자가_게시판_수정_접근시_실패() throws Exception {
        //given
        BoardUpdateRequest request = new BoardUpdateRequest("수정한게시판", "수정한게시판입니다.", 1L);

        //when
        mockMvc.perform(patch("/api/admin/boards/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ADMIN_게시판_삭제_성공() throws Exception {
        //when
        mockMvc.perform(delete("/api/admin/boards/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ADMIN_삭제되거나_없는_게시판_삭제_실패() throws Exception {
        //given
        willThrow(new CustomException(ErrorCode.BOARD_NOT_FOUND)).given(boardService).deleteBoard(any());

        //when
        mockMvc.perform(delete("/api/admin/boards/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("존재하지 않는 게시판입니다."));
    }

    @Test
    @WithMockUser(roles = "USER")
    void 일반사용자가_게시판_삭제_접근시_실패() throws Exception {
        //when
        mockMvc.perform(delete("/api/admin/boards/1"))
                .andExpect(status().isForbidden());
    }

    //"api/boards/**"

    @Test
    void 게시판_단건_조회_성공() throws Exception {
        //given
        BoardCategory category = new BoardCategory();
        ReflectionTestUtils.setField(category, "id", 1L);
        ReflectionTestUtils.setField(category, "name", "자유");

        Board board = Board.create("조회할게시판", "조회할게시판입니다.", category);
        BoardResponse response = BoardResponse.from(board);

        given(boardService.findBoard(any())).willReturn(response);

        //when
        mockMvc.perform(get("/api/boards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("조회 성공"))
                .andExpect(jsonPath("$.data.name").value(response.name()))
                .andExpect(jsonPath("$.data.description").value(response.description()))
                .andExpect(jsonPath("$.data.id").value(response.id()));

    }


    @Test
    void 게시판_단건_조회_실패() throws Exception {
        //given
        given(boardService.findBoard(any())).willThrow(new CustomException(ErrorCode.BOARD_NOT_FOUND));

        //when
        mockMvc.perform(get("/api/boards/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("존재하지 않는 게시판입니다."));
    }

}
