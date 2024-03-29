package com.todotogether.auth;

//시큐리티 /login 주요청이 오면 낚채서 로그인을 진행시킨다.
//로그인을 진행 완료가 되면 시큐리티 session을 만들어줍니다. (Security ContextHolder)
//오브젝트 타입 => Authentication 타입 객체
//Authentication 안에 User정보가 있어야 됨
//User오브젝타입 => UserDetail 타입 객체
//Security Session => Authentication => UserDetails

import com.todotogether.domain.entity.Member;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Getter
public class PrincipalDetails implements UserDetails, OAuth2User {

    private Member member;
    private Map<String,Object> attributes;
    //일발 로그인
    public PrincipalDetails(Member member)
    {
        this.member = member;
    }

        //OAuth 로그인
    public PrincipalDetails(Member member,Map<String,Object> attributes)
    {
        this.member = member;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return null;
    }

    //해당 유저의 권한 리턴
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collect = new ArrayList<>();
        collect.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                return member.getRoles().get(1).toString();
            }
        });
        return collect;
    }

    @Override
    public String getPassword() {
        return member.getPassword();
    }

    @Override
    public String getUsername() {
        return member.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    //계정이 잠겨있는지 여부
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    //계정활성
    //휴먼 계정으로 하기로 했으면
    @Override
    public boolean isEnabled() {

        //계정을 삭제하면 false로 바꿔주면 됨
        return true;
    }


}
