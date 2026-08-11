package com.nithusan.jobtracker

class ApplicationNotFoundException(id: Long) : RuntimeException("Fant ingen søknad med id $id")
