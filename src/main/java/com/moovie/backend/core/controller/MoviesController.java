package com.moovie.backend.core.controller;

import com.moovie.backend.domain.model.*;
import com.moovie.backend.domain.usecase.movies.*;
import com.moovie.backend.domain.model.MovieListing;
import com.moovie.backend.domain.model.MovieListListing;
import com.moovie.backend.domain.model.MovieReviewListing;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/movies")
public class MoviesController {

    private final GetTrendingMovies getTrendingMovies;
    private final GetMovieDetail getMovieDetail;
    private final SearchMovies searchMovies;
    private final DiscoverMovies discoverMovies;
    private final GetGenres getGenres;
    private final GetCountries getCountries;
    private final GetLanguages getLanguages;
    private final GetMovieReviews getMovieReviews;
    private final GetUserFavoriteMovies getUserFavoriteMovies;
    private final GetUserWatchList getUserWatchList;
    private final GetMovieLists getMovieLists;
    private final GetUserMovieLists getUserMovieLists;
    private final GetMovieListDetail getMovieListDetail;
    private final GetFeaturedLists getFeaturedLists;

    public MoviesController(GetTrendingMovies getTrendingMovies,
                            GetMovieDetail getMovieDetail,
                            SearchMovies searchMovies,
                            DiscoverMovies discoverMovies,
                            GetGenres getGenres,
                            GetCountries getCountries,
                            GetLanguages getLanguages,
                            GetMovieReviews getMovieReviews,
                            GetUserFavoriteMovies getUserFavoriteMovies,
                            GetUserWatchList getUserWatchList,
                            GetMovieLists getMovieLists,
                            GetUserMovieLists getUserMovieLists,
                            GetMovieListDetail getMovieListDetail,
                            GetFeaturedLists getFeaturedLists) {
        this.getTrendingMovies = getTrendingMovies;
        this.getMovieDetail = getMovieDetail;
        this.searchMovies = searchMovies;
        this.discoverMovies = discoverMovies;
        this.getGenres = getGenres;
        this.getCountries = getCountries;
        this.getLanguages = getLanguages;
        this.getMovieReviews = getMovieReviews;
        this.getUserFavoriteMovies = getUserFavoriteMovies;
        this.getUserWatchList = getUserWatchList;
        this.getMovieLists = getMovieLists;
        this.getUserMovieLists = getUserMovieLists;
        this.getMovieListDetail = getMovieListDetail;
        this.getFeaturedLists = getFeaturedLists;
    }

    @GetMapping("/trending")
    public MovieListing trending(@RequestParam(defaultValue = "1") int page) {
        return getTrendingMovies.invoke(page);
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(required = false) String query,
                                    @RequestParam(defaultValue = "1") int page) {
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().body("Missing required parameter: query");
        }
        return ResponseEntity.ok(searchMovies.invoke(query, page));
    }

    @GetMapping("/discover")
    public MovieListing discover(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(required = false) String year,
                                  @RequestParam(name = "release_date_gte", required = false) String releaseDateGte,
                                  @RequestParam(name = "release_date_lte", required = false) String releaseDateLte,
                                  @RequestParam(name = "sort_by", required = false) String sortBy,
                                  @RequestParam(name = "with_genres", required = false) String withGenres,
                                  @RequestParam(name = "with_original_language", required = false) String withOriginalLanguage,
                                  @RequestParam(name = "with_origin_country", required = false) String withOriginCountry,
                                  @RequestParam(name = "vote_count_gte", required = false) String voteCountGte) {
        return discoverMovies.invoke(page, year, releaseDateGte, releaseDateLte, sortBy,
                withGenres, withOriginalLanguage, withOriginCountry, voteCountGte);
    }

    @GetMapping("/genres")
    public List<Genre> genres() {
        return getGenres.invoke();
    }

    @GetMapping("/countries")
    public List<Country> countries() {
        return getCountries.invoke();
    }

    @GetMapping("/languages")
    public List<Language> languages() {
        return getLanguages.invoke();
    }

    @GetMapping("/{id:\\d+}")
    public MovieDetail movieDetail(@PathVariable int id) {
        return getMovieDetail.invoke(id);
    }

    @GetMapping("/{id:\\d+}/reviews")
    public MovieReviewListing movieReviews(@PathVariable int id,
                                            @RequestParam(defaultValue = "1") int page) {
        return getMovieReviews.invoke(id, page);
    }

    @GetMapping("/favorites/{userId}")
    public MovieListing favorites(@PathVariable String userId,
                                   @RequestParam(defaultValue = "1") int page) {
        return getUserFavoriteMovies.invoke(userId, page);
    }

    @GetMapping("/watchlist/{userId}")
    public MovieListing watchlist(@PathVariable String userId,
                                   @RequestParam(defaultValue = "1") int page) {
        return getUserWatchList.invoke(userId, page);
    }

    @GetMapping("/lists")
    public MovieListListing lists(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(required = false) String userId) {
        return getMovieLists.invoke(page, userId);
    }

    @GetMapping("/lists/user")
    public MovieListListing userLists(@RequestParam(defaultValue = "1") int page) {
        return getUserMovieLists.invoke(page);
    }

    @GetMapping("/lists/featured")
    public MovieListListing featuredLists(@RequestParam(defaultValue = "1") int page) {
        return getFeaturedLists.invoke(page);
    }

    @GetMapping("/lists/{id}")
    public MovieListDetail listDetail(@PathVariable int id,
                                       @RequestParam(defaultValue = "1") int page) {
        return getMovieListDetail.invoke(id, page);
    }
}
