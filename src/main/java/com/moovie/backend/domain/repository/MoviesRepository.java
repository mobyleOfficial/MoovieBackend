package com.moovie.backend.domain.repository;

import com.moovie.backend.domain.model.*;
import com.moovie.backend.domain.model.MovieListing;
import com.moovie.backend.domain.model.MovieListListing;
import com.moovie.backend.domain.model.MovieReviewListing;

import java.util.List;

public interface MoviesRepository {

    MovieListing getTrendingMovies(int page);

    MovieDetail getMovieDetail(int movieId);

    MovieListing searchMovies(String query, int page);

    MovieListing discoverMovies(int page, String year, String releaseDateGte, String releaseDateLte,
                                String sortBy, String withGenres, String withOriginalLanguage,
                                String withOriginCountry, String voteCountGte);

    List<Genre> getGenres();

    List<Country> getCountries();

    List<Language> getLanguages();

    MovieReviewListing getMovieReviews(int movieId, int page);

    MovieListing getUserFavoriteMovies(String userId, int page);

    MovieListing getUserWatchList(String userId, int page);

    MovieListListing getMovieLists(int page, String userId);

    MovieListListing getUserMovieLists(int page);

    MovieListDetail getMovieListDetail(int listId, int page);

    MovieListListing getFeaturedLists(int page);
}
