package com.nithusan.jobtracker

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/applications")
class ApplicationController(private val service: ApplicationService) {

	@GetMapping
	fun getAll(): ResponseEntity<List<Application>> =
		ResponseEntity.ok(service.findAll())

	@GetMapping("/{id}")
	fun getOne(@PathVariable id: Long): ResponseEntity<Application> =
		ResponseEntity.ok(service.findById(id))

	@PostMapping
	fun create(@Valid @RequestBody application: Application): ResponseEntity<Application> {
		val created = service.create(application)
		return ResponseEntity.status(HttpStatus.CREATED).body(created)
	}

	@PutMapping("/{id}")
	fun update(@PathVariable id: Long, @Valid @RequestBody application: Application): ResponseEntity<Application> =
		ResponseEntity.ok(service.update(id, application))

	@DeleteMapping("/{id}")
	fun delete(@PathVariable id: Long): ResponseEntity<Void> {
		service.delete(id)
		return ResponseEntity.noContent().build()
	}
}
