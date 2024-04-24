package io.github.rafambn.kmap

import io.ktor.client.HttpClient

object KtorClient {
    fun provideHttpClient(): HttpClient {
        return HttpClient()
    }
}