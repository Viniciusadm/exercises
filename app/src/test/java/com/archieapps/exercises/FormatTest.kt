package com.archieapps.exercises

import com.archieapps.exercises.core.format.Format
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun `volume uses kilos below ten tons and tons above it`() {
        assertEquals("5.580 kg", Format.volume(5580.0))
        assertEquals("9.999 kg", Format.volume(9999.4))
        assertEquals("10,0 t", Format.volume(10_000.0))
        assertEquals("838,7 t", Format.volume(838_749.0))
        assertEquals("—", Format.volume(null))
    }

    @Test
    fun `weight drops the decimal when it is not needed`() {
        assertEquals("55 kg", Format.weight(55.0))
        assertEquals("7,5 kg", Format.weight(7.5))
        assertEquals("55 kg", Format.weight(55.04))
        assertEquals("—", Format.weight(null))
    }

    @Test
    fun `minutes turn into hours after sixty`() {
        assertEquals("48 min", Format.minutes(48))
        assertEquals("1h", Format.minutes(60))
        assertEquals("1h02", Format.minutes(62))
        assertEquals("2h30", Format.minutes(150))
    }

    @Test
    fun `relative days speak portuguese`() {
        assertEquals("hoje", Format.relativeDays(0))
        assertEquals("ontem", Format.relativeDays(1))
        assertEquals("há 6 dias", Format.relativeDays(6))
        assertEquals("nunca", Format.relativeDays(null))
    }

    @Test
    fun `dates and months are short and lowercase`() {
        val date = LocalDate.of(2026, 8, 13)

        assertEquals("13/08", Format.dayMonth(date))
        assertEquals("qui", Format.weekdayShort(date))
        assertEquals("qui, 13/08", Format.dateWithWeekday(date))
        assertEquals("ago", Format.monthShort(YearMonth.of(2026, 8)))
    }

    @Test
    fun `signed values show the direction`() {
        assertEquals("+5", Format.signed(5.0))
        assertEquals("−2,5", Format.signed(-2.5))
        assertEquals("—", Format.signed(0.0))
    }

    @Test
    fun `parsers accept the api format and reject junk`() {
        assertEquals(LocalDate.of(2026, 8, 13), Format.parseDate("2026-08-13"))
        assertEquals(LocalDate.of(2026, 8, 13), Format.parseDate("2026-08-13T18:00:00-03:00"))
        assertEquals(null, Format.parseDate("nope"))
        assertEquals(YearMonth.of(2026, 8), Format.parseMonth("2026-08"))
    }

    @Test
    fun `percent rounds the share`() {
        assertEquals("33%", Format.percent(0.3273))
        assertEquals("100%", Format.percent(1.0))
    }
}
