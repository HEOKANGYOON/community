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
import com.kangyoon.community.infrastructure.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final PostVoteRepository postVoteRepository;
    private final RedisService redisService;
    private final S3Service s3Service;
    private final ApplicationEventPublisher eventPublisher;

    private static final Set<Character> LIKE_FALLBACK_TRIGGER_CHARS =
            Set.of('+', '-', '>', '<', '(', ')', '~', '*', '"', '@');

    private static final Pattern S3_URL_PATTERN =
            Pattern.compile("https://[\\w.-]+\\.s3[\\w.-]*\\.amazonaws\\.com/(temp|posts)/[\\w\\-./]+");

    public Page<PostSummaryResponse> getAllPost(Long boardId, String keyword, String searchType, Pageable pageable) {

        String assembled = null;

        if (keyword != null && !keyword.isBlank()) {
            String trimKeyword = keyword.trim();

            if (requiresLikeFallback(trimKeyword)) {    //LIKE_FALLBACK_TRIGGER_CHARS에 있는 특수문자를 포함한 경우 LIKE검색으로
                Page<Post> posts = postRepository.findPostsByBoardLike(boardId, trimKeyword, searchType, pageable);
                return toSummaryResponses(posts);
            }

            List<String> tokens = Arrays.stream(trimKeyword.split("\\s+"))  // 공백이 연속되는 경우까지 막아야함 \s+
                    .filter(token -> token.length() >= 2)
                    .toList();

            if (tokens.isEmpty()) { //입력은 했지만 유효한 토큰이 없으면
                return Page.empty(pageable);    //DB 호출 없이 바로 리턴함(조회 결과 없이 페이징 정보만)
            }

            assembled = tokens.stream()
                    .map(token -> "+" + token)
                    .collect(Collectors.joining(" "));
        }

        //특수문자가 없을 떄는 FULLTEXT
        Page<Post> posts = postRepository.findPostsByBoard(boardId, assembled, searchType, pageable);
        return toSummaryResponses(posts);
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


    public PostResponse writePost(Long memberId, Long boardId, String title, String content) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Board board = boardRepository.findByIdAndDeletedAtIsNull(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));


        Set<String> tempUrls = extractS3Urls(content).stream()
                .filter(url -> url.contains("/temp/"))
                .collect(Collectors.toSet());

        for (String tempUrl : tempUrls) {
            String postUrl = s3Service.moveToPostFolder(tempUrl);   //클라이언트에서 등록한 temp를 posts로 이동
            content = content.replace(tempUrl, postUrl);    //이동한 경로로 본문 url 수정
        }


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

        String newContent = content;

        //새 본문의 temp URL만 이동
        Set<String> tempUrls = extractS3Urls(newContent).stream()
                .filter(url -> url.contains("/temp/"))
                .collect(Collectors.toSet());

        for (String tempUrl : tempUrls) {
            String postUrl = s3Service.moveToPostFolder(tempUrl);   //temp -> posts
            newContent = newContent.replace(tempUrl, postUrl);  //본문의 경로도 수정
        }

        int viewCount = redisService.getViewCount(postId);
        int recommendCount = redisService.getRecommendCount(postId);
        int disrecommendCount = redisService.getDisrecommendCount(postId);

        post.editPost(title, newContent);
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

        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (postVoteRepository.existsByMemberIdAndPostId(memberId, postId)) {
            throw new CustomException(ErrorCode.DUPLICATE_VOTE);
        }

        PostVote postVote = PostVote.builder()
                .post(post)
                .member(member)
                .voteType(voteType)
                .build();

        try {
            postVoteRepository.save(postVote);
            postVoteRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.DUPLICATE_VOTE);
        }

        if (voteType == VoteType.UP) {
            redisService.increaseRecommend(postId);
        } else {
            redisService.increaseDisrecommend(postId);
        }
    }

    private boolean requiresLikeFallback(String keyword) {
        return keyword.chars().anyMatch(c -> LIKE_FALLBACK_TRIGGER_CHARS.contains((char) c));
    }

    private Page<PostSummaryResponse> toSummaryResponses(Page<Post> posts) {
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

    private Set<String> extractS3Urls(String html) {
        if (html == null) return Collections.emptySet();
        Matcher matcher = S3_URL_PATTERN.matcher(html);
        Set<String> urls = new HashSet<>();
        while (matcher.find()) {
            urls.add(matcher.group());
        }
        return urls;
    }

}