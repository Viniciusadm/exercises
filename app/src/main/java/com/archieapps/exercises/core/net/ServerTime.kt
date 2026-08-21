package com.archieapps.exercises.core.net

import java.time.LocalDate
import java.time.ZoneId

object ServerTime {
    val zone: ZoneId = ZoneId.of("America/Sao_Paulo")

    fun today(): LocalDate = LocalDate.now(zone)
}
