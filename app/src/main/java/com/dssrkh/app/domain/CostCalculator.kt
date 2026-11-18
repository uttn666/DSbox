package com.dssrkh.app.domain

import com.dssrkh.app.data.SettingsStore
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/**
 * 内置价格表（元 / 百万 tokens）。高峰时段按北京时间判断，
 * 法定节假日表暂未内置，后续随版本更新。
 */
object CostCalculator {

    data class Price(val hit: Double, val miss: Double, val output: Double)

    fun price(model: String, peak: Boolean): Price = when (model) {
        SettingsStore.MODEL_PRO ->
            if (peak) Price(0.30, 9.0, 27.0) else Price(0.15, 4.5, 13.5)
        else ->
            if (peak) Price(0.04, 2.0, 8.0) else Price(0.02, 1.0, 4.0)
    }

    fun isPeak(ts: Long, zone: ZoneId = ZoneId.of("Asia/Shanghai")): Boolean {
        val dt = Instant.ofEpochMilli(ts).atZone(zone)
        if (dt.dayOfWeek == DayOfWeek.SATURDAY || dt.dayOfWeek == DayOfWeek.SUNDAY) return false
        val minutes = dt.hour * 60 + dt.minute
        return minutes in (9 * 60 until 12 * 60) || minutes in (14 * 60 until 18 * 60)
    }

    /** 估算费用，单位元。 */
    fun costCny(model: String, peak: Boolean, hit: Long, miss: Long, output: Long): Double {
        val p = price(model, peak)
        return (hit * p.hit + miss * p.miss + output * p.output) / 1_000_000.0
    }
}
