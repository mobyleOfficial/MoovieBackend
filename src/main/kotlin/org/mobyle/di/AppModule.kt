package org.mobyle.di

import org.mobyle.domain.usecase.activities.GetFriendsActivities
import org.mobyle.domain.usecase.activities.GetUserActivities
import org.mobyle.domain.usecase.activities.SubmitReview
import org.mobyle.domain.usecase.movies.*
import org.mobyle.domain.usecase.profile.GetPublicProfile
import org.mobyle.domain.usecase.profile.GetUserProfile
import org.mobyle.domain.usecase.profile.UpdateUserProfile
import org.koin.dsl.module

val appModule = module {
    // Movies use cases
    factory { GetTrendingMovies(repository = get()) }
    factory { GetMovieDetail(repository = get()) }
    factory { SearchMovies(repository = get()) }
    factory { DiscoverMovies(repository = get()) }
    factory { GetGenres(repository = get()) }
    factory { GetCountries(repository = get()) }
    factory { GetLanguages(repository = get()) }
    factory { GetMovieReviews(repository = get()) }
    factory { GetUserFavoriteMovies(repository = get()) }
    factory { GetUserWatchList(repository = get()) }
    factory { GetMovieLists(repository = get()) }
    factory { GetUserMovieLists(repository = get()) }
    factory { GetMovieListDetail(repository = get()) }
    factory { GetFeaturedLists(repository = get()) }

    // Profile use cases
    factory { GetUserProfile(repository = get()) }
    factory { UpdateUserProfile(repository = get()) }
    factory { GetPublicProfile(repository = get()) }

    // Activities use cases
    factory { GetUserActivities(repository = get()) }
    factory { GetFriendsActivities(repository = get()) }
    factory { SubmitReview(repository = get()) }
}
