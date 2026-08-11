package com.nithusan.jobtracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class JobTrackerApplication

fun main(args: Array<String>) {
	runApplication<JobTrackerApplication>(*args)
}
