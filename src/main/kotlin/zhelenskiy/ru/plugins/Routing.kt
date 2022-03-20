package zhelenskiy.ru.plugins

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

val permittedUserPosts = listOf(
    "addFreeMoney",
    "addUser",
    "buyStocks",
    "sellStocks",
)

val permittedUserGets = listOf(
    "getCompanyById",
    "getUserById",
    "getStocksCompaniesByUser",
    "getStockHoldersByCompany",
    "getUserStocksCount",
)

const val mainServerAddress = "http://127.0.0.1:8080"

fun Application.configureRouting(client: HttpClient) {
    routing {
        for (permittedGet in permittedUserGets) {
            get("/$permittedGet/{param...}") {
                call.respondText(client.get(mainServerAddress + call.request.uri).bodyAsText())
            }
        }
        for (permittedPost in permittedUserPosts) {
            post("/$permittedPost/{param...}") {
                call.respondText(client.post(mainServerAddress + call.request.uri).bodyAsText())
            }
        }
    }
}
