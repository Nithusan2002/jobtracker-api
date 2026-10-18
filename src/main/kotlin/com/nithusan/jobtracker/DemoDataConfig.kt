package com.nithusan.jobtracker

import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = ["jobtracker.demo.enabled"], havingValue = "true")
class DemoDataConfig(private val demoData: DemoDataService) {
    @Bean
    fun seedDemoApplications() = ApplicationRunner { demoData.reset() }

    @Scheduled(
        initialDelayString = "\${jobtracker.demo.reset-interval:PT30M}",
        fixedDelayString = "\${jobtracker.demo.reset-interval:PT30M}"
    )
    fun resetDemoApplications() = demoData.reset()
}

@Service
@ConditionalOnProperty(name = ["jobtracker.demo.enabled"], havingValue = "true")
class DemoDataService(private val repository: ApplicationRepository, private val policy: DemoPolicy) {
    @Transactional
    fun reset() {
        policy.lockWrites()
        repository.deleteAllInBatch()
        repository.saveAll(samples())
    }

    private fun samples(): List<Application> = listOf(
        sample("Fjord Demo AS", "Backendutvikler", ApplicationStatus.INTERVIEW),
        sample("Nordlys Eksempel AS", "Sommerstudent", ApplicationStatus.SENT),
        sample("Skog Demo AS", "Juniorutvikler", ApplicationStatus.SENT),
        sample("Hav Eksempel AS", "Utvikler", ApplicationStatus.REJECTED),
        sample("Fjell Demo AS", "Backend trainee", ApplicationStatus.OFFER)
    )

    private fun sample(company: String, title: String, status: ApplicationStatus) = Application(
        companyName = company,
        jobTitle = title,
        applicationDate = LocalDate.of(2026, 8, 1),
        status = status,
        jobListingUrl = "https://example.com/",
        notes = "Fiktiv demosøknad. Dette er ikke en virkelig jobbsøknad."
    )
}
