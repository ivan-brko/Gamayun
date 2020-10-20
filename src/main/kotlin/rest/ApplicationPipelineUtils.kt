package rest

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.serialization.*

fun Application.setupContentNegotiation() {
    install(ContentNegotiation) {
        json()
    }
}