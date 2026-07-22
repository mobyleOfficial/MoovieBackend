"""
Filmow profile scraper using cloudscraper to bypass Cloudflare.
Called by the Kotlin backend as a subprocess.

Based on https://github.com/yanari/filmow_to_letterboxd

Usage:
    python filmow_scraper.py <username> [cookies]

Output:
    JSON to stdout with the scraped profile data.
"""

import json
import re
import sys
import time
import cloudscraper
from bs4 import BeautifulSoup

BASE_URL = "https://filmow.com"
REQUEST_DELAY = 0.5
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


def scrape_profile_page(session, username):
    """Scrapes the profile page and returns display name, stats, and recently watched movies."""
    display_name = username
    recent = []
    stats = {}

    try:
        soup = get_page(session, f"{BASE_URL}/@{username}")

        # Display name
        el = soup.select_one("span[itemprop=name] a") or soup.select_one("span[itemprop=name]")
        if el:
            display_name = el.get_text(strip=True)

        # Stats from profile header (e.g. "4 Já Vi", "0 Comentários")
        for a in soup.select(".profile__stats a"):
            text = a.get_text(strip=True)
            count_match = re.match(r"(\d+)", text)
            if count_match:
                count = int(count_match.group(1))
                if "Vi" in text:
                    stats["watchedCount"] = count

        log(f"Stats: {stats}")

        # Recently watched from .last-seen section
        last_seen = soup.select_one(".last-seen")
        if last_seen:
            items = last_seen.select(".recent-movies-list > div.movie_list_item")
            for item in items:
                mi = item.select_one("div.movie-item")
                if not mi:
                    continue

                parsed = parse_movie_item_from_div(mi, "Assisti Recentemente")
                if not parsed:
                    continue

                # Override user rating from stars above the movie-item
                stars = item.select_one(".user-extras__item[title]")
                if stars:
                    rating_match = re.search(r"Nota:\s*([0-5](?:[.,]5)?)", stars.get("title", ""))
                    if rating_match:
                        parsed["userRating"] = int(float(rating_match.group(1).replace(",", ".")))

                recent.append(parsed)

            log(f"Recently watched: {len(recent)} items")

    except Exception as e:
        log(f"Failed to scrape profile page for @{username}: {e}")

    return display_name, stats, recent


def extract_titles_from_alt(alt_text):
    """Extract localTitle (PT) and originalTitle from alt text like 'Eternos (Eternals)'."""
    orig_match = re.search(r"\(([^)]+)\)$", alt_text)
    if orig_match:
        local_title = alt_text[:orig_match.start()].strip()
        original_title = orig_match.group(1)
        return local_title, original_title
    return alt_text.strip(), None


def parse_movie_item_from_list(item, status):
    """Extract movie data from a li.movie_list_item element (classic layout)."""
    link = item.select_one("a.tip-movie[href]") or item.select_one("a[href]")
    if not link:
        return None

    img = item.select_one("img.lazyload") or item.select_one("img")
    alt_text = img.get("alt", "") if img else ""
    title = link.get("title", "").strip() or alt_text or link.get_text(strip=True)

    local_title, original_title = extract_titles_from_alt(alt_text)
    if original_title:
        title = original_title

    poster_url = None
    if img:
        poster_url = img.get("data-src") or img.get("data-original") or img.get("src")
        if poster_url and "placeholder" in poster_url:
            poster_url = None

    year_match = re.search(r"\((\d{4})\)", alt_text)
    year = year_match.group(1) if year_match else None

    rating_el = item.select_one(".star-rating[title]") or item.select_one("span.star-rating-small[title]")
    user_rating = None
    if rating_el:
        rating_match = re.search(r"Nota:\s*([0-5](?:[.,]5)?)", rating_el.get("title", ""))
        if rating_match:
            user_rating = int(float(rating_match.group(1).replace(",", ".")))

    return {
        "title": title,
        "localTitle": local_title if original_title else None,
        "originalTitle": original_title,
        "year": year,
        "posterUrl": poster_url,
        "voteAverage": 0.0,
        "userRating": user_rating,
        "status": status,
    }


def parse_movie_item_from_div(item, status):
    """Extract movie data from a div.movie-item element (newer layout)."""
    a = item.select_one("a[href][data-movie-pk]")
    if not a:
        return None

    title_el = item.select_one("h3.movie-item__title")
    title = title_el.get_text(strip=True) if title_el else ""

    poster = item.select_one("img.movie-item__poster")
    poster_url = poster.get("src") if poster else None

    # Extract original title from poster alt: "Título PT (Original Title)"
    alt_text = poster.get("alt", "") if poster else ""
    local_title, original_title = extract_titles_from_alt(alt_text)
    if original_title:
        title = original_title

    gr_el = item.select_one("span.movie-item__rating")
    vote_average = 0.0
    if gr_el:
        try:
            vote_average = float(re.sub(r"[^0-9.]", "", gr_el.get_text()))
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

    return {
        "title": title,
        "localTitle": local_title if original_title else None,
        "originalTitle": original_title,
        "year": year,
        "posterUrl": poster_url,
        "voteAverage": vote_average,
        "userRating": user_rating,
        "status": status,
    }


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
                parsed = parse_movie_item_from_list(item, status)
                if parsed:
                    movies.append(parsed)
        else:
            # Newer layout: div.movie-item
            div_items = soup.select("div.movie-item")
            for item in div_items:
                parsed = parse_movie_item_from_div(item, status)
                if parsed:
                    movies.append(parsed)

        count = len(items) or len(soup.select("div.movie-item"))
        log(f"  page {page_num}/{total_pages} -> {count} items")

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

            # Skip series
            is_series = bool(re.search(r"-(temporada|season)-|\d+a-temporada", item_href))
            if is_series:
                log(f"    skipping series: {item_href}")
                continue

            parsed = parse_movie_item_from_div(item, "Lista")
            if parsed:
                parsed["filmowId"] = a.get("data-movie-pk", "")
                movies.append(parsed)

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

    display_name, stats, recently_watched = scrape_profile_page(session, username)

    watched = scrape_section(session, username, "filmes", "ja-vi", errors)
    watchlist = scrape_section(session, username, "filmes", "quero-ver", errors)
    favorites = scrape_section(session, username, "filmes", "favoritos", errors)
    user_lists = scrape_lists(session, username, errors)

    return {
        "username": username,
        "displayName": display_name,
        "watchedCount": stats.get("watchedCount", 0),
        "recentlyWatched": recently_watched,
        "watched": watched,
        "watchlist": watchlist,
        "favorites": favorites,
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
