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

        global_rating_el = soup.select_one(".movie-rating-average") or soup.select_one(".movie__average")
        global_rating = None
        if global_rating_el:
            try:
                global_rating = float(global_rating_el.get_text(strip=True).replace(",", "."))
            except ValueError:
                pass

        return {
            "title": title,
            "director": ", ".join(directors) if directors else None,
            "year": year,
            "posterUrl": poster_url,
            "globalRating": global_rating,
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
                        "year": detail["year"],
                        "filmowUrl": f"{BASE_URL}{href}",
                        "posterUrl": detail["posterUrl"],
                        "globalRating": detail["globalRating"],
                        "userRating": detail["userRating"],
                        "status": status,
                        "director": detail["director"],
                    }
                    movies.append(movie)
                    log(f"  + {detail['title']} ({detail.get('year', '?')})")
                else:
                    # Fallback: basic info from list
                    alt_text = item.select_one("img")
                    title = alt_text.get("alt", "") if alt_text else link.get_text(strip=True)
                    movies.append({
                        "filmowId": pk,
                        "title": title,
                        "year": None,
                        "filmowUrl": f"{BASE_URL}{href}",
                        "posterUrl": None,
                        "globalRating": None,
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

                movies.append({
                    "filmowId": pk,
                    "title": title,
                    "year": year,
                    "filmowUrl": f"{BASE_URL}{href}",
                    "posterUrl": poster_url,
                    "globalRating": global_rating,
                    "userRating": user_rating,
                    "status": status,
                    "director": None,
                })

        log(f"  page {page_num}/{total_pages} -> {len(items) or len(soup.select('div.movie-item'))} items")

    return movies


def scrape_lists(session, username, errors):
    """Scrape user lists from /listas/usuario/{username}/"""
    url = f"{BASE_URL}/listas/usuario/{username}/"
    lists = []

    try:
        soup = get_page(session, url)
    except Exception as e:
        errors.append(f"Lists page failed: {e}")
        return lists

    for card in soup.select("div.list-card"):
        link = card.select_one("a.list-card__covers[href]") or card.select_one("a[href*='/listas/']")
        if not link:
            continue

        href = link.get("href", "")
        # Extract list ID from URL like /listas/teste-2-l212586/
        id_match = re.search(r"-l(\d+)/?$", href)
        filmow_id = id_match.group(1) if id_match else ""

        title_el = card.select_one("a.list-card__title")
        title = title_el.get_text(strip=True) if title_el else ""

        cover_el = card.select_one("img.list-card__cover")
        cover_url = cover_el.get("src") if cover_el else None

        # Item count is usually in a div inside the card
        count = None
        count_el = card.select_one(".list-card__count, .list-card__stats")
        if count_el:
            count_match = re.search(r"(\d+)", count_el.get_text())
            if count_match:
                count = int(count_match.group(1))

        if title:
            lists.append({
                "filmowId": filmow_id,
                "title": title,
                "filmowUrl": f"{BASE_URL}{href}",
                "coverUrl": cover_url,
                "itemCount": count,
            })
            log(f"  list: {title} (id={filmow_id})")

    log(f"Lists: found {len(lists)}")
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
