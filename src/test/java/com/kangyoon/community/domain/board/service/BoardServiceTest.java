package com.kangyoon.community.domain.board.service;


import com.kangyoon.community.domain.board.entity.Board;
import com.kangyoon.community.domain.board.entity.BoardCategory;
import com.kangyoon.community.domain.board.repository.BoardCategoryRepository;
import com.kangyoon.community.domain.board.repository.BoardManagerRepository;
import com.kangyoon.community.domain.board.repository.BoardRepository;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class BoardServiceTest {

    @Mock private BoardRepository boardRepository;
    @Mock private BoardCategoryRepository boardCategoryRepository;
    @Mock private BoardManagerRepository boardManagerRepository;

    @InjectMocks BoardService boardService;

    @Test
    void 없거나_삭제된_게시판_조회시_실패() {
        //given
        given(boardRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> boardService.findBoard(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOARD_NOT_FOUND);
    }

    @Test
    void 게시판_생성_성공() {
        //given
        BoardCategory mockCategory = new BoardCategory();
        ReflectionTestUtils.setField(mockCategory, "id", 1L);
        ReflectionTestUtils.setField(mockCategory, "name", "테스트카테고리");

        given(boardCategoryRepository.findById(any())).willReturn(Optional.of(mockCategory));
        given(boardRepository.existsByName(any())).willReturn(false);

        //when
        boardService.createBoard("테스트", "테스트 게시판", 1L);

        //then
        then(boardRepository).should().save(any());
    }

    @Test
    void 게시판명이_중복되면_게시판_생성_실패() {
        //given
        BoardCategory mockCategory = new BoardCategory();
        ReflectionTestUtils.setField(mockCategory, "id", 1L);
        ReflectionTestUtils.setField(mockCategory, "name", "테스트카테고리");

        given(boardCategoryRepository.findById(any())).willReturn(Optional.of(mockCategory));
        given(boardRepository.existsByName(any())).willReturn(true);

        //when & then
        assertThatThrownBy(() -> boardService.createBoard("테스트", "테스트 게시판", 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_BOARD_NAME);
    }

    @Test
    void 카테고리가_없으면_게시판_생성_실패() {
        //given
        BoardCategory mockCategory = new BoardCategory();
        ReflectionTestUtils.setField(mockCategory, "id", 1L);
        ReflectionTestUtils.setField(mockCategory, "name", "테스트카테고리");

        given(boardCategoryRepository.findById(any())).willReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> boardService.createBoard("테스트", "테스트 게시판", 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOARD_CATEGORY_NOT_FOUND);
    }

    @Test
    void 게시판_정보수정_성공() {
        //given
        BoardCategory mockCategory_A = new BoardCategory();
        ReflectionTestUtils.setField(mockCategory_A, "id", 1L);
        ReflectionTestUtils.setField(mockCategory_A, "name", "테스트카테고리");

        BoardCategory mockCategory_B = new BoardCategory();
        ReflectionTestUtils.setField(mockCategory_B, "id", 2L);
        ReflectionTestUtils.setField(mockCategory_B, "name", "수정한테스트카테고리");

        Board mockBoard = Board.create("테스트게시판", "테스트게시판입니다.", mockCategory_A);

        given(boardRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.of(mockBoard));
        given(boardCategoryRepository.findById(any())).willReturn(Optional.of(mockCategory_B));

        //when
        boardService.updateBoard(1L, "수정한테스트게시판", "수정한테스트게시판입니다.", 2L);

        //then
        assertThat(mockBoard.getName()).isEqualTo("수정한테스트게시판");
        assertThat(mockBoard.getDescription()).isEqualTo("수정한테스트게시판입니다.");
        assertThat(mockBoard.getBoardCategory()).isEqualTo(mockCategory_B);
    }

    @Test
    void 없거나_삭제된_게시판을_수정하면_실패() {
        //given
        given(boardRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> boardService.updateBoard(1L, "수정한테스트게시판", "수정한테스트게시판입니다.", 2L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOARD_NOT_FOUND);
    }

    @Test
    void 게시판_삭제_성공() {
        //given
        BoardCategory mockCategory = new BoardCategory();
        ReflectionTestUtils.setField(mockCategory, "id", 1L);
        ReflectionTestUtils.setField(mockCategory, "name", "테스트카테고리");

        Board mockBoard = Board.create("테스트게시판", "테스트게시판입니다.", mockCategory);

        given(boardRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.of(mockBoard));

        //when
        boardService.deleteBoard(1L);

        //then
        assertThat(mockBoard.getDeletedAt()).isNotNull();
    }

    @Test
    void 삭제되거나_없는_게시판_삭제시_실패() {
        //given
        given(boardRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> boardService.deleteBoard(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOARD_NOT_FOUND);
    }
}
