package com.archieapps.exercises.core.format

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val PtBr: Locale = Locale.forLanguageTag("pt-BR")

private val integerFormat: NumberFormat = NumberFormat.getIntegerInstance(PtBr)
private val decimalFormat: NumberFormat = NumberFormat.getNumberInstance(PtBr).apply {
    maximumFractionDigits = 1
    minimumFractionDigits = 1
}
private val distanceFormat: NumberFormat = NumberFormat.getNumberInstance(PtBr).apply {
    maximumFractionDigits = 2
    minimumFractionDigits = 1
}
private val monthDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM", PtBr)
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", PtBr)

object Format {
    const val PLACEHOLDER = "-"

    fun integer(value: Number?): String = value?.let { integerFormat.format(it.toLong()) } ?: PLACEHOLDER

    fun weightValue(kg: Double?): String {
        if (kg == null) return PLACEHOLDER
        val rounded = Math.round(kg * 10) / 10.0

        return if (rounded % 1.0 == 0.0) integerFormat.format(rounded.toLong()) else decimalFormat.format(rounded)
    }

    fun weight(kg: Double?): String = if (kg == null) PLACEHOLDER else "${weightValue(kg)} kg"

    fun distanceValue(km: Double?): String {
        if (km == null) return PLACEHOLDER
        val rounded = Math.round(km * 100) / 100.0

        return if (rounded % 1.0 == 0.0) integerFormat.format(rounded.toLong()) else distanceFormat.format(rounded)
    }

    fun distance(km: Double?): String = if (km == null) PLACEHOLDER else "${distanceValue(km)} km"

    fun speed(kmh: Double?): String = if (kmh == null) PLACEHOLDER else "${decimalFormat.format(kmh)} km/h"

    fun pace(secondsPerKm: Int?): String {
        if (secondsPerKm == null) return PLACEHOLDER

        return "%d'%02d\"/km".format(secondsPerKm / 60, secondsPerKm % 60)
    }

    fun volume(kg: Double?): String {
        if (kg == null) return PLACEHOLDER

        return if (kg < 10_000) "${integerFormat.format(Math.round(kg))} kg" else "${decimalFormat.format(kg / 1000)} t"
    }

    fun minutes(minutes: Int?): String = when {
        minutes == null -> PLACEHOLDER
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60}h"
        else -> "${minutes / 60}h${(minutes % 60).toString().padStart(2, '0')}"
    }

    fun clock(seconds: Long?): String {
        if (seconds == null) return PLACEHOLDER
        val safe = if (seconds < 0) 0 else seconds

        return "%d:%02d:%02d".format(safe / 3600, (safe % 3600) / 60, safe % 60)
    }

    fun duration(seconds: Long?): String = when {
        seconds == null -> PLACEHOLDER
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> {
            val remainder = seconds % 60
            if (remainder == 0L) "${seconds / 60}min" else "${seconds / 60}min ${remainder}s"
        }

        else -> {
            val remainder = (seconds % 3600) / 60
            if (remainder == 0L) "${seconds / 3600}h" else "${seconds / 3600}h${remainder.toString().padStart(2, '0')}"
        }
    }

    fun duration(seconds: Int?): String = duration(seconds?.toLong())

    fun timeOfDay(epochMillis: Long?): String = epochMillis?.let {
        timeFormatter.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
    } ?: PLACEHOLDER

    fun timeOfDay(isoInstant: String?): String = parseInstant(isoInstant)?.let { timeOfDay(it) } ?: PLACEHOLDER

    fun parseInstant(value: String?): Long? = value?.let {
        runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
    }

    fun percent(share: Double?): String = if (share == null) PLACEHOLDER else "${Math.round(share * 100)}%"

    fun signed(value: Double?): String {
        if (value == null || value == 0.0) return PLACEHOLDER
        val prefix = if (value > 0) "+" else "−"

        return prefix + weightValue(Math.abs(value))
    }

    fun weekdayShort(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, PtBr).take(3).trimEnd('.').lowercase(PtBr)

    fun monthLong(month: YearMonth): String =
        month.month.getDisplayName(TextStyle.FULL, PtBr).lowercase(PtBr)

    fun monthShort(month: YearMonth): String =
        month.month.getDisplayName(TextStyle.SHORT, PtBr).take(3).trimEnd('.').lowercase(PtBr)

    fun dayMonth(date: LocalDate): String = date.format(monthDayFormatter)

    fun dateWithWeekday(date: LocalDate): String = "${weekdayShort(date)}, ${dayMonth(date)}"

    fun relativeDays(days: Int?): String = when {
        days == null -> "nunca"
        days <= 0 -> "hoje"
        days == 1 -> "ontem"
        else -> "há $days dias"
    }

    fun parseDate(value: String?): LocalDate? = value?.let {
        runCatching { LocalDate.parse(it.take(10)) }.getOrNull()
    }

    fun parseMonth(value: String?): YearMonth? = value?.let {
        runCatching { YearMonth.parse(it.take(7)) }.getOrNull()
    }
}
