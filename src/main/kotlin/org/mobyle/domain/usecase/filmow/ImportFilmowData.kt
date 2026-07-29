package org.mobyle.domain.usecase.filmow

import org.mobyle.data.local.user.UserDatabaseDataSource
import org.mobyle.domain.model.FilmowProfile
import org.slf4j.LoggerFactory

class ImportFilmowData(
    private val userDatabaseDataSource: UserDatabaseDataSource
) {
    private val log = LoggerFactory.getLogger(ImportFilmowData::class.java)

    operator fun invoke(userExternalId: String, profile: FilmowProfile) {
        log.info("[IMPORT] Starting — ${profile.watched.size} watched, " +
            "${profile.watchlist.size} watchlist, ${profile.favorites.size} favorites, " +
            "${profile.lists.size} lists")

        if (profile.watched.isNotEmpty()) {
            log.info("[IMPORT] Importing ${profile.watched.size} watched movies...")
            userDatabaseDataSource.importMovies(
                userExternalId = userExternalId,
                movies = profile.watched,
                status = "watched"
            )
            log.info("[IMPORT] Watched done.")
        }

        if (profile.watchlist.isNotEmpty()) {
            log.info("[IMPORT] Importing ${profile.watchlist.size} watchlist movies...")
            userDatabaseDataSource.importMovies(
                userExternalId = userExternalId,
                movies = profile.watchlist,
                status = "want_to_watch"
            )
            log.info("[IMPORT] Watchlist done.")
        }

        if (profile.favorites.isNotEmpty()) {
            log.info("[IMPORT] Importing ${profile.favorites.size} favorite movies...")
            userDatabaseDataSource.importMovies(
                userExternalId = userExternalId,
                movies = profile.favorites,
                status = "watched",
                isFavorite = true
            )
            log.info("[IMPORT] Favorites done.")
        }

        if (profile.lists.isNotEmpty()) {
            log.info("[IMPORT] Importing ${profile.lists.size} lists...")
            userDatabaseDataSource.importLists(
                userExternalId = userExternalId,
                lists = profile.lists
            )
            log.info("[IMPORT] Lists done.")
        }

        if (profile.recentlyWatched.isNotEmpty()) {
            log.info("[IMPORT] Importing ${profile.recentlyWatched.size} recently watched movies...")
            userDatabaseDataSource.importRecentlyWatched(
                userExternalId = userExternalId,
                movies = profile.recentlyWatched
            )
            log.info("[IMPORT] Recently watched done.")
        }

        log.info("[IMPORT] All done for user $userExternalId")
    }
}
