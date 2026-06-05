package com.moovie.backend.data.remote.tmdb;

import com.moovie.backend.data.remote.tmdb.model.TmdbResponses.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class TmdbDataSourceImpl implements TmdbDataSource {

    private static final Logger log = LoggerFactory.getLogger(TmdbDataSourceImpl.class);
    private final RestClient restClient;

    public TmdbDataSourceImpl(@Value("${tmdb.api-key}") String apiKey) {
        log.info("TMDB API key loaded: {}", apiKey.isEmpty() ? "EMPTY" : apiKey.substring(0, 10) + "...");
        this.restClient = RestClient.builder()
                .baseUrl("https://api.themoviedb.org/3/")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Override
    public TmdbMovieListResponse getTrendingMovies(int page) {
        return restClient.get()
                .uri("trending/movie/week?page={page}", page)
                .retrieve()
                .body(TmdbMovieListResponse.class);
    }

    @Override
    public TmdbMovieDetailResponse getMovieDetail(int movieId) {
        return restClient.get()
                .uri("movie/{id}?append_to_response=credits,watch/providers,similar,reviews", movieId)
                .retrieve()
                .body(TmdbMovieDetailResponse.class);
    }

    @Override
    public TmdbMovieListResponse searchMovies(String query, int page) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("search/movie")
                        .queryParam("query", query)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .body(TmdbMovieListResponse.class);
    }

    @Override
    public TmdbMovieListResponse discoverMovies(int page, String year, String releaseDateGte,
                                                  String releaseDateLte, String sortBy, String withGenres,
                                                  String withOriginalLanguage, String withOriginCountry,
                                                  String voteCountGte) {
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("discover/movie").queryParam("page", page);
                    if (year != null && !year.isEmpty()) uriBuilder.queryParam("year", year);
                    if (releaseDateGte != null && !releaseDateGte.isEmpty())
                        uriBuilder.queryParam("release_date.gte", releaseDateGte);
                    if (releaseDateLte != null && !releaseDateLte.isEmpty())
                        uriBuilder.queryParam("release_date.lte", releaseDateLte);
                    if (sortBy != null && !sortBy.isEmpty()) uriBuilder.queryParam("sort_by", sortBy);
                    if (withGenres != null && !withGenres.isEmpty())
                        uriBuilder.queryParam("with_genres", withGenres);
                    if (withOriginalLanguage != null && !withOriginalLanguage.isEmpty())
                        uriBuilder.queryParam("with_original_language", withOriginalLanguage);
                    if (withOriginCountry != null && !withOriginCountry.isEmpty())
                        uriBuilder.queryParam("with_origin_country", withOriginCountry);
                    if (voteCountGte != null && !voteCountGte.isEmpty())
                        uriBuilder.queryParam("vote_count.gte", voteCountGte);
                    return uriBuilder.build();
                })
                .retrieve()
                .body(TmdbMovieListResponse.class);
    }

    @Override
    public TmdbGenreListResponse getGenres() {
        return restClient.get()
                .uri("genre/movie/list")
                .retrieve()
                .body(TmdbGenreListResponse.class);
    }

    @Override
    public List<TmdbCountry> getCountries() {
        return restClient.get()
                .uri("configuration/countries")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    @Override
    public List<TmdbLanguage> getLanguages() {
        return restClient.get()
                .uri("configuration/languages")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    @Override
    public TmdbReviewListResponse getMovieReviews(int movieId, int page) {
        return restClient.get()
                .uri("movie/{id}/reviews?page={page}", movieId, page)
                .retrieve()
                .body(TmdbReviewListResponse.class);
    }
}
