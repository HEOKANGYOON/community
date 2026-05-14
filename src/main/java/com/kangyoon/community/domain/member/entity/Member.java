package com.kangyoon.community.domain.member.entity;

import com.kangyoon.community.domain.Role;
import com.kangyoon.community.global.config.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  //기본 생성자 대신 생성
public class Member extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String password;
    private String provider;
    private String providerId;
    @Column(nullable = false)
    private String nickname;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}
