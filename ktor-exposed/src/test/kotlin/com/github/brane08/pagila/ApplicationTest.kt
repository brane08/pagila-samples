package com.github.brane08.pagila

import com.github.brane08.pagila.plugins.configureRouting
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.assertEquals

class ApplicationTest {
	//	@Test
	fun testRoot() = testApplication {
		application {
			configureRouting()
		}
		client.get("/").apply {
			assertEquals(HttpStatusCode.OK, status)
			assertEquals("Hello World!", bodyAsText())
		}
	}
}
