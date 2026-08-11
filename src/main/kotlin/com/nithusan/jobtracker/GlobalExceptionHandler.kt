package com.nithusan.jobtracker

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

data class ErrorResponse(
	val timestamp: Instant = Instant.now(),
	val status: Int,
	val error: String,
	val message: String,
	val fieldErrors: Map<String, String>? = null
)

@RestControllerAdvice
class GlobalExceptionHandler {

	@ExceptionHandler(ApplicationNotFoundException::class)
	fun handleNotFound(ex: ApplicationNotFoundException): ResponseEntity<ErrorResponse> =
		ResponseEntity.status(HttpStatus.NOT_FOUND).body(
			ErrorResponse(
				status = HttpStatus.NOT_FOUND.value(),
				error = "Not Found",
				message = ex.message ?: "Ikke funnet"
			)
		)

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
		val fieldErrors = ex.bindingResult.fieldErrors.associate {
			it.field to (it.defaultMessage ?: "Ugyldig verdi")
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
			ErrorResponse(
				status = HttpStatus.BAD_REQUEST.value(),
				error = "Bad Request",
				message = "Validering feilet",
				fieldErrors = fieldErrors
			)
		)
	}
}
