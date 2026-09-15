package com.oregontrail.engine


    /** Test hook: fires a named event and returns its messages. */
internal fun Game.debugFireEvent(id: String): List<String> {
        val msgs = ArrayList<String>()
        applyEvent(id, msgs)
        return msgs
    }

    /** Test hook: the list of event ids the game can produce. */
internal fun Game.debugEventIds(): List<String> =
        (eventPool(0) + eventPool(1500)).toSet().toList()
