package bnorm.challenge.wiki

import bnorm.challenge.Challenge
import bnorm.challenge.roundDecimals

fun Challenge.toWiki(title: String) = buildList {
    add(title)

    for (group in groups) {
        for (result in group.results) {
            add(result.scores.average().roundDecimals(2).toString())
        }

        val avg = group.results.asSequence().flatMap { it.scores }.average()
        add("'''${avg.roundDecimals(2)}'''")
    }

    val avg = groups.asSequence().flatMap { it.results }.flatMap { it.scores }.average()
    add("'''${avg.roundDecimals(2)}'''")

    add("$sessions seasons")
}.joinToString(separator = " || ", prefix = "| ")
