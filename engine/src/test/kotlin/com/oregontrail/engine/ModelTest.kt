package com.oregontrail.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class ModelTest {

    @Test
    fun known_weekday_is_correct() {
        // 1 January 1848 was a Saturday.
        assertEquals("Saturday", GameDate(1848, 1, 1).dayName)
        // 29 February 1848 was a Tuesday (leap year).
        assertEquals("Tuesday", GameDate(1848, 2, 29).dayName)
    }

    @Test
    fun leap_day_rolls_into_march() {
        val d = GameDate(1848, 2, 28)
        d.plusDays(1)
        assertEquals(29, d.day)
        d.plusDays(1)
        assertEquals(3, d.month)
        assertEquals(1, d.day)
    }

    @Test
    fun non_leap_year_has_28_days_in_february() {
        val d = GameDate(1849, 2, 28)
        d.plusDays(1)
        assertEquals(3, d.month)
        assertEquals(1, d.day)
    }

    @Test
    fun health_state_buckets() {
        assertEquals(HealthState.DEAD, HealthState.fromHealth(0))
        assertEquals(HealthState.VERY_POOR, HealthState.fromHealth(10))
        assertEquals(HealthState.POOR, HealthState.fromHealth(40))
        assertEquals(HealthState.FAIR, HealthState.fromHealth(60))
        assertEquals(HealthState.GOOD, HealthState.fromHealth(100))
    }

    @Test
    fun party_member_dies_when_health_reaches_zero() {
        val m = PartyMember("Test", health = 15)
        m.hurt(5)
        assertEquals(10, m.health)
        assertEquals(HealthState.VERY_POOR, m.state)
        m.hurt(100)
        assertEquals(0, m.health)
        assertEquals(false, m.alive)
        assertEquals(HealthState.DEAD, m.state)
        m.heal(50)
        assertEquals(0, m.health) // the dead do not recover
    }
}
