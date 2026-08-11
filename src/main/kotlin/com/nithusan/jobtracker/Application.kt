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
	@Column(nullable = false)
	val companyName: String = "",

	@field:NotBlank(message = "jobTitle er påkrevd")
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
	val jobListingUrl: String? = null,

	@Column(columnDefinition = "TEXT")
	val notes: String? = null
)
