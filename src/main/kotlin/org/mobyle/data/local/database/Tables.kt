package org.mobyle.data.local.database

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object UsersTable : LongIdTable("users") {
    val externalId = varchar("external_id", 255).uniqueIndex()
    val username = varchar("username", 255)
    val email = varchar("email", 255).nullable()
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val createdAt = timestamp("created_at")
}

object MoviesTable : LongIdTable("movies") {
    val tmdbId = integer("tmdb_id").uniqueIndex()
    val title = varchar("title", 500)
    val originalTitle = varchar("original_title", 500).nullable()
    val year = integer("year").nullable()
    val posterPath = varchar("poster_path", 500).nullable()
}

object UserMoviesTable : LongIdTable("user_movies") {
    val userId = reference("user_id", UsersTable)
    val movieId = reference("movie_id", MoviesTable)
    val status = varchar("status", 50) // watched, want_to_watch, dropped
    val rating = float("rating").nullable()
    val review = text("review").nullable()
    val rewatches = integer("rewatches").default(0)
    val watchedAt = timestamp("watched_at").nullable()
    val importSource = varchar("source", 50) // filmow, letterboxd, manual
    val importedAt = timestamp("imported_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    init {
        uniqueIndex("uq_user_movie_source", userId, movieId, importSource)
    }
}

object UserListsTable : LongIdTable("user_lists") {
    val userId = reference("user_id", UsersTable)
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val isPublic = bool("is_public").default(true)
    val createdAt = timestamp("created_at")
}

object UserListItemsTable : LongIdTable("user_list_items") {
    val listId = reference("list_id", UserListsTable)
    val movieId = reference("movie_id", MoviesTable)
    val position = integer("position")
    val addedAt = timestamp("added_at")

    init {
        uniqueIndex("uq_list_movie", listId, movieId)
    }
}

object TagsTable : LongIdTable("tags") {
    val userId = reference("user_id", UsersTable)
    val name = varchar("name", 255)

    init {
        uniqueIndex("uq_user_tag", userId, name)
    }
}

object MovieTagsTable : LongIdTable("movie_tags") {
    val tagId = reference("tag_id", TagsTable)
    val userMovieId = reference("user_movie_id", UserMoviesTable)

    init {
        uniqueIndex("uq_tag_user_movie", tagId, userMovieId)
    }
}
