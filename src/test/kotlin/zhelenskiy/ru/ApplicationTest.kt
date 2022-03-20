package zhelenskiy.ru

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.testcontainers.containers.FixedHostPortGenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.shaded.org.bouncycastle.crypto.tls.ConnectionEnd.client
import zhelenskiy.ru.plugins.*
import kotlin.test.assertTrue

@Testcontainers
class ApplicationTest {

    @Container
    private val exchangeContainer = FixedHostPortGenericContainer("stocks-server")
        .withFixedExposedPort(8080, 8080)
        .withExposedPorts(8080)

    private fun testConfiguredApplication(body: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            configureRouting(HttpClient())
        }
        body()
    }

    @Test
    fun getCompanyById() = testConfiguredApplication {
        client.get("/getCompanyById/0").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("""{"name":"Yandex","stocksState":{"count":97,"price":1000}}""", bodyAsText())
        }
        client.get("/getCompanyById/1").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("""{"name":"Google","stocksState":{"count":197,"price":2000}}""", bodyAsText())
        }
    }

    @Test
    fun getUserById() = testConfiguredApplication {
        client.get("/getUserById/0").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("""{"name":"Evgeniy","freeMoney":5000}""", bodyAsText())
        }
        client.get("/getUserById/1").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("""{"name":"Petr","freeMoney":4000}""", bodyAsText())
        }
    }

    @Test
    fun getStocksCompaniesByUser() = testConfiguredApplication {
        client.get("/getStocksCompaniesByUser/0").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("[0,1]", bodyAsText())
        }
        client.get("/getStocksCompaniesByUser/1").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("[0,1]", bodyAsText())
        }
    }

    @Test
    fun getStockHoldersByCompany() = testConfiguredApplication {
        client.get("/getStockHoldersByCompany/0").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("[0,1]", bodyAsText())
        }
        client.get("/getStockHoldersByCompany/1").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("[0,1]", bodyAsText())
        }
    }

    @Test
    fun getUserStocksCount() = testConfiguredApplication {
        for (companyId in 0..1) {
            for (userId in 0..1) {
                client.get("/getUserStocksCount/$companyId/$userId").apply {
                    assertEquals(HttpStatusCode.OK, status)
                    assertEquals(if (companyId == userId) "1" else "2", bodyAsText())
                }
            }
        }
    }

    @Test
    fun addFreeMoney() = testConfiguredApplication {
        client.post("/addFreeMoney/0/200").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        client.get("/getUserById/0").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("""{"name":"Evgeniy","freeMoney":5200}""", bodyAsText())
        }
    }

    @Test
    fun addUser() = testConfiguredApplication {
        client.post("/addUser/Serezha?freeMoney=200").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("2", bodyAsText())
        }
        client.get("/getUserById/2").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("""{"name":"Serezha","freeMoney":200}""", bodyAsText())
        }
        client.post("/addUser/Yuri").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("3", bodyAsText())
        }
        client.get("/getUserById/3").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("""{"name":"Yuri","freeMoney":0}""", bodyAsText())
        }
    }

    @Test
    fun buyStocks() = testConfiguredApplication {
        client.post("/buyStocks/0/1/2/3").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("false", bodyAsText())
        }
        client.post("/buyStocks/0/1/2/2000").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("true", bodyAsText())
        }
        client.get("/getUserById/0").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("""{"name":"Evgeniy","freeMoney":1000}""", bodyAsText())
        }
        client.get("/getUserStocksCount/1/0").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("4", bodyAsText())
        }
        client.get("/getCompanyById/1").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("""{"name":"Google","stocksState":{"count":195,"price":2000}}""", bodyAsText())
        }
    }

    @Test
    fun sellStocks() = testConfiguredApplication {
        client.post("/sellStocks/0/1/2/3").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("false", bodyAsText())
        }
        client.post("/sellStocks/0/1/2/2000").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("true", bodyAsText())
        }
        client.get("/getUserById/0").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("""{"name":"Evgeniy","freeMoney":9000}""", bodyAsText())
        }
        client.get("/getUserStocksCount/1/0").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("0", bodyAsText())
        }
        client.get("/getCompanyById/1").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("""{"name":"Google","stocksState":{"count":199,"price":2000}}""", bodyAsText())
        }
    }

    @Test
    fun forbiddenCommands() = testConfiguredApplication {
        for (command in listOf<suspend ApplicationTestBuilder.() -> Unit>(
            { client.put("/changePrice/0/1") },
            { client.post("/addCompany/name") },
            { client.post("/addStocks/0/3") },
            { client.get("/ktor/application/shutdown") },
        )) {
            try {
                command()
                throw AssertionError("Must fail")
            } catch (e: ClientRequestException) {
                assertTrue("404 Not Found." in e.message)
            }
        }
    }
}