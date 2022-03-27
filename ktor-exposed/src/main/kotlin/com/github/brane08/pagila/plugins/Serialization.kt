package com.github.brane08.pagila.plugins

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.configureSerialization() {
	install(ContentNegotiation) {
		jackson {
			configure(SerializationFeature.INDENT_OUTPUT, true)
			configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
			registerModule(JavaTimeModule())  // support java.time.* types
		}
	}
}
