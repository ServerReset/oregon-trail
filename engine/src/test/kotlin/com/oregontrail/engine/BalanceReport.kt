package com.oregontrail.engine

import java.io.File

/**
 * Prints a Markdown balance report and, when `-Dbalance.out=...` is set, writes
 * it to that file. Run it with `./gradlew balanceReport`.
 */
fun main(args: Array<String>) {
    val n = (System.getProperty("balance.games") ?: args.firstOrNull())?.toIntOrNull() ?: 120
    val rows = BalanceHarness.all(n)
    val report = BalanceHarness.markdown(rows)
    println(report)
    System.getProperty("balance.out")?.let { path ->
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(report)
        println("Wrote $path")
    }
}
