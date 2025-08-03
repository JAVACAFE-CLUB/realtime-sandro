package com.sandro.realtime.shared.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(RealtimeAppException::class)
    fun handleBusinessException(ex: RealtimeAppException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(message = ex.message)
        return ResponseEntity.status(ex.httpStatus).body(errorResponse)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors =
            ex.bindingResult.allErrors.map { error ->
                val fieldName = (error as FieldError).field
                val errorMessage = error.defaultMessage ?: "Invalid value"
                "$fieldName: $errorMessage"
            }

        val errorResponse = ErrorResponse(message = errors.toString())
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(message = "Internal server error")
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}

data class ErrorResponse(val message: String)