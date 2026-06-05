package com.moovie.backend.data.remote.tmdb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class TmdbResponses {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbMovieResponse(
            int id,
            String title,
            String overview,
            @JsonProperty("poster_path") String posterPath,
            @JsonProperty("backdrop_path") String backdropPath,
            @JsonProperty("vote_average") double voteAverage,
            @JsonProperty("release_date") String releaseDate
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbMovieListResponse(
            int page,
            @JsonProperty("total_pages") int totalPages,
            @JsonProperty("total_results") int totalResults,
            List<TmdbMovieResponse> results
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbMovieDetailResponse(
            int id,
            String title,
            String overview,
            @JsonProperty("poster_path") String posterPath,
            @JsonProperty("backdrop_path") String backdropPath,
            @JsonProperty("vote_average") double voteAverage,
            @JsonProperty("release_date") String releaseDate,
            String tagline,
            Integer runtime,
            List<TmdbGenre> genres,
            TmdbCredits credits,
            @JsonProperty("watch/providers") TmdbWatchProvidersWrapper watchProviders,
            TmdbMovieListResponse similar,
            TmdbReviewListResponse reviews
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbCredits(
            List<TmdbCrewMember> crew,
            List<TmdbCastMember> cast
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbCrewMember(
            String name,
            String job
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbCastMember(
            String name,
            String character
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbWatchProvidersWrapper(
            Map<String, TmdbWatchProviderRegion> results
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbWatchProviderRegion(
            List<TmdbWatchProviderEntry> flatrate
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbWatchProviderEntry(
            @JsonProperty("provider_name") String providerName,
            @JsonProperty("logo_path") String logoPath
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbReviewListResponse(
            int page,
            @JsonProperty("total_pages") int totalPages,
            @JsonProperty("total_results") int totalResults,
            List<TmdbReview> results
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbReview(
            String id,
            String author,
            String content,
            @JsonProperty("created_at") String createdAt,
            @JsonProperty("author_details") TmdbAuthorDetails authorDetails
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbAuthorDetails(
            Double rating
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbGenre(
            int id,
            String name
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbGenreListResponse(
            List<TmdbGenre> genres
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbCountry(
            @JsonProperty("iso_3166_1") String iso,
            @JsonProperty("english_name") String englishName
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmdbLanguage(
            @JsonProperty("iso_639_1") String iso,
            @JsonProperty("english_name") String englishName
    ) {
    }
}
