package com.kangyoon.community.domain.member.service;

import com.kangyoon.community.domain.member.dto.LoginResult;
import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.domain.member.repository.MemberRepository;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import com.kangyoon.community.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;


    public void signup(String email, String password, String nickname) {
        if (memberRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (memberRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        String encodedPassword = passwordEncoder.encode(password);

        Member member = Member.createLocal(email, encodedPassword, nickname);
        memberRepository.save(member);

        try {
            memberRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.DUPLICATE_MEMBER);
        }
    }

    @Transactional(readOnly = true)
    public LoginResult login(String email, String password) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_LOGIN));


        //입력받은 패스워드(평문) DB값(해시값) 검증 matches()
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_LOGIN);
        }

        String accessToken = jwtProvider.generateToken(member.getEmail(), member.getId(), member.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(member.getEmail());

        redisTemplate.opsForValue().set(
                "refresh:" + member.getId(),
                refreshToken,
                jwtProvider.getRefreshExpiration(),
                TimeUnit.MILLISECONDS
        );

        return new LoginResult(accessToken, refreshToken);
    }

    public LoginResult refresh(String refreshToken) {
        String email = jwtProvider.parseToken(refreshToken).getSubject();

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        String stored = redisTemplate.opsForValue().get("refresh:" + member.getId());
        if (stored == null) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        if (!stored.equals(refreshToken)) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }
        String newAccessToken = jwtProvider.generateToken(member.getEmail(), member.getId(), member.getRole());
        String newRefreshToken = jwtProvider.generateRefreshToken(member.getEmail());

        redisTemplate.opsForValue().set(
                "refresh:" + member.getId(),
                newRefreshToken,
                jwtProvider.getRefreshExpiration(),
                TimeUnit.MILLISECONDS
        );

        return new LoginResult(newAccessToken, newRefreshToken);
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        String email;

        try {
            email = jwtProvider.parseToken(refreshToken).getSubject();
        } catch (CustomException e) {
            return; //유효하지 않거나 만료된 토큰은 그냥 삼킴 리프레시 토큰 삭제 X
        }

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        redisTemplate.delete("refresh:" + member.getId());
    }
}
