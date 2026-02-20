package com.beyond.pochaon.common.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

//구분 :  	    예전 필터	   지금 필터
//토큰 없음:    	그냥 통과	   인증 실패
//인증 책임:    	있으면 한다	  없으면 실패
//permitAll :    의존O	        의존 X
//보안 강도	:     느슨	        엄격
//실무 트렌드:	  과거	        현재

public class JwtTokenFilter extends OncePerRequestFilter {

    // 인증 예외 URL
    private static final List<String> WHITE_LIST = List.of(
            "/owner/baseLogin",
            "/owner/refresh",
            "/owner/create",
            "/auth/email/present",
            "/auth/email/send",
            "/auth/email/verify",
            "/auth/password/reset",
            "/auth/sms/send",
            "/auth/sms/verify",
            "/customertable/tablestatuslist",
            "/customertable/tablestatuslist",
            "/owner/business/verify"
    );

    private final JwtTokenProvider jwtTokenProvider;

    public JwtTokenFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // 화이트리스트 URL은 필터 제외
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return WHITE_LIST.stream().anyMatch(uri::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String bearerToken = request.getHeader("Authorization");

        // 토큰 없음 → 인증 실패
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "토큰이 없습니다.");
            return;
        }

        String accessToken = bearerToken.substring(7);

        try {
            Claims claims = jwtTokenProvider.validateAccessToken(accessToken);

            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            String stage = claims.get("stage", String.class);

            if (stage == null)
                throw new RuntimeException("stage 없음");

            // request attribute 세팅
            request.setAttribute("email", email);
            request.setAttribute("stage", stage);

            if (claims.get("storeId") != null)
                request.setAttribute("storeId", claims.get("storeId", Long.class));

            if (claims.get("tableNum") != null)
                request.setAttribute("tableNum", claims.get("tableNum", Integer.class)); //Long

            if (claims.get("tableId") != null)
                request.setAttribute("tableId", claims.get("tableId", Long.class));


            /* =========================
               권한 검사
               ========================= */

            if (!UrlAuthorizationMatcher.isAllowed(uri, TokenStage.valueOf(stage))) {

                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");

                response.getWriter().write("""
                        {
                          "statusCode":403,
                          "errorMessage":"접근 권한이 없습니다."
                        }
                        """);

                return;
            }


            /* =========================
               Security 인증 객체 생성
               ========================= */

            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {

            SecurityContextHolder.clearContext();

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "토큰 검증 실패: " + e.getMessage()
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}