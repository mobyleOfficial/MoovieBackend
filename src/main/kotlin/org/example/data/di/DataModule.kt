package org.example.data.di

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.example.data.remote.TmdbDataSource
import org.example.data.remote.TmdbDataSourceImpl
import org.example.data.repository.MoviesRepositoryImpl
import org.example.data.repository.ProfileRepositoryImpl
import org.example.data.repository.UserActivitiesRepositoryImpl
import org.example.domain.repository.MoviesRepository
import org.example.domain.repository.ProfileRepository
import org.example.domain.repository.UserActivitiesRepository
import org.koin.dsl.module

val dataModule = module {
    single<HttpClient> {
        val apiKey = System.getenv("TMDB_API_KEY")?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "TMDB_API_KEY environment variable is not set. Set it to your TMDB API Bearer token."
            )

        HttpClient(CIO) {
            defaultRequest {
                url("https://api.themoviedb.org/3/")
                headers.append(HttpHeaders.Authorization, "Bearer $apiKey")
                headers.append(HttpHeaders.Accept, "application/json")
            }
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = false
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }

    single<TmdbDataSource> {
        TmdbDataSourceImpl(httpClient = get())
    }

    single<MoviesRepository> {
        MoviesRepositoryImpl(tmdbDataSource = get())
    }

    single<ProfileRepository> {
        ProfileRepositoryImpl()
    }

    single<UserActivitiesRepository> {
        UserActivitiesRepositoryImpl()
    }
}
