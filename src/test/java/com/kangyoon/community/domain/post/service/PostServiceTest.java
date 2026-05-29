package com.kangyoon.community.domain.post.service;

import com.kangyoon.community.domain.board.entity.Board;
import com.kangyoon.community.domain.board.entity.BoardCategory;
import com.kangyoon.community.domain.board.repository.BoardRepository;
import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.domain.member.repository.MemberRepository;
import com.kangyoon.community.domain.post.dto.PostResponse;
import com.kangyoon.community.domain.post.entity.Post;
import com.kangyoon.community.domain.post.repository.PostRepository;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import static org.mockito.ArgumentMatchers.any;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private BoardRepository boardRepository;

    @InjectMocks private PostService postService;

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
    void 게시글_작성_성공() {
        //given
        given(memberRepository.findById(any())).willReturn(Optional.of(member));
        given(boardRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.of(board));

        Post savedPost = Post.createPost(member, board, "테스트 게시글", "테스트 게시글입니다.");
        ReflectionTestUtils.setField(savedPost, "id", 1L);

        given(postRepository.save(any())).willReturn(savedPost);

        //when
        postService.writePost(member.getId(), board.getId(), "테스트 게시글", "테스트 게시글입니다.");

        //then
        then(postRepository).should().save(any());
    }

    @Test
    void 없는회원으로_게시글_작성_실패() {
        //given
        given(memberRepository.findById(any())).willReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> postService.writePost(1L, 1L,"테스트 게시글", "테스트 게시글입니다."))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    void 없는게시판으로_게시글_작성_실패() {
        //given
        given(memberRepository.findById(any())).willReturn(Optional.of(member));
        given(boardRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> postService.writePost(member.getId(), 1L,"테스트 게시글", "테스트 게시글입니다."))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BOARD_NOT_FOUND);
    }

    @Test
    void 단건_조회_성공() {
        //given
        given(postRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.of(post));

        //when
        PostResponse response = postService.getPost(1L);

        //then
        assertThat(response.postId()).isEqualTo(1L);
        assertThat(response.boardId()).isEqualTo(board.getId());
        assertThat(response.title()).isEqualTo("테스트 게시글");
        assertThat(response.content()).isEqualTo("테스트 게시글입니다.");
    }

    @Test
    void 없는게시판_단건_조회_실패() {
        //given
        given(postRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> postService.getPost(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 수정_성공() {
        //given
        given(postRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.of(post));

        //when
        PostResponse response = postService.editPost(1L, 1L, "수정된 게시글", "수정된 게시글입니다.");

        //then
        assertThat(response.postId()).isEqualTo(1L);
        assertThat(response.boardId()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("수정된 게시글");
        assertThat(response.content()).isEqualTo("수정된 게시글입니다.");
    }

    @Test
    void 없는게시글_수정_실패() {
        //given
        given(postRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> postService.editPost(1L, 1L, "수정된 게시글", "수정된 게시글입니다."))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }
    @Test
    void 작성자_불일치_수정_실패() {
        given(postRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.of(post));

        //when & then
        assertThatThrownBy(() -> postService.editPost(1L, 30L, "수정된 게시글", "수정된 게시글입니다."))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_AUTHOR_MISMATCH);
    }

    @Test
    void 게시글_작성자가_게시글_삭제_성공() {
        //given
        given(postRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.of(post));

        //when
        postService.deletePost(post.getId(), member.getId(), member.getRole().name());

        //then
        assertThat(post.getDeletedAt()).isNotNull();
    }

    @Test
    void 없는게시글_삭제_실패() {
        //given
        given(postRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> postService.deletePost(1L, 1L, "USER"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);

    }

    @Test
    void 작성자_불일치_삭제_실패() {
        //given
        given(postRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.of(post));

        //when & then
        assertThatThrownBy(() -> postService.deletePost(post.getId(), 30L, "USER"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_AUTHOR_MISMATCH);
    }

    @Test
    void ADMIN이_다른_사람_게시글_삭제_성공() {
        //given
        given(postRepository.findByIdAndDeletedAtIsNull(any())).willReturn(Optional.of(post));

        //when
        postService.deletePost(post.getId(), 30L, "ADMIN");     //게시글 작성자가 아닌(30L) ADMIN이 게시글을 삭제함

        //then
        assertThat(post.getDeletedAt()).isNotNull();
    }


}
