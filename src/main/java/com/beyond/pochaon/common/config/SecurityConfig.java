package com.beyond.pochaon.common.config;

import com.beyond.pochaon.common.auth.JwtTokenFilter;
import com.beyond.pochaon.common.auth.JwtTokenProvider;
import com.beyond.pochaon.common.exception.JwtAccessDeniedHandler;
import com.beyond.pochaon.common.exception.JwtAuthenticationHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationHandler jwtAuthenticationHandler;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Autowired
    public SecurityConfig(
            JwtTokenProvider jwtTokenProvider,
            JwtAuthenticationHandler jwtAuthenticationHandler,
            JwtAccessDeniedHandler jwtAccessDeniedHandler
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtAuthenticationHandler = jwtAuthenticationHandler;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }

    @Bean
    public JwtTokenFilter jwtTokenFilter() {
        return new JwtTokenFilter(jwtTokenProvider);
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return http
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .addFilterBefore(jwtTokenFilter(), UsernamePasswordAuthenticationFilter.class)

                .exceptionHandling(e ->
                        e.authenticationEntryPoint(jwtAuthenticationHandler)
                                .accessDeniedHandler(jwtAccessDeniedHandler)
                )

                .authorizeHttpRequests(a ->
                        // WebSocket 관련 모든 경로 허용
                        a.requestMatchers("/ws-stomp/**").permitAll().requestMatchers(
                                        "/owner/baseLogin",
                                        "/test/**",
                                        "/ws-stomp/**",
                                        "/topic/**",
                                        "/connect/**",
                                        "/app/**",
                                        "/owner/business/verify",
                                        "/owner/login",
                                        "/owner/create",
                                        "/owner/refresh",
                                        "/customertable/tablestatuslist",
                                        "/pay/kakao/success",
                                        "/pay/kakao/fail",
                                        "/pay/kakao/cancel",

                                        "/auth/email/send",
                                        "/auth/email/verify",
                                        "/auth/password/reset",
                                        "/auth/sms/send",
                                        "/auth/sms/verify",

                                        "/customertable/**",
                                        "/store/detail/**",
                                        "/ws-stomp/**"
                                ).permitAll()
                                .anyRequest().authenticated()
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                Arrays.asList("http://localhost:3002")
        );
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder pwEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}