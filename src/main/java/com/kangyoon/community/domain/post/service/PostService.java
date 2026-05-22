package com.kangyoon.community.domain.post.service;

import com.kangyoon.community.domain.Role;
import com.kangyoon.community.domain.board.entity.Board;
import com.kangyoon.community.domain.board.repository.BoardRepository;
import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.domain.member.repository.MemberRepository;
import com.kangyoon.community.domain.post.dto.PostResponse;
import com.kangyoon.community.domain.post.dto.PostSummaryResponse;
import com.kangyoon.community.domain.post.entity.Post;
import com.kangyoon.community.domain.post.repository.PostRepository;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;

    public List<PostSummaryResponse> getAllPost(Long boardId, Pageable pageable) {
        return postRepository.findByBoardIdAndDeletedAtIsNull(boardId, pageable)
                .stream()
                .map(PostSummaryResponse::from)
                .toList();
    }

    public PostResponse getPost(Long postId) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        return PostResponse.from(post);
    }

    //검색 기능 구현할 때 합시다(15번 mySQL FULLTEXT 인덱스 설정)
//    public void findByTitle(String title) {
//        //검색 결과를 어떻게 가져올것인가
//        return postRepository.findByDeletedAtIsNull()
//                .stream()
//                .map(//DTO만들어서 값 답아라)
//                .toList();
//    }

    //이미지 URL도 추가해야함(18번 이미지 업로드 구현하면서 ㄱㄱ)
    public PostResponse writePost(Long memberId, Long boardId, String title, String content) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Board board = boardRepository.findByIdAndDeletedAtIsNull(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

        Post post = Post.createPost(member, board, title, content);

        Post saved = postRepository.save(post);

        return PostResponse.from(saved);
    }

    public PostResponse editPost(Long postId, Long memberId, String title, String content) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.POST_AUTHOR_MISMATCH);
        }

        post.editPost(title, content);
        postRepository.flush();     //updatedAt을 정확하게 받아오기 위함
        return PostResponse.from(post);
    }

    public void deletePost(Long postId, Long memberId, String role) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!role.equals(Role.ADMIN.toString())  && !post.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.POST_AUTHOR_MISMATCH);
        }
        //소프트 딜리트
        post.deletePost();
    }
}
