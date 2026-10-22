package br.com.laboratorio.todo.web;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErroHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validarCorpo(MethodArgumentNotValidException ex) {
        ProblemDetail erro = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Verifique os campos informados.");
        erro.setProperty("campos", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage()).toList());
        return erro;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail validarParametros(ConstraintViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
