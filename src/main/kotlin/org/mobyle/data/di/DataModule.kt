package org.mobyle.data.di

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.mobyle.data.local.oauth.OAuthStateDataSource
import org.mobyle.data.local.oauth.OAuthStateDataSourceImpl
import org.mobyle.data.local.user.UserLocalDataSource
import org.mobyle.data.local.user.UserLocalDataSourceImpl
import org.mobyle.data.remote.articles.ArticlesDataSource
import org.mobyle.data.remote.articles.ArticlesDataSourceImpl
import org.mobyle.data.remote.comments.CommentsDataSource
import org.mobyle.data.remote.comments.CommentsDataSourceImpl
import org.mobyle.data.remote.oauth.OAuthDataSource
import org.mobyle.data.remote.oauth.OAuthDataSourceImpl
import org.mobyle.data.remote.tmdb.TmdbDataSource
import org.mobyle.data.remote.tmdb.TmdbDataSourceImpl
import org.mobyle.data.repository.AuthRepositoryImpl
import org.mobyle.data.repository.ArticlesRepositoryImpl
import org.mobyle.data.repository.CommentsRepositoryImpl
import org.mobyle.data.repository.MoviesRepositoryImpl
import org.mobyle.data.repository.ProfileRepositoryImpl
import org.mobyle.data.repository.UserActivitiesRepositoryImpl
import org.mobyle.data.repository.UserRepositoryImpl
import org.mobyle.data.util.JWTUtil
import org.mobyle.domain.repository.AuthRepository
import org.mobyle.domain.repository.ArticlesRepository
import org.mobyle.domain.repository.CommentsRepository
import org.mobyle.domain.repository.MoviesRepository
import org.mobyle.domain.repository.ProfileRepository
import org.mobyle.domain.repository.UserActivitiesRepository
import org.mobyle.domain.repository.UserRepository
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
        try {
            TmdbDataSourceImpl(httpClient = get())
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create TmdbDataSourceImpl: ${e.message}", e)
        }
    }

    single<MoviesRepository> {
        try {
            MoviesRepositoryImpl(tmdbDataSource = get())
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create MoviesRepositoryImpl: ${e.message}", e)
        }
    }

    single<ProfileRepository> {
        ProfileRepositoryImpl()
    }

    single<UserActivitiesRepository> {
        UserActivitiesRepositoryImpl()
    }

    single<CommentsDataSource> {
        CommentsDataSourceImpl()
    }

    single<CommentsRepository> {
        CommentsRepositoryImpl(commentsDataSource = get())
    }

    single<ArticlesDataSource> {
        ArticlesDataSourceImpl()
    }

    single<ArticlesRepository> {
        ArticlesRepositoryImpl(articlesDataSource = get())
    }

    // Auth-related datasources and repositories
    single<UserLocalDataSource> {
        UserLocalDataSourceImpl()
    }

    single<OAuthStateDataSource> {
        OAuthStateDataSourceImpl()
    }

    single<OAuthDataSource> {
        val clientId = System.getenv("OAUTH_CLIENT_ID")?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "OAUTH_CLIENT_ID environment variable is not set. Set it to your OAuth provider's client ID."
            )
        val clientSecret = System.getenv("OAUTH_CLIENT_SECRET")?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "OAUTH_CLIENT_SECRET environment variable is not set. Set it to your OAuth provider's client secret."
            )
        val providerUrl = System.getenv("OAUTH_PROVIDER_URL")?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "OAUTH_PROVIDER_URL environment variable is not set. Set it to your OAuth provider's base URL."
            )
        val redirectUri = System.getenv("OAUTH_REDIRECT_URI")?.takeIf { it.isNotBlank() }
            ?: "http://localhost:8080/api/v1/auth/oauth/callback"

        val oauthHttpClient = HttpClient(CIO) {
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

        OAuthDataSourceImpl(
            httpClient = oauthHttpClient,
            oauthClientId = clientId,
            oauthClientSecret = clientSecret,
            oauthProviderUrl = providerUrl,
            oauthRedirectUri = redirectUri
        )
    }

    single<JWTUtil> {
        val secret = System.getenv("JWT_SECRET")?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "JWT_SECRET environment variable is not set. Generate one with: openssl rand -base64 32"
            )
        val expirySeconds = System.getenv("JWT_EXPIRY_SECONDS")?.toLongOrNull() ?: 3600
        val issuer = System.getenv("JWT_ISSUER")?.takeIf { it.isNotBlank() } ?: "moovie-backend"

        JWTUtil(
            jwtSecret = secret,
            jwtExpirySeconds = expirySeconds,
            jwtIssuer = issuer
        )
    }

    single<UserRepository> {
        UserRepositoryImpl(userLocalDataSource = get())
    }

    single<AuthRepository> {
        AuthRepositoryImpl(
            oauthDataSource = get(),
            oauthStateDataSource = get(),
            userRepository = get(),
            jwtUtil = get()
        )
    }
}
