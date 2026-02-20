package com.beyond.pochaon.common.exception;

import com.beyond.pochaon.common.dtos.CommonErrorDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class CommonExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> illegal(IllegalArgumentException e) {
        e.printStackTrace();
        CommonErrorDto ce_dto = CommonErrorDto.builder()
                .statusCode(400)
                .errorMessage(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ce_dto);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> notValid(MethodArgumentNotValidException e) {
        e.printStackTrace();
        CommonErrorDto ce_dto = CommonErrorDto.builder()
                .statusCode(400)
                .errorMessage(e.getFieldError().getDefaultMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ce_dto);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> noSuch(NoSuchElementException e) {
        e.printStackTrace();
        CommonErrorDto ce_dto = CommonErrorDto.builder()
                .statusCode(404)
                .errorMessage(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ce_dto);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> notfoundEntity(EntityNotFoundException e) {
        e.printStackTrace();
        CommonErrorDto ce_dto = CommonErrorDto.builder().statusCode(404).errorMessage(e.getMessage()).build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ce_dto);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<?> autho(AuthorizationDeniedException e) {
        e.printStackTrace();
        CommonErrorDto ce_dto = CommonErrorDto.builder()
                .statusCode(403)
                .errorMessage(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ce_dto);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> security(SecurityException e) {
        e.printStackTrace();
        CommonErrorDto ce_dto = CommonErrorDto.builder()
                .statusCode(401)
                .errorMessage(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ce_dto);
    }

    @ExceptionHandler(RestClientException .class)
    public ResponseEntity<?> client(@NonNull RestClientException e) {
        e.printStackTrace();
        CommonErrorDto ce_dto = CommonErrorDto.builder()
                .statusCode(404)
                .errorMessage(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ce_dto);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> state(@NonNull IllegalStateException e) {
        e.printStackTrace();
        CommonErrorDto ce_dto = CommonErrorDto.builder()
                .statusCode(404)
                .errorMessage(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ce_dto);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> exception(Exception e) {
        e.printStackTrace();
        CommonErrorDto ce_dto = CommonErrorDto.builder()
                .statusCode(500)
                .errorMessage(e.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ce_dto);
    }

}
