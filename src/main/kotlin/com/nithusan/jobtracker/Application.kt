package com.nithusan.jobtracker

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

enum class ApplicationStatus {
	SENT,
	INTERVIEW,
	REJECTED,
	OFFER
}

@Entity
@Table(name = "applications")
data class Application(
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long? = null,

	@field:NotBlank(message = "companyName er påkrevd")
	@field:Size(max = 255, message = "Bedriftsnavn kan ha maks 255 tegn")
	@Column(nullable = false)
	val companyName: String = "",

	@field:NotBlank(message = "jobTitle er påkrevd")
	@field:Size(max = 255, message = "Stilling kan ha maks 255 tegn")
	@Column(nullable = false)
	val jobTitle: String = "",

	@field:NotNull(message = "applicationDate er påkrevd")
	@Column(nullable = false)
	val applicationDate: LocalDate? = null,

	@field:NotNull(message = "status er påkrevd")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	val status: ApplicationStatus = ApplicationStatus.SENT,

	@Column(name = "job_listing_url")
	@field:Size(max = 255, message = "Stillingslenken kan ha maks 255 tegn")
	@field:HttpUrl
	val jobListingUrl: String? = null,

	@Column(columnDefinition = "TEXT")
	@field:Size(max = 2000, message = "Notater kan ha maks 2000 tegn")
	val notes: String? = null
)
