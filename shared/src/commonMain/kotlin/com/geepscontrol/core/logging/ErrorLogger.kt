package com.geepscontrol.core.logging

interface ErrorLogger {
    fun record(
        throwable: Throwable,
        context: String,
    )
}

object ConsoleErrorLogger : ErrorLogger {
    override fun record(
        throwable: Throwable,
        context: String,
    ) {
        println("GeepsControl error [$context]: ${throwable.message}")
        throwable.printStackTrace()
    }
}
