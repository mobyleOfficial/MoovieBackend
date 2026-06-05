package com.moovie.backend.data.remote.tmdb;

import com.moovie.backend.data.remote.tmdb.model.TmdbResponses.*;

import java.util.List;

public interface TmdbDataSource {

    TmdbMovieListResponse getTrendingMovies(int page);

    TmdbMovieDetailResponse getMovieDetail(int movieId);

    TmdbMovieListResponse searchMovies(String query, int page);

    TmdbMovieListResponse discoverMovies(int page, String year, String releaseDateGte, String releaseDateLte,
                                          String sortBy, String withGenres, String withOriginalLanguage,
                                          String withOriginCountry, String voteCountGte);

    TmdbGenreListResponse getGenres();

    List<TmdbCountry> getCountries();

    List<TmdbLanguage> getLanguages();

    TmdbReviewListResponse getMovieReviews(int movieId, int page);
}
