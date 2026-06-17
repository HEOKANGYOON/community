package com.kangyoon.community.domain.member.entity;

import com.kangyoon.community.domain.Role;
import com.kangyoon.community.global.config.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  //기본 생성자 대신 생성
public class Member extends BaseEntity {

    @Builder(access = AccessLevel.PRIVATE)
    private Member(String email, String password, String nickname, String provider, String providerId, Role role) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role;
    }

    public static Member createLocal(String email, String password, String nickname) {
        return Member.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .provider("LOCAL")
                .role(Role.USER)
                .build();
    }

    public static Member createOAuth(String email, String nickname, String provider, String providerId) {
        return Member.builder()
                .email(email)
                .nickname(nickname)
                .password(UUID.randomUUID().toString())     //쓰이지 않는 random UUID 값 nullable = false여서 넣어줌
                .provider(provider)
                .providerId(providerId)
                .role(Role.USER)
                .build();
    }



    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String email;
    @Column(nullable = false) private String password;
    private String provider;
    private String providerId;
    @Column(nullable = false) private String nickname;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role;
    private LocalDateTime deletedAt;

}
