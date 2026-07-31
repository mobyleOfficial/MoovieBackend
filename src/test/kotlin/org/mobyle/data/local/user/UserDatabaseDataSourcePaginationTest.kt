package org.mobyle.data.local.user

import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.mobyle.data.local.database.MoviesTable
import org.mobyle.data.local.database.UserListItemsTable
import org.mobyle.data.local.database.UserListsTable
import org.mobyle.data.local.database.UserMoviesTable
import org.mobyle.data.local.database.UsersTable
import org.mobyle.data.local.movies.MovieCatalogDataSource
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UserDatabaseDataSourcePaginationTest {

    private lateinit var dataSource: UserDatabaseDataSourceImpl
    private val userExternalId = "user-ext-1"

    private var userDbId: Long = 0
    private var movieDbIds: List<Long> = emptyList()

    @BeforeTest
    fun setUp() {
        Database.connect("jdbc:h2:mem:test_pagination;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(UsersTable, MoviesTable, UserMoviesTable, UserListsTable, UserListItemsTable)
        }
        dataSource = UserDatabaseDataSourceImpl(mockk<MovieCatalogDataSource>())

        transaction {
            UsersTable.insert {
                it[externalId] = userExternalId
                it[username] = "testuser"
                it[email] = "test@test.com"
                it[createdAt] = Clock.System.now()
            }

            for (i in 1..3) {
                MoviesTable.insert {
                    it[tmdbId] = i
                    it[title] = "Movie $i"
                    it[posterPath] = "/poster$i.jpg"
                }
            }
        }

        transaction {
            userDbId = UsersTable.selectAll().first()[UsersTable.id].value
            movieDbIds = MoviesTable.selectAll().map { it[MoviesTable.id].value }
        }
    }

    @AfterTest
    fun tearDown() {
        transaction {
            SchemaUtils.drop(UserListItemsTable, UserListsTable, UserMoviesTable, MoviesTable, UsersTable)
        }
    }

    @Test
    fun `getFavoriteMovies totalResults matches actual movies returned`() {
        transaction {
            UserMoviesTable.insert {
                it[userId] = userDbId
                it[movieId] = movieDbIds[0]
                it[status] = "watched"
                it[isFavorite] = true
                it[importSource] = "manual"
                it[createdAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
            }
        }

        val result = dataSource.getFavoriteMovies(userExternalId, page = 1, pageSize = 20)

        assertEquals(1, result.totalResults)
        assertEquals(1, result.totalPages)
        assertEquals(1, result.movies.size)
    }

    @Test
    fun `getWatchlistMovies totalResults matches actual movies returned`() {
        transaction {
            UserMoviesTable.insert {
                it[userId] = userDbId
                it[movieId] = movieDbIds[0]
                it[status] = "want_to_watch"
                it[isFavorite] = false
                it[importSource] = "manual"
                it[createdAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
            }
        }

        val result = dataSource.getWatchlistMovies(userExternalId, page = 1, pageSize = 20)

        assertEquals(1, result.totalResults)
        assertEquals(1, result.totalPages)
        assertEquals(1, result.movies.size)
    }

    @Test
    fun `getListDetail totalMovies and totalPages are consistent with movies`() {
        val listId = transaction {
            val id = UserListsTable.insert {
                it[userId] = userDbId
                it[name] = "Test List"
                it[createdAt] = Clock.System.now()
            }[UserListsTable.id].value

            movieDbIds.forEachIndexed { index: Int, mid: Long ->
                UserListItemsTable.insert {
                    it[listId] = id
                    it[movieId] = mid
                    it[position] = index
                    it[addedAt] = Clock.System.now()
                }
            }

            id
        }

        val result = dataSource.getListDetail(listId, page = 1, pageSize = 2)

        assertEquals(3, result.totalMovies)
        assertEquals(2, result.totalPages) // ceil(3/2)
        assertEquals(2, result.movies.size)
    }

    @Test
    fun `getListDetail page 2 returns remaining movies`() {
        val listId = transaction {
            val id = UserListsTable.insert {
                it[userId] = userDbId
                it[name] = "Paginated List"
                it[createdAt] = Clock.System.now()
            }[UserListsTable.id].value

            movieDbIds.forEachIndexed { index: Int, mid: Long ->
                UserListItemsTable.insert {
                    it[listId] = id
                    it[movieId] = mid
                    it[position] = index
                    it[addedAt] = Clock.System.now()
                }
            }

            id
        }

        val page2 = dataSource.getListDetail(listId, page = 2, pageSize = 2)

        assertEquals(3, page2.totalMovies)
        assertEquals(2, page2.totalPages)
        assertEquals(1, page2.movies.size)
    }

    @Test
    fun `getUserLists movieCount matches joinable movies`() {
        transaction {
            val id = UserListsTable.insert {
                it[userId] = userDbId
                it[name] = "Count Test"
                it[createdAt] = Clock.System.now()
            }[UserListsTable.id].value

            movieDbIds.forEachIndexed { index: Int, mid: Long ->
                UserListItemsTable.insert {
                    it[listId] = id
                    it[movieId] = mid
                    it[position] = index
                    it[addedAt] = Clock.System.now()
                }
            }
        }

        val result = dataSource.getUserLists(userExternalId, page = 1, pageSize = 20)

        assertEquals(1, result.totalResults)
        assertEquals(3, result.lists.first().movieCount)
    }
}
