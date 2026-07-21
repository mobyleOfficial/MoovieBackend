"""
Filmow profile scraper using cloudscraper to bypass Cloudflare.
Called by the Kotlin backend as a subprocess.

Usage:
    python filmow_scraper.py <username> [cookies]

Output:
    JSON to stdout with the scraped profile data.
"""

import json
import re
import sys
import time
from urllib.parse import urljoin

import cloudscraper
from bs4 import BeautifulSoup

BASE_URL = "https://filmow.com"
REQUEST_DELAY = 1.0
MAX_RETRIES = 5


def create_session(cookies_str=""):
    session = cloudscraper.create_scraper()
    if cookies_str:
        for pair in cookies_str.split(";"):
            pair = pair.strip()
            if "=" in pair:
                key, value = pair.split("=", 1)
                session.cookies.set(key.strip(), value.strip())
    return session


def get_page(session, url):
    last_response = None
    for attempt in range(MAX_RETRIES):
        time.sleep(REQUEST_DELAY)
        last_response = session.get(url, timeout=30)
        if last_response.status_code == 429:
            retry_after = last_response.headers.get("Retry-After")
            wait = (
                float(retry_after)
                if retry_after and retry_after.isdigit()
                else min(60, 5 * (2 ** attempt))
            )
            log(f"Rate limited, waiting {wait:.0f}s...")
            time.sleep(wait)
            continue
        last_response.raise_for_status()
        return BeautifulSoup(last_response.text, "html.parser")
    last_response.raise_for_status()


def get_last_page(soup):
    pages = []
    for link in soup.select(".pagination a[href]"):
        match = re.search(r"pagina=(\d+)", link.get("href", ""))
        if match:
            pages.append(int(match.group(1)))
    return max(pages, default=1)


def get_display_name(session, username):
    try:
        soup = get_page(session, f"{BASE_URL}/@{username}")
        el = soup.select_one("span[itemprop=name] a") or soup.select_one("span[itemprop=name]")
        if el:
            return el.get_text(strip=True)
    except Exception as e:
        log(f"Failed to get display name: {e}")
    return username


def get_movie_detail(session, path, rating):
    try:
        soup = get_page(session, urljoin(BASE_URL, path))
        title_el = soup.select_one(".movie__title")
        if not title_el:
            return None

        title = title_el.get_text(strip=True)

        # Try to get original title
        orig_el = soup.select_one("div.col-12.col-xl-7 > div.d-flex.gap-2 span i")
        if orig_el:
            title = orig_el.get_text(strip=True)

        directors = [
            a.get_text(strip=True)
            for a in soup.select(".movie__mobile-directors a")
        ]

        year = None
        year_el = soup.select_one(".movie__year")
        if year_el:
            year_match = re.search(r"\d{4}", year_el.get_text())
            if year_match:
                year = year_match.group()

        poster_el = soup.select_one("img.movie__poster") or soup.select_one(".movie__poster img")
        poster_url = poster_el.get("src") if poster_el else None

        # Extract overview, voteAverage, releaseDate, imdbId from JSON-LD
        overview = None
        vote_average = 0.0
        release_date = None
        backdrop_url = None
        imdb_id = None

        for script in soup.select('script[type="application/ld+json"]'):
            try:
                data = json.loads(script.string)
                if data.get("@type") == "Movie":
                    if not overview and data.get("description"):
                        overview = data["description"].strip()
                    agg = data.get("aggregateRating")
                    if agg:
                        try:
                            vote_average = float(agg.get("ratingValue", 0))
                        except (ValueError, TypeError):
                            pass
                    if not backdrop_url and data.get("image"):
                        backdrop_url = data["image"]
                    # datePublished (real release date)
                    if not release_date and data.get("datePublished"):
                        release_date = data["datePublished"]
                    # IMDB ID from sameAs
                    same_as = data.get("sameAs") or []
                    for link in same_as:
                        imdb_match = re.search(r"imdb\.com/title/(tt\d+)", link)
                        if imdb_match:
                            imdb_id = imdb_match.group(1)
            except (json.JSONDecodeError, AttributeError):
                pass

        # Fallback: overview from page content
        if not overview:
            desc_el = soup.select_one("div[itemprop=description]") or soup.select_one("p.description-text")
            if desc_el:
                overview = desc_el.get_text(strip=True)

        # Release date fallback from year
        if not release_date and year:
            release_date = f"{year}-01-01"

        # OG image as backdrop fallback
        if not backdrop_url:
            og_img = soup.select_one('meta[property="og:image"]')
            if og_img:
                backdrop_url = og_img.get("content")

        return {
            "title": title,
            "overview": overview,
            "posterUrl": poster_url,
            "backdropUrl": backdrop_url,
            "voteAverage": vote_average,
            "releaseDate": release_date,
            "imdbId": imdb_id,
            "director": ", ".join(directors) if directors else None,
            "year": year,
            "userRating": rating,
        }
    except Exception as e:
        log(f"Failed to get movie detail for {path}: {e}")
        return None


def scrape_section(session, username, content_type, status_key, errors):
    """
    Scrape a section (e.g. filmes/ja-vi, series/quero-ver).
    content_type: "filmes" or "series"
    status_key: "ja-vi", "quero-ver", or "favoritos"
    """
    status_labels = {
        "ja-vi": "Já Vi",
        "quero-ver": "Quero Ver",
        "favoritos": "Favorito",
    }
    status = status_labels.get(status_key, status_key)
    base_path = f"/usuario/{username}/{content_type}/{status_key}/"
    url = f"{BASE_URL}{base_path}"

    movies = []

    try:
        first_page = get_page(session, url)
    except Exception as e:
        errors.append(f"{base_path} failed: {e}")
        return movies

    # Check if page has content
    movie_list = first_page.select_one("#movies-list")
    if not movie_list:
        # Try newer layout
        items = first_page.select("div.movie-item")
        if not items:
            return movies

    total_pages = get_last_page(first_page)
    log(f"{base_path} -> {total_pages} pages")

    for page_num in range(1, total_pages + 1):
        soup = first_page if page_num == 1 else None
        if soup is None:
            try:
                soup = get_page(session, f"{url}?pagina={page_num}")
            except Exception as e:
                errors.append(f"{base_path} page {page_num} failed: {e}")
                continue

        # Classic layout: li.movie_list_item
        items = soup.select("li.movie_list_item")
        if items:
            for item in items:
                link = item.select_one("a.tip-movie[href]") or item.select_one("a[href]")
                if not link:
                    continue

                pk = item.get("data-movie-pk", "")
                href = link.get("href", "")

                # Rating
                rating_el = item.select_one(".star-rating[title]") or item.select_one("span.star-rating-small[title]")
                user_rating = None
                if rating_el:
                    rating_match = re.search(r"Nota:\s*([0-5](?:[.,]5)?)", rating_el.get("title", ""))
                    if rating_match:
                        user_rating = int(float(rating_match.group(1).replace(",", ".")))

                # Get full detail from movie page
                detail = get_movie_detail(session, href, user_rating)
                if detail:
                    movie = {
                        "filmowId": pk,
                        "title": detail["title"],
                        "overview": detail.get("overview"),
                        "year": detail["year"],
                        "filmowUrl": f"{BASE_URL}{href}",
                        "posterUrl": detail["posterUrl"],
                        "backdropUrl": detail.get("backdropUrl"),
                        "voteAverage": detail.get("voteAverage", 0.0),
                        "releaseDate": detail.get("releaseDate"),
                        "imdbId": detail.get("imdbId"),
                        "userRating": detail["userRating"],
                        "status": status,
                        "director": detail["director"],
                    }
                    movies.append(movie)
                    log(f"  + {detail['title']} ({detail.get('year', '?')}) imdb={detail.get('imdbId')}")
                else:
                    # Fallback: basic info from list
                    alt_text = item.select_one("img")
                    title = alt_text.get("alt", "") if alt_text else link.get_text(strip=True)
                    movies.append({
                        "filmowId": pk,
                        "title": title,
                        "overview": None,
                        "year": None,
                        "filmowUrl": f"{BASE_URL}{href}",
                        "posterUrl": None,
                        "backdropUrl": None,
                        "voteAverage": 0.0,
                        "releaseDate": None,
                        "imdbId": None,
                        "userRating": user_rating,
                        "status": status,
                        "director": None,
                    })
        else:
            # Newer layout: div.movie-item
            div_items = soup.select("div.movie-item")
            for item in div_items:
                a = item.select_one("a[href][data-movie-pk]")
                if not a:
                    continue

                pk = a.get("data-movie-pk", "")
                href = a.get("href", "")
                title_el = item.select_one("h3.movie-item__title")
                title = title_el.get_text(strip=True) if title_el else ""
                poster = item.select_one("img.movie-item__poster")
                poster_url = poster.get("src") if poster else None

                gr_el = item.select_one("span.movie-item__rating")
                global_rating = None
                if gr_el:
                    try:
                        global_rating = float(re.sub(r"[^0-9.]", "", gr_el.get_text()))
                    except ValueError:
                        pass

                ur_el = item.select_one("span.movie-item__user-rating")
                user_rating = None
                if ur_el:
                    try:
                        user_rating = int(re.sub(r"[^0-9]", "", ur_el.get_text()))
                    except ValueError:
                        pass

                year_match = re.search(r"\((\d{4})\)", title)
                year = year_match.group(1) if year_match else None

                # Fetch detail page for full info
                detail = get_movie_detail(session, href, user_rating)
                if detail:
                    movies.append({
                        "filmowId": pk,
                        "title": detail["title"],
                        "overview": detail.get("overview"),
                        "year": detail.get("year") or year,
                        "filmowUrl": f"{BASE_URL}{href}",
                        "posterUrl": detail.get("posterUrl") or poster_url,
                        "backdropUrl": detail.get("backdropUrl"),
                        "voteAverage": detail.get("voteAverage", 0.0),
                        "releaseDate": detail.get("releaseDate"),
                        "imdbId": detail.get("imdbId"),
                        "userRating": detail.get("userRating"),
                        "status": status,
                        "director": detail.get("director"),
                    })
                    log(f"  + {detail['title']} ({detail.get('year', '?')}) imdb={detail.get('imdbId')}")
                else:
                    movies.append({
                        "filmowId": pk,
                        "title": title,
                        "overview": None,
                        "year": year,
                        "filmowUrl": f"{BASE_URL}{href}",
                        "posterUrl": poster_url,
                        "backdropUrl": None,
                        "voteAverage": global_rating or 0.0,
                        "releaseDate": f"{year}-01-01" if year else None,
                        "imdbId": None,
                        "userRating": user_rating,
                        "status": status,
                        "director": None,
                    })

        log(f"  page {page_num}/{total_pages} -> {len(items) or len(soup.select('div.movie-item'))} items")

    return movies


def scrape_list_detail(session, href, errors):
    """Scrape a single list detail page. Returns description and list of movies (non-movie items ignored)."""
    url = f"{BASE_URL}{href}"
    description = None
    movies = []

    try:
        soup = get_page(session, url)
    except Exception as e:
        errors.append(f"List detail {href} failed: {e}")
        return description, movies

    # Description from meta tag (Filmow doesn't have a dedicated description field in the UI)
    meta_desc = soup.select_one('meta[name=description]')
    if meta_desc:
        description = meta_desc.get("content", "").strip() or None

    # Collect all movie items across all pages
    total_pages = get_last_page(soup)
    log(f"  list detail {href} -> {total_pages} pages")

    for page_num in range(1, total_pages + 1):
        page_soup = soup if page_num == 1 else None
        if page_soup is None:
            try:
                page_soup = get_page(session, f"{url}?pagina={page_num}")
            except Exception as e:
                errors.append(f"List {href} page {page_num} failed: {e}")
                continue

        items = page_soup.select("div.movie-item")
        for item in items:
            a = item.select_one("a[href][data-movie-pk]")
            if not a:
                continue

            item_href = a.get("href", "")

            # Check if it's a movie by fetching the detail page breadcrumb
            # We use the URL pattern as a fast heuristic first:
            # Series URLs typically contain "-temporada-" or "-season-"
            is_series = bool(re.search(r"-(temporada|season)-|\d+a-temporada", item_href))

            if is_series:
                log(f"    skipping series: {item_href}")
                continue

            pk = a.get("data-movie-pk", "")
            title_el = item.select_one("h3.movie-item__title")
            title = title_el.get_text(strip=True) if title_el else ""
            poster = item.select_one("img.movie-item__poster")
            poster_url = poster.get("src") if poster else None

            gr_el = item.select_one("span.movie-item__rating")
            global_rating = None
            if gr_el:
                try:
                    global_rating = float(re.sub(r"[^0-9.]", "", gr_el.get_text()))
                except ValueError:
                    pass

            year_match = re.search(r"\((\d{4})\)", title)
            year = year_match.group(1) if year_match else None

            movies.append({
                "filmowId": pk,
                "title": title,
                "year": year,
                "filmowUrl": f"{BASE_URL}{item_href}",
                "posterUrl": poster_url,
                "globalRating": global_rating,
                "director": None,
            })

        log(f"    page {page_num}/{total_pages} -> {len(items)} items")

    return description, movies


def scrape_lists(session, username, errors):
    """Scrape user lists from /listas/usuario/{username}/ and each list's detail."""
    url = f"{BASE_URL}/listas/usuario/{username}/"
    lists = []

    try:
        soup = get_page(session, url)
    except Exception as e:
        errors.append(f"Lists page failed: {e}")
        return lists

    list_refs = []
    for card in soup.select("div.list-card"):
        link = card.select_one("a.list-card__covers[href]") or card.select_one("a[href*='/listas/']")
        if not link:
            continue

        href = link.get("href", "")
        id_match = re.search(r"-l(\d+)/?$", href)
        filmow_id = id_match.group(1) if id_match else ""

        title_el = card.select_one("a.list-card__title")
        title = title_el.get_text(strip=True) if title_el else ""

        cover_el = card.select_one("img.list-card__cover")
        cover_url = cover_el.get("src") if cover_el else None

        if title:
            list_refs.append({
                "filmowId": filmow_id,
                "title": title,
                "href": href,
                "coverUrl": cover_url,
            })

    log(f"Lists: found {len(list_refs)}, fetching details...")

    for ref in list_refs:
        description, movies = scrape_list_detail(session, ref["href"], errors)
        lists.append({
            "filmowId": ref["filmowId"],
            "title": ref["title"],
            "description": description,
            "filmowUrl": f"{BASE_URL}{ref['href']}",
            "coverUrl": ref["coverUrl"],
            "movies": movies,
        })
        log(f"  list '{ref['title']}': {len(movies)} movies")

    return lists


def scrape_profile(username, cookies_str=""):
    session = create_session(cookies_str)
    errors = []

    display_name = get_display_name(session, username)

    watched = scrape_section(session, username, "filmes", "ja-vi", errors)
    watchlist = scrape_section(session, username, "filmes", "quero-ver", errors)
    favorites = scrape_section(session, username, "filmes", "favoritos", errors)
    watched_series = scrape_section(session, username, "series", "ja-vi", errors)
    watchlist_series = scrape_section(session, username, "series", "quero-ver", errors)
    user_lists = scrape_lists(session, username, errors)

    return {
        "username": username,
        "displayName": display_name,
        "watched": watched,
        "watchlist": watchlist,
        "favorites": favorites,
        "watchedSeries": watched_series,
        "watchlistSeries": watchlist_series,
        "lists": user_lists,
        "errors": errors,
    }


def log(msg):
    print(msg, file=sys.stderr)


def main():
    # Force UTF-8 output on Windows
    if sys.platform == "win32":
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")

    if len(sys.argv) < 2:
        print(json.dumps({"error": "Usage: filmow_scraper.py <username> [cookies]"}))
        sys.exit(1)

    username = sys.argv[1]
    cookies = sys.argv[2] if len(sys.argv) > 2 else ""

    try:
        result = scrape_profile(username, cookies)
        print(json.dumps(result, ensure_ascii=False))
    except Exception as e:
        print(json.dumps({"error": str(e), "username": username}))
        sys.exit(1)


if __name__ == "__main__":
    main()
