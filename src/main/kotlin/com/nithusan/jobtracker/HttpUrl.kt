package com.nithusan.jobtracker

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import java.net.URI
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [HttpUrlValidator::class])
annotation class HttpUrl(
    val message: String = "Bruk en gyldig http- eller https-lenke uten brukernavn og passord",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class HttpUrlValidator : ConstraintValidator<HttpUrl, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value.isNullOrEmpty()) return true
        return runCatching {
            val uri = URI(value)
            uri.scheme?.lowercase() in setOf("http", "https") &&
                !uri.host.isNullOrBlank() && uri.rawUserInfo == null &&
                (uri.port == -1 || uri.port in 1..65535)
        }.getOrDefault(false)
    }
}
