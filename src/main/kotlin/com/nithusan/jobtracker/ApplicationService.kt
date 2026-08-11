package com.nithusan.jobtracker

import org.springframework.stereotype.Service

@Service
class ApplicationService(private val repository: ApplicationRepository) {

	fun findAll(): List<Application> = repository.findAll()

	fun findById(id: Long): Application =
		repository.findById(id).orElseThrow { ApplicationNotFoundException(id) }

	fun create(application: Application): Application =
		repository.save(application.copy(id = null))

	fun update(id: Long, updated: Application): Application {
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

	fun delete(id: Long) {
		if (!repository.existsById(id)) {
			throw ApplicationNotFoundException(id)
		}
		repository.deleteById(id)
	}
}
