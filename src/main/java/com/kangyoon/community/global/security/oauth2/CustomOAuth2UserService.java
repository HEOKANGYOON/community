package com.kangyoon.community.global.security.oauth2;

import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.domain.member.repository.MemberRepository;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;


import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();

        // .toString이 아니라 (String) 으로 캐스팅 한것을 null일수도 있기 떄문
        // null인 객체에 또 뭔짓을 하려고 하면 NPE 터짐
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        String providerIdKey = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();    //OAuth 제공사에서 사용자를 식별하는 필드가 무엇인지 가져오는 것

        String provider = userRequest.getClientRegistration().getRegistrationId();
        Object providerIdObj = attributes.get(providerIdKey);

        if (email == null || email.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_OAUTH_USER);
        }

        if (providerIdObj == null || providerIdObj.toString().isBlank()) {
            throw new CustomException(ErrorCode.INVALID_OAUTH_USER);
        }

        String providerId = providerIdObj.toString();

        String initialNickname = generateNickname(name);


        //동일 email이 로컬에 저장되어있을 경우 현재 구조로서는 동일한 사용자로 취급됨을 인지함
        //현재 email로 조회함, 만약 OAuth2 사용자 만을 조회가 필요하면 provider, providerId로 조회해야 함
        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> {
                    Member oAuthMember = Member.createOAuth(email, initialNickname, provider, providerId);
                    return memberRepository.save(oAuthMember);
                });

        return new CustomOAuth2User(member.getId(), member.getEmail(), member.getRole(), attributes);
    }

    //랜덤값이 겹칠 수도 있음 검증이 필요함을 인지했음
    private String generateNickname(String name) {
        String base = (name == null || name.isBlank()) ? "user" : name;
        String random = UUID.randomUUID().toString().substring(0,6);
        return base + "_" + random;
    }
}
