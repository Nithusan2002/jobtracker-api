package com.nithusan.jobtracker

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ApplicationRepository : JpaRepository<Application, Long>
