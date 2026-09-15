package com.oregontrail.engine

import java.io.File
import kotlin.test.Test

/**
 * Runs the [TestPilot] over many seeds and writes a balance report, so the
 * economy and difficulty can be tuned from data rather than guesswork.
 */
class BalanceTest {

    private data class Row(
        val occ: Occupation, val diff: Difficulty, val n: Int,
        val arrivals: Int, val avgDays: Int, val avgSurvivors: Double, val avgScore: Int
    )

    private fun measure(occ: Occupation, diff: Difficulty, n: Int = 120): Row {
        var arrivals = 0
        var days = 0
        var survivors = 0.0
        var score = 0
        for (s in 1..n) {
            val o = TestPilot(s * 31L + diff.ordinal, occ, diff).play()
            if (o.arrived) {
                arrivals++
                score += o.score
            }
            days += o.days
            survivors += o.survived
        }
        return Row(
            occ, diff, n, arrivals,
            days / n,
            survivors / n,
            if (arrivals == 0) 0 else score / arrivals
        )
    }

    @Test
    fun balance_report() {
        val rows = ArrayList<Row>()
        for (diff in Difficulty.entries) {
            for (occ in Occupation.entries) rows.add(measure(occ, diff))
        }
        val out = StringBuilder("OCCUPATION  DIFF     ARRIVED   AVG DAYS  AVG ALIVE  AVG SCORE\n")
        for (r in rows) {
            out.append(
                "%-11s %-8s %3d/%3d   %6d    %6.2f    %6d\n".format(
                    r.occ.displayName, r.diff.displayName, r.arrivals, r.n, r.avgDays,
                    r.avgSurvivors, r.avgScore
                )
            )
        }
        val file = File(System.getProperty("java.io.tmpdir"), "balance_report.txt")
        file.writeText(out.toString())
        println(out)

        fun row(occ: Occupation, diff: Difficulty) = rows.first { it.occ == occ && it.diff == diff }
        fun rate(r: Row) = r.arrivals.toDouble() / r.n

        // The bot must not hang or leave a broken state.
        for (r in rows) assert(r.avgDays in 90..280) { "unreasonable days for ${r.occ}/${r.diff}" }

        // Harder settings must be harder, and a good player should usually but
        // not always make it on Normal.
        val b = Occupation.BANKER
        assert(rate(row(b, Difficulty.EASY)) >= rate(row(b, Difficulty.NORMAL))) { "easy should beat normal" }
        assert(rate(row(b, Difficulty.NORMAL)) >= rate(row(b, Difficulty.HARD))) { "normal should beat hard" }
        assert(rate(row(b, Difficulty.NORMAL)) in 0.70..0.97) { "normal arrival ${rate(row(b, Difficulty.NORMAL))}" }
        assert(rate(row(b, Difficulty.HARD)) in 0.35..0.85) { "hard arrival ${rate(row(b, Difficulty.HARD))}" }

        // More points require more risk: the Farmer starts poorer and so should
        // not out-survive the Banker, but must score far higher per arrival.
        assert(rate(row(Occupation.FARMER, Difficulty.NORMAL)) <= rate(row(b, Difficulty.NORMAL)) + 0.05)
        assert(row(Occupation.FARMER, Difficulty.NORMAL).avgScore > row(Occupation.CARPENTER, Difficulty.NORMAL).avgScore)
        assert(row(Occupation.CARPENTER, Difficulty.NORMAL).avgScore > row(b, Difficulty.NORMAL).avgScore)
    }
}
