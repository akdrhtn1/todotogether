package com.todotogether.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todotogether.auth.PrincipalDetails;
import com.todotogether.domain.dto.MemberDto;
import com.todotogether.domain.dto.UploadFileDto;
import com.todotogether.domain.entity.Member;
import com.todotogether.domain.entity.Role;
import com.todotogether.service.EmailService;
import com.todotogether.service.ImageManagerService;
import com.todotogether.service.MemberService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserRestController {

    private MemberService memberService;
    private ModelMapper modelMapper;
    private EmailService emailService;
    private ImageManagerService imageManagerService;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public UserRestController(MemberService memberService, ModelMapper modelMapper, EmailService emailService, ImageManagerService imageManagerService ,PasswordEncoder passwordEncoder) {
        this.memberService = memberService;
        this.modelMapper = modelMapper;
        this.emailService = emailService;
        this.imageManagerService = imageManagerService;
        this.passwordEncoder = passwordEncoder;
    }
    /*
            회원 유효성 검사
     */
    @PostMapping("/register/valid")
    public ResponseEntity userValid(@RequestBody(required = false) @Valid MemberDto memberDto, Errors errors){

        Map<String, String> validatorResult = null;
        //회원 유효성 검사
        if(errors.hasErrors()) {
            //--------------추가수정
            validatorResult = memberService.validateHandling(errors);
        }
        //
        /*
        if(memberDto.getPassword() != null && memberDto.getPassword2() !=null){
            if(!memberDto.getPassword().equals(memberDto.getPassword2())){
                validatorResult.put("valid_pwCheck","비밀번호가 일치하지 않습니다.");
            }
        }
        */
        //아이디 중복 검증 유효성 검사
        try{
            Member member = modelMapper.map(memberDto, Member.class);
            String email = memberDto.getEmail();
            memberService.validateMember(member, email);
        }catch (IllegalStateException e){
            validatorResult.put("valid_idCheck",e.getMessage());
            //return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.OK).body(validatorResult);
    }

   //회원가입(s3 가능해야함)
    @PostMapping(value = "/register/")
    public ResponseEntity<Boolean> signUp(@RequestBody MemberDto memberDto){

        System.out.println(memberDto);
        Member member = modelMapper.map(memberDto, Member.class);
        long result = memberService.signUp(member, passwordEncoder);

        System.out.println(result + "asd"+ member);
        if(result >0){
            return ResponseEntity.status(HttpStatus.CREATED).body(true);
        }else{
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }

    // 파일전송 테스트 코드 실제론 dto받고 경로 전송
    @PostMapping(value ="/register/file")
    public ResponseEntity upload(@RequestParam(required = false) MultipartFile profile){
        String folderName = "image";
        UploadFileDto uploadFileDto = null;
        String foldDiv = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String filePath = folderName + File.separator + foldDiv;

        try {
             uploadFileDto = imageManagerService.createAndUploadFile(profile, filePath);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
            return ResponseEntity.status(HttpStatus.OK).body(uploadFileDto);
    }

    //email인증 클릭시 인증번호 발송
    @PostMapping("/register/emailConfirm")
    public ResponseEntity<String> emailConfirm(@RequestBody String email)throws Exception{

        String confirm = emailService.sendSimpleMessage(email);

        return ResponseEntity.status(HttpStatus.OK).body(confirm);
    }

    //로그인 후 계정 세션 정보를 확인
    @GetMapping("/register/login")
    public String testLogin(Authentication authentication, @AuthenticationPrincipal PrincipalDetails PrincipalDetails){
        //1번째 방법 getUser정보
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        log.info("authentication : " + principalDetails.getMember());

        //2번째 방법 생성한 principalDetails 상속받은 userDetails을 활용하는 방법
        log.info("userDetails : " + PrincipalDetails.getMember());
        return "세션 정보 확인";
    }

    //구글 로그인 후 세션 정보 확인하기
    @GetMapping("/register/login")
    public String testOAuthLogin(Authentication authentication, @AuthenticationPrincipal OAuth2User oauth){
        //1번째 방법 getUser정보
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        log.info("authentication : " + oAuth2User.getAttributes());

        log.info("oauth2User : " + oauth.getAttributes());


        //2번째 방법 생성한 principalDetails 상속받은 userDetails을 활용하는 방법
        return "OAuth 세션 정보 확인";
    }

    //OAuth 로그인을 해도 PrincipalDetails
    //일반 로그인을 해도 PrincipaDetails
    @GetMapping("/register/user")
    public Member user(@AuthenticationPrincipal PrincipalDetails principalDetails){
        return principalDetails.getMember();
    }

    @GetMapping("/token/refresh")
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION);
        if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                String refresh_token = authorizationHeader.substring("Bearer ".length());
                Algorithm algorithm = Algorithm.HMAC256("secret".getBytes());
                JWTVerifier verifier = JWT.require(algorithm).build();
                DecodedJWT decodedJWT = verifier.verify(refresh_token);
                String email = decodedJWT.getSubject();
                log.info("email{}", email);
                Member member = memberService.getMember(email);
                String access_token = JWT.create()
                        .withSubject(member.getEmail())
                        .withExpiresAt(new Date(System.currentTimeMillis() + 5 * 60 * 1000))
                        .withIssuer(request.getRequestURL().toString())
                        .withClaim("roles", member.getRoles().stream().map(Role::getRName).collect(Collectors.toList()))
                        .sign(algorithm);
                Map<String, String> tokens = new HashMap<>();
                tokens.put("access_token", access_token);
                tokens.put("refresh_token", refresh_token);
                response.setContentType(APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), tokens);
            }catch (Exception exception) {
                response.setHeader("error", exception.getMessage());
                response.setStatus(FORBIDDEN.value());
                Map<String,String> error = new HashMap<>();
                error.put("error_message", exception.getMessage());
                response.setContentType(APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), error);
            }
        }else {
            throw new RuntimeException("refresh token 사용불가.");
        }
    }
}
