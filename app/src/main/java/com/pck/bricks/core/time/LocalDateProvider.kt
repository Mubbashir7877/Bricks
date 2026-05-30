package com.pck.bricks.core.time

import java.time.LocalDate
import java.time.LocalDateTime

interface LocalDateProvider {
    fun today(): LocalDate
    fun now(): LocalDateTime
}

class SystemLocalDateProvider : LocalDateProvider {
    override fun today(): LocalDate = LocalDate.now()
    override fun now(): LocalDateTime = LocalDateTime.now()
}
