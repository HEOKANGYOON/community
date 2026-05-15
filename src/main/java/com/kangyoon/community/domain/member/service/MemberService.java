package com.kangyoon.community.domain.member.service;

import com.kangyoon.community.domain.member.dto.LoginResponse;
import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.domain.member.repository.MemberRepository;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import com.kangyoon.community.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;


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
    public LoginResponse login(String email, String password) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_LOGIN));


        //입력받은 패스워드(평문) DB값(해시값) 검증 matches()
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_LOGIN);
        }

        return new LoginResponse(
                jwtProvider.generateToken(member.getEmail(), member.getId(), member.getRole()),
                jwtProvider.generateRefreshToken(member.getEmail())
        );
    }



}
