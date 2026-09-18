package com.nannyapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BookingStatusTest {
    @Test
    fun `maps known API values to their matching status`() {
        assertEquals(BookingStatus.CONFIRMED, BookingStatus.fromApi("confirmed"))
        assertEquals(BookingStatus.IN_PROGRESS, BookingStatus.fromApi("in_progress"))
        assertEquals(BookingStatus.COMPLETED, BookingStatus.fromApi("completed"))
    }

    @Test
    fun `uses pending for an unknown API value`() {
        assertEquals(BookingStatus.PENDING, BookingStatus.fromApi("unexpected_status"))
    }
}
