package com.kangyoon.community.domain.post.service;

import com.kangyoon.community.domain.Role;
import com.kangyoon.community.domain.board.entity.Board;
import com.kangyoon.community.domain.board.repository.BoardRepository;
import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.domain.member.repository.MemberRepository;
import com.kangyoon.community.domain.post.dto.PostResponse;
import com.kangyoon.community.domain.post.dto.PostSummaryResponse;
import com.kangyoon.community.domain.post.entity.Post;
import com.kangyoon.community.domain.post.entity.PostVote;
import com.kangyoon.community.domain.post.entity.VoteType;
import com.kangyoon.community.domain.post.repository.PostRepository;
import com.kangyoon.community.domain.post.repository.PostVoteRepository;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import com.kangyoon.community.infrastructure.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final PostVoteRepository postVoteRepository;
    private final RedisService redisService;

    public Page<PostSummaryResponse> getAllPost(Long boardId, String keyword, String searchType, Pageable pageable) {

        Page<Post> posts = postRepository.findPostsByBoard(boardId, keyword, searchType, pageable);

        List<Long> postIds = posts.getContent().stream()
                .map(Post::getId)
                .toList();

        Map<Long, Integer> viewCountMap = redisService.getViewCountList(postIds);
        Map<Long, Integer> recommendMap = redisService.getRecommendList(postIds);

        return posts.map(post -> PostSummaryResponse.from(
                post,
                viewCountMap.getOrDefault(post.getId(), post.getViewCount()),
                recommendMap.getOrDefault(post.getId(), post.getRecommendationCount())
        ));
    }

    public PostResponse getPost(Long postId) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        //게시글 상세 조회 시 조회수가 증가한다, TTL은 따로 설정하지 않기로 함
        //TTL을 걸 경우 increaseViewCount 호출 전 ip주소, memberId등을 redis에 TTL걸어서 적재 하는 방법이 있음
        redisService.increaseViewCount(postId);

        int viewCount = redisService.getViewCount(postId);
        int recommendCount = redisService.getRecommendCount(postId);
        int disrecommendCount = redisService.getDisrecommendCount(postId);

        return PostResponse.from(post, viewCount, recommendCount, disrecommendCount);
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

        //게시글 작성 직후는 조회 0, 댓글 0 추천 / 비추천 0 을 내려줌
        return PostResponse.from(saved, 0, 0, 0);
    }

    public PostResponse editPost(Long postId, Long memberId, String title, String content) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.POST_AUTHOR_MISMATCH);
        }

        int viewCount = redisService.getViewCount(postId);
        int recommendCount = redisService.getRecommendCount(postId);
        int disrecommendCount = redisService.getDisrecommendCount(postId);

        post.editPost(title, content);
        postRepository.flush();     //updatedAt을 정확하게 받아오기 위함
        return PostResponse.from(post, viewCount, recommendCount, disrecommendCount);
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

    public void vote(Long memberId, Long postId, VoteType voteType) {

        //FK라고 해서 무조건 연관관계가 필요하지 않을 수 있음 로그성 데이터의 경우 객체 그래프 탐색 안함 -> 굳이 연관으로 둘 필요 없음
        //굳이 post를 find하지 않고 exists만 확인 해서 성능을 챙기고(속도는 find < exists)
        //postvote에 연관을 넣어줄 땐 프록시로 id만 넣어주는 방식도 가능
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        //컨트롤러의 AuthenticationPrincipal을 신뢰한다면 굳이 검증하지 않아도 됨
        //근데 soft delete를 멤버에게도 적용할것이기 때문에 일단 검증하는 로직을 남겨둠
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (postVoteRepository.existsByMemberIdAndPostId(memberId, postId)) {
            throw new CustomException(ErrorCode.DUPLICATE_VOTE);
        }

        //굳이 빌더 써야했나?
        PostVote postVote = PostVote.builder()
                .post(post)
                .member(member)
                .voteType(voteType)
                .build();

        try {
            postVoteRepository.save(postVote);
            //DB insert 성공 이후 redis.incr을 실행시키기 위한 명시적 flush 호출임
            // if (postVoteRepository.existsByMemberIdAndPostId(memberId, postId)) 이 조건문으로 중복검사는 미리 했음
            //해당 flush는 동시요청 경쟁상태에서만 동작함 최종 방어선은 DB의 unique 제약
            //@TransactionalEventListener(AFTER_COMMIT) 이 어노테이션으로 커밋이 성공적으로 완료되면 외부 시스템 호출하는 식으로 리팩터링할 에쩡
            postVoteRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.DUPLICATE_VOTE);
        }

        //redis는 인프라 쪽 redis를 사용하는 로직은 redisService에 공통적으로 처리
        if (voteType == VoteType.UP) {
            redisService.increaseRecommend(postId);
        } else {
            redisService.increaseDisrecommend(postId);
        }
    }

}
