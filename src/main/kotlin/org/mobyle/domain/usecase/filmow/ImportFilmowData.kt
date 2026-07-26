package org.mobyle.domain.usecase.filmow

import org.mobyle.data.local.user.UserDatabaseDataSource
import org.mobyle.domain.model.FilmowProfile
import org.slf4j.LoggerFactory

class ImportFilmowData(
    private val userDatabaseDataSource: UserDatabaseDataSource
) {
    private val log = LoggerFactory.getLogger(ImportFilmowData::class.java)

    operator fun invoke(userExternalId: String, profile: FilmowProfile) {
        log.info("Importing Filmow data for user $userExternalId: " +
            "${profile.watched.size} watched, ${profile.watchlist.size} watchlist, " +
            "${profile.favorites.size} favorites, ${profile.lists.size} lists")

        if (profile.watched.isNotEmpty()) {
            userDatabaseDataSource.importMovies(
                userExternalId = userExternalId,
                movies = profile.watched,
                status = "watched"
            )
        }

        if (profile.watchlist.isNotEmpty()) {
            userDatabaseDataSource.importMovies(
                userExternalId = userExternalId,
                movies = profile.watchlist,
                status = "want_to_watch"
            )
        }

        if (profile.favorites.isNotEmpty()) {
            userDatabaseDataSource.importMovies(
                userExternalId = userExternalId,
                movies = profile.favorites,
                status = "watched",
                isFavorite = true
            )
        }

        if (profile.lists.isNotEmpty()) {
            userDatabaseDataSource.importLists(
                userExternalId = userExternalId,
                lists = profile.lists
            )
        }

        log.info("Filmow import completed for user $userExternalId")
    }
}
