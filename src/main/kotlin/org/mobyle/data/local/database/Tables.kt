package org.mobyle.data.local.database

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object UsersTable : LongIdTable("users") {
    val externalId = varchar("external_id", 255).uniqueIndex()
    val username = varchar("username", 255).uniqueIndex()
    val email = varchar("email", 255).nullable()
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val bio = varchar("bio", 500).nullable()
    val passwordHash = varchar("password_hash", 255).nullable()
    val createdAt = timestamp("created_at")
}

object UserFollowsTable : LongIdTable("user_follows") {
    val followerId = reference("follower_id", UsersTable)
    val followedId = reference("followed_id", UsersTable)
    val createdAt = timestamp("created_at")

    init {
        uniqueIndex("uq_follower_followed", followerId, followedId)
    }
}

object MoviesTable : LongIdTable("movies") {
    val tmdbId = integer("tmdb_id").uniqueIndex()
    val title = varchar("title", 500)
    val localTitle = varchar("local_title", 500).nullable()
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
    val isFavorite = bool("is_favorite").default(false)
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

object ArticlesTable : LongIdTable("articles") {
    val sourceUrl = varchar("source_url", 1000).uniqueIndex()
    val title = varchar("title", 500)
    val summary = text("summary")
    val content = text("content")
    val imageUrl = varchar("image_url", 1000).nullable()
    val sourceName = varchar("source", 100)
    val publishedAt = timestamp("published_at")
    val scrapedAt = timestamp("scraped_at")
}

object MovieTagsTable : LongIdTable("movie_tags") {
    val tagId = reference("tag_id", TagsTable)
    val userMovieId = reference("user_movie_id", UserMoviesTable)

    init {
        uniqueIndex("uq_tag_user_movie", tagId, userMovieId)
    }
}

object TokenBlocklistTable : LongIdTable("token_blocklist") {
    val token = varchar("token", 1000).uniqueIndex()
    val expiresAt = long("expires_at")
}
