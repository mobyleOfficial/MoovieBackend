package com.moovie.backend.data.repository;

import com.moovie.backend.data.remote.tmdb.TmdbDataSource;
import com.moovie.backend.data.remote.tmdb.TmdbMapper;
import com.moovie.backend.domain.model.*;
import com.moovie.backend.domain.repository.MoviesRepository;
import com.moovie.backend.domain.model.MovieListing;
import com.moovie.backend.domain.model.MovieListListing;
import com.moovie.backend.domain.model.MovieReviewListing;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class MoviesRepositoryImpl implements MoviesRepository {

    private final TmdbDataSource tmdbDataSource;

    public MoviesRepositoryImpl(TmdbDataSource tmdbDataSource) {
        this.tmdbDataSource = tmdbDataSource;
    }

    @Override
    public MovieListing getTrendingMovies(int page) {
        return TmdbMapper.toMovieListing(tmdbDataSource.getTrendingMovies(page));
    }

    @Override
    public MovieDetail getMovieDetail(int movieId) {
        return TmdbMapper.toMovieDetail(tmdbDataSource.getMovieDetail(movieId));
    }

    @Override
    public MovieListing searchMovies(String query, int page) {
        return TmdbMapper.toMovieListing(tmdbDataSource.searchMovies(query, page));
    }

    @Override
    public MovieListing discoverMovies(int page, String year, String releaseDateGte, String releaseDateLte,
                                        String sortBy, String withGenres, String withOriginalLanguage,
                                        String withOriginCountry, String voteCountGte) {
        return TmdbMapper.toMovieListing(tmdbDataSource.discoverMovies(
                page, year, releaseDateGte, releaseDateLte, sortBy, withGenres,
                withOriginalLanguage, withOriginCountry, voteCountGte));
    }

    @Override
    public List<Genre> getGenres() {
        return tmdbDataSource.getGenres().genres().stream()
                .map(TmdbMapper::toGenre)
                .toList();
    }

    @Override
    public List<Country> getCountries() {
        return tmdbDataSource.getCountries().stream()
                .map(TmdbMapper::toCountry)
                .toList();
    }

    @Override
    public List<Language> getLanguages() {
        return tmdbDataSource.getLanguages().stream()
                .map(TmdbMapper::toLanguage)
                .toList();
    }

    @Override
    public MovieReviewListing getMovieReviews(int movieId, int page) {
        var response = tmdbDataSource.getMovieReviews(movieId, page);
        var reviews = response.results() != null
                ? response.results().stream().map(TmdbMapper::toMovieReview).toList()
                : Collections.<MovieReview>emptyList();
        return new MovieReviewListing(response.totalPages(), response.totalResults(), reviews);
    }

    @Override
    public MovieListing getUserFavoriteMovies(String userId, int page) {
        // TODO: implement with user storage
        return new MovieListing(0, 0, Collections.emptyList());
    }

    @Override
    public MovieListing getUserWatchList(String userId, int page) {
        // TODO: implement with user storage
        return new MovieListing(0, 0, Collections.emptyList());
    }

    @Override
    public MovieListListing getMovieLists(int page, String userId) {
        // TODO: implement with user storage
        return new MovieListListing(0, 0, Collections.emptyList());
    }

    @Override
    public MovieListListing getUserMovieLists(int page) {
        // TODO: implement with user storage
        return new MovieListListing(0, 0, Collections.emptyList());
    }

    @Override
    public MovieListDetail getMovieListDetail(int listId, int page) {
        // TODO: implement with user storage
        return new MovieListDetail(listId, "", "", null, 0, Collections.emptyList(),
                Collections.emptyList(), 0, 0, 0, 0, false, Collections.emptyList());
    }

    @Override
    public MovieListListing getFeaturedLists(int page) {
        // TODO: implement with user storage
        return new MovieListListing(0, 0, Collections.emptyList());
    }
}
