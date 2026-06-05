package com.moovie.backend.data.remote.tmdb;

import com.moovie.backend.data.remote.tmdb.model.TmdbResponses.*;
import com.moovie.backend.domain.model.*;
import com.moovie.backend.domain.model.MovieListing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TmdbMapper {

    private TmdbMapper() {
    }

    public static Movie toMovie(TmdbMovieResponse response) {
        return new Movie(
                response.id(),
                response.title(),
                response.overview(),
                response.posterPath(),
                response.backdropPath(),
                response.voteAverage(),
                response.releaseDate()
        );
    }

    public static MovieListing toMovieListing(TmdbMovieListResponse response) {
        List<Movie> movies = response.results() != null
                ? response.results().stream().map(TmdbMapper::toMovie).toList()
                : Collections.emptyList();
        return new MovieListing(response.totalPages(), response.totalResults(), movies);
    }

    public static MovieDetail toMovieDetail(TmdbMovieDetailResponse response) {
        List<String> genres = response.genres() != null
                ? response.genres().stream().map(TmdbGenre::name).toList()
                : Collections.emptyList();

        String director = null;
        List<String> cast = Collections.emptyList();
        if (response.credits() != null) {
            if (response.credits().crew() != null) {
                director = response.credits().crew().stream()
                        .filter(c -> "Director".equals(c.job()))
                        .map(TmdbCrewMember::name)
                        .findFirst()
                        .orElse(null);
            }
            if (response.credits().cast() != null) {
                cast = response.credits().cast().stream()
                        .limit(10)
                        .map(TmdbCastMember::name)
                        .toList();
            }
        }

        List<WatchProvider> watchProviders = new ArrayList<>();
        if (response.watchProviders() != null && response.watchProviders().results() != null) {
            response.watchProviders().results().values().forEach(region -> {
                if (region.flatrate() != null) {
                    region.flatrate().forEach(entry ->
                            watchProviders.add(new WatchProvider(entry.providerName(), entry.logoPath()))
                    );
                }
            });
        }

        List<Movie> similarMovies = Collections.emptyList();
        if (response.similar() != null && response.similar().results() != null) {
            similarMovies = response.similar().results().stream()
                    .limit(10)
                    .map(TmdbMapper::toMovie)
                    .toList();
        }

        List<MovieReview> popularReviews = Collections.emptyList();
        int reviewCount = 0;
        if (response.reviews() != null) {
            reviewCount = response.reviews().totalResults();
            if (response.reviews().results() != null) {
                popularReviews = response.reviews().results().stream()
                        .limit(5)
                        .map(TmdbMapper::toMovieReview)
                        .toList();
            }
        }

        return new MovieDetail(
                response.id(),
                response.title(),
                response.overview(),
                response.posterPath(),
                response.backdropPath(),
                response.voteAverage(),
                response.releaseDate(),
                response.tagline(),
                response.runtime(),
                genres,
                director,
                cast,
                watchProviders,
                similarMovies,
                popularReviews,
                reviewCount,
                0,
                0
        );
    }

    public static MovieReview toMovieReview(TmdbReview review) {
        double rating = review.authorDetails() != null && review.authorDetails().rating() != null
                ? review.authorDetails().rating()
                : 0.0;
        return new MovieReview(
                review.id(),
                review.content(),
                review.createdAt(),
                rating,
                review.author(),
                review.content()
        );
    }

    public static Genre toGenre(TmdbGenre genre) {
        return new Genre(genre.id(), genre.name());
    }

    public static Country toCountry(TmdbCountry country) {
        return new Country(country.iso(), country.englishName());
    }

    public static Language toLanguage(TmdbLanguage language) {
        return new Language(language.iso(), language.englishName());
    }
}
