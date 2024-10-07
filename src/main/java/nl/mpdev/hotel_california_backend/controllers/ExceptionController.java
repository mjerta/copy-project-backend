package nl.mpdev.hotel_california_backend.controllers;

import nl.mpdev.hotel_california_backend.exceptions.GeneralException;
import nl.mpdev.hotel_california_backend.exceptions.RecordNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ControllerAdvice
public class ExceptionController {

  @ExceptionHandler(value = MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleException(MethodArgumentNotValidException ex) {
    Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream().
      collect(Collectors.toMap(
        FieldError::getField,
        fieldError -> Optional.ofNullable(fieldError.getDefaultMessage()).orElse("Invalid value")
      ));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
  }

  @ExceptionHandler(value = DataIntegrityViolationException.class)
  public ResponseEntity<Object> handleException(DataIntegrityViolationException ex) {
    Map<String, String> error = new LinkedHashMap<>();
    error.put("error", "Constrain error in database");
    // extra details about error
    error.put("error-message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(value = IndexOutOfBoundsException.class)
  public ResponseEntity<Object> handleException(IndexOutOfBoundsException ex) {
    Map<String, String> error = new LinkedHashMap<>();
    error.put("error", "Index out of bounds");
    // extra details about error
    error.put("error-message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(value = HttpMessageNotReadableException.class)
  public ResponseEntity<Object> handleException(HttpMessageNotReadableException ex) {
    Map<String, String> error = new LinkedHashMap<>();
    error.put("error", "Invalid properties inside your request");
    // extra details about error
    error.put("error-message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

//  @ExceptionHandler(value = UsernameNotFoundException.class)
//  public ResponseEntity<Object> handleException(UsernameNotFoundException ex) {
//    Map<String, String> error = new LinkedHashMap<>();
//    error.put("error", "Username not found");
//    // extra details about error
//    error.put("error-message", ex.getMessage());
//    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
//  }

  //custom exceptions

  @ExceptionHandler(RecordNotFoundException.class)
  public ResponseEntity<Object> handleException(RecordNotFoundException ex) {
    Map<String, String> error = new LinkedHashMap<>();
    error.put("error-message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(GeneralException.class)
  public ResponseEntity<Object> handleException(GeneralException ex) {
    Map<String, String> error = new LinkedHashMap<>();
    error.put("error-message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }
}
