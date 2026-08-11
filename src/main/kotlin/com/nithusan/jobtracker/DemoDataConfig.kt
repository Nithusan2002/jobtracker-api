package com.nithusan.jobtracker

import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDate

@Configuration
class DemoDataConfig {

	@Bean
	fun seedDemoApplications(repository: ApplicationRepository): ApplicationRunner =
		ApplicationRunner {
			if (repository.count() > 0) {
				return@ApplicationRunner
			}

			repository.saveAll(
				listOf(
					Application(
						companyName = "Computas",
						jobTitle = "Backend-utvikler",
						applicationDate = LocalDate.of(2026, 8, 1),
						status = ApplicationStatus.INTERVIEW,
						jobListingUrl = "https://example.com/computas-backend",
						notes = "Intervju avtalt. Forbered systemdesign og Kotlin/Spring Boot."
					),
					Application(
						companyName = "Sopra Steria",
						jobTitle = "Summer Intern - Technology",
						applicationDate = LocalDate.of(2026, 8, 3),
						status = ApplicationStatus.SENT,
						jobListingUrl = "https://example.com/sopra-intern",
						notes = "Sendt søknad og CV. Følg opp om to uker."
					),
					Application(
						companyName = "Bekk",
						jobTitle = "Utvikler intern",
						applicationDate = LocalDate.of(2026, 8, 5),
						status = ApplicationStatus.SENT,
						jobListingUrl = "https://example.com/bekk-intern",
						notes = "Relevant for backend, Kotlin og produktutvikling."
					),
					Application(
						companyName = "Bouvet",
						jobTitle = "Juniorutvikler",
						applicationDate = LocalDate.of(2026, 8, 7),
						status = ApplicationStatus.REJECTED,
						jobListingUrl = "https://example.com/bouvet-junior",
						notes = "Avslag mottatt. Bruk erfaringen til å spisse neste søknad."
					),
					Application(
						companyName = "Knowit",
						jobTitle = "Backend trainee",
						applicationDate = LocalDate.of(2026, 8, 9),
						status = ApplicationStatus.OFFER,
						jobListingUrl = "https://example.com/knowit-trainee",
						notes = "Tilbud mottatt. Sammenlign rolle, læringsmuligheter og oppstartsdato."
					)
				)
			)
		}
}
