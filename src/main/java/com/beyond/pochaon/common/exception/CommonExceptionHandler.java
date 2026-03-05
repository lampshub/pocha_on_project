package com.beyond.pochaon.common.exception;

import com.beyond.pochaon.common.dto.CommonErrorDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class CommonExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> illegal(IllegalArgumentException e) {
        log.error("에러 메시지", e);
        CommonErrorDto ce_dto = CommonErrorDto.builder()
                .statusCode(400)
                .errorMessage(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ce_dto);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> notValid(MethodArgumentNotValidException e) {
        log.error("에러 메시지", e);
        CommonErrorDto ce_dto = CommonErrorDto.builder()
                .statusCode(400)
                .errorMessage(e.getFieldError().getDefaultMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ce_dto);
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Void> handleAsyncTimeout(AsyncRequestTimeoutException e) {
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> noSuch(NoSuchElementException e) {
        log.error("에러 메시지", e);
        CommonErrorDto ce_dto = CommonErrorDto.builder()
                .statusCode(404)
                .errorMessage(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ce_dto);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> notfoundEntity(EntityNotFoundException e) {
        log.error("에러 메시지", e);
        CommonErrorDto ce_dto = CommonErrorDto.builder().statusCode(404).errorMessage(e.getMessage()).build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ce_dto);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> accessDenied(AccessDeniedException e) {
        log.error("에러 메시지", e);
        CommonErrorDto ce_dto = CommonErrorDto.builder()
                .statusCode(403)
                .errorMessage(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ce_dto);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<?> autho(AuthorizationDeniedException e) {
        log.error("에러 메시지", e);
        CommonErrorDto ce_dto = CommonErrorDto.builder()
                .statusCode(403)
                .errorMessage(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ce_dto);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> security(SecurityException e) {
        log.error("에러 메시지", e);
        CommonErrorDto ce_dto = CommonErrorDto.builder()
                .statusCode(401)
                .errorMessage(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ce_dto);
    }

    @ExceptionHandler(RestClientException .class)
    public ResponseEntity<?> client(@NonNull RestClientException e) {
        log.error("에러 메시지", e);
        CommonErrorDto ce_dto = CommonErrorDto.builder()
                .statusCode(404)
                .errorMessage(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ce_dto);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> state(@NonNull IllegalStateException e) {
        log.error("에러 메시지", e);
        CommonErrorDto ce_dto = CommonErrorDto.builder()
                .statusCode(404)
                .errorMessage(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ce_dto);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> duplicate(DataIntegrityViolationException e) {
        String message = "중복된 데이터가 존재합니다.";
        if (e.getMessage().contains("businessRegistrationNumber")) {
            message = "이미 등록된 사업자등록번호입니다.";
        } else if (e.getMessage().contains("ownerEmail")) {
            message = "이미 사용 중인 이메일입니다.";
        }
        CommonErrorDto ce_dto = CommonErrorDto.builder()
                .statusCode(400)
                .errorMessage(message)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ce_dto);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> exception(Exception e) {
        log.error("에러 메시지", e);
        CommonErrorDto ce_dto = CommonErrorDto.builder()
                .statusCode(500)
                .errorMessage("서버 내부 오류가 발생했습니다")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ce_dto);
    }

}
