package com.nithusan.jobtracker

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ApplicationService(private val repository: ApplicationRepository, private val demoPolicy: DemoPolicy) {

	fun findAll(): List<Application> = repository.findAll()

	fun findById(id: Long): Application =
		repository.findById(id).orElseThrow { ApplicationNotFoundException(id) }

	@Transactional
	fun create(application: Application): Application {
		demoPolicy.lockWrites()
		demoPolicy.checkCapacity()
		return repository.save(application.copy(id = null))
	}

	@Transactional
	fun update(id: Long, updated: Application): Application {
		demoPolicy.lockWrites()
		val existing = findById(id)
		val toSave = existing.copy(
			companyName = updated.companyName,
			jobTitle = updated.jobTitle,
			applicationDate = updated.applicationDate,
			status = updated.status,
			jobListingUrl = updated.jobListingUrl,
			notes = updated.notes
		)
		return repository.save(toSave)
	}

	@Transactional
	fun delete(id: Long) {
		demoPolicy.lockWrites()
		if (!repository.existsById(id)) {
			throw ApplicationNotFoundException(id)
		}
		repository.deleteById(id)
	}
}
