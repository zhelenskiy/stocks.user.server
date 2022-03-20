package zhelenskiy.ru

import io.ktor.client.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import zhelenskiy.ru.plugins.*

fun main() {
    embeddedServer(Netty, port = 8081, host = "0.0.0.0") {
        configureRouting(HttpClient())
    }.start(wait = true)
}
