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
REQUEST_DELAY = 2.0
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
        t0 = time.time()
        log(f"[HTTP] GET {url} (attempt {attempt + 1}/{MAX_RETRIES})...")
        last_response = session.get(url, timeout=30)
        elapsed = time.time() - t0
        log(f"[HTTP] {last_response.status_code} in {elapsed:.1f}s ({len(last_response.text)} chars)")
        if last_response.status_code == 429:
            retry_after = last_response.headers.get("Retry-After")
            wait = (
                float(retry_after)
                if retry_after and retry_after.isdigit()
                else min(60, 5 * (2 ** attempt))
            )
            log(f"[HTTP] Rate limited, waiting {wait:.0f}s...")
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
            items = last_seen.select(".recent-movies-list div.movie_list_item")
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
                    rating_match = re.search(r"Nota:\s*([0-5](?:[.,]\d)?)", stars.get("title", ""))
                    if rating_match:
                        parsed["userRating"] = float(rating_match.group(1).replace(",", "."))

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

    filmow_id = item.get("data-movie-pk", "") or link.get("data-movie-pk", "")

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
        rating_match = re.search(r"Nota:\s*([0-5](?:[.,]\d)?)", rating_el.get("title", ""))
        if rating_match:
            user_rating = float(rating_match.group(1).replace(",", "."))

    vote_average = 0.0
    avg_el = item.select_one("span.movie-rating-average")
    if avg_el:
        try:
            vote_average = float(avg_el.get_text(strip=True))
        except ValueError:
            pass

    return {
        "filmowId": filmow_id or None,
        "title": title,
        "localTitle": local_title if original_title else None,
        "originalTitle": original_title,
        "year": year,
        "posterUrl": poster_url,
        "voteAverage": vote_average,
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
            user_rating = float(re.sub(r"[^0-9.]", "", ur_el.get_text()))
        except ValueError:
            pass

    year_match = re.search(r"\((\d{4})\)", title)
    year = year_match.group(1) if year_match else None

    return {
        "filmowId": a.get("data-movie-pk", "") or None,
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

    # Collect all movie items across all pages.
    # Lists use two pagination styles:
    #   1. Classic .pagination with numbered links (handled by get_last_page)
    #   2. "Carregar mais" button with data-next-page (infinite scroll)
    total_pages = get_last_page(soup)

    seen_movie_ids = set()

    if total_pages > 1:
        # Classic pagination — iterate known page range
        log(f"  list detail {href} -> {total_pages} pages (classic pagination)")
        for page_num in range(1, total_pages + 1):
            page_soup = soup if page_num == 1 else None
            if page_soup is None:
                try:
                    page_soup = get_page(session, f"{url}?pagina={page_num}")
                except Exception as e:
                    errors.append(f"List {href} page {page_num} failed: {e}")
                    continue
            _collect_list_movies(page_soup, movies, seen_movie_ids, page_num, total_pages)
    else:
        # "Load more" pagination — follow data-next-page until exhausted
        log(f"  list detail {href} -> load-more pagination")
        max_pages = 50  # safety limit
        page_num = 1
        page_soup = soup
        seen_movie_ids = set()
        while page_soup and page_num <= max_pages:
            new_count = _collect_list_movies(page_soup, movies, seen_movie_ids, page_num, "?")
            # Stop if page yielded no new movies (server returning same page)
            if new_count == 0:
                log(f"  list detail {href} -> page {page_num} added 0 new movies, stopping")
                break
            load_more = page_soup.select_one("a.btn-lists-infinite-scroll[data-next-page]")
            if not load_more or "disabled" in load_more.get("class", []):
                break
            next_page = load_more.get("data-next-page")
            if not next_page:
                break
            page_num += 1
            try:
                page_soup = get_page(session, f"{url}?pagina={next_page}")
            except Exception as e:
                errors.append(f"List {href} page {next_page} failed: {e}")
                break

    return description, movies


def _collect_list_movies(soup, movies, seen_ids, page_num, total_label):
    """Extract movie items from a single list page. Returns count of new (non-duplicate) movies added."""
    items = soup.select("div.movie-item")
    new_count = 0
    for item in items:
        a = item.select_one("a[href][data-movie-pk]")
        if not a:
            continue

        movie_pk = a.get("data-movie-pk", "")
        if movie_pk in seen_ids:
            continue

        item_href = a.get("href", "")

        # Skip series
        is_series = bool(re.search(r"-(temporada|season)-|\d+a-temporada", item_href))
        if is_series:
            log(f"    skipping series: {item_href}")
            continue

        parsed = parse_movie_item_from_div(item, "Lista")
        if parsed:
            seen_ids.add(movie_pk)
            movies.append(parsed)
            new_count += 1

    log(f"    page {page_num}/{total_label} -> {len(items)} items ({new_count} new)")
    return new_count


def _collect_list_cards(soup):
    """Extract list card references from a lists index page."""
    refs = []
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
            refs.append({
                "filmowId": filmow_id,
                "title": title,
                "href": href,
                "coverUrl": cover_url,
            })
    return refs


def scrape_lists(session, username, errors):
    """Scrape user lists from /listas/usuario/{username}/ and each list's detail."""
    url = f"{BASE_URL}/listas/usuario/{username}/"
    lists = []

    try:
        soup = get_page(session, url)
    except Exception as e:
        errors.append(f"Lists page failed: {e}")
        return lists

    # Collect list cards across all index pages.
    # Uses classic pagination or "load more" button, same as list detail.
    list_refs = []
    seen_list_ids = set()
    total_pages = get_last_page(soup)

    def _extend_list_refs(page_soup):
        """Add new list cards, skip duplicates. Returns count of new cards."""
        cards = _collect_list_cards(page_soup)
        new_count = 0
        for card in cards:
            fid = card["filmowId"] or card["title"]
            if fid not in seen_list_ids:
                seen_list_ids.add(fid)
                list_refs.append(card)
                new_count += 1
        return new_count

    if total_pages > 1:
        log(f"Lists index -> {total_pages} pages (classic pagination)")
        for page_num in range(1, total_pages + 1):
            page_soup = soup if page_num == 1 else None
            if page_soup is None:
                try:
                    page_soup = get_page(session, f"{url}?pagina={page_num}")
                except Exception as e:
                    errors.append(f"Lists index page {page_num} failed: {e}")
                    continue
            _extend_list_refs(page_soup)
            log(f"  lists index page {page_num}/{total_pages} -> {len(list_refs)} lists so far")
    else:
        max_pages = 50  # safety limit
        page_soup = soup
        page_num = 1
        while page_soup and page_num <= max_pages:
            new_count = _extend_list_refs(page_soup)
            log(f"  lists index page {page_num} -> {len(list_refs)} lists so far ({new_count} new)")
            if new_count == 0:
                log(f"  lists index page {page_num} added 0 new lists, stopping")
                break
            load_more = page_soup.select_one("a.btn-lists-infinite-scroll[data-next-page]")
            if not load_more or "disabled" in load_more.get("class", []):
                break
            next_page = load_more.get("data-next-page")
            if not next_page:
                break
            page_num += 1
            try:
                page_soup = get_page(session, f"{url}?pagina={next_page}")
            except Exception as e:
                errors.append(f"Lists index page {next_page} failed: {e}")
                break

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
    t_start = time.time()

    log(f"[PHASE] scrape_profile_page...")
    display_name, stats, recently_watched = scrape_profile_page(session, username)
    log(f"[PHASE] scrape_profile_page done in {time.time() - t_start:.1f}s")

    t = time.time()
    log(f"[PHASE] scrape_section filmes/ja-vi...")
    watched = scrape_section(session, username, "filmes", "ja-vi", errors)
    log(f"[PHASE] filmes/ja-vi done: {len(watched)} movies in {time.time() - t:.1f}s")

    t = time.time()
    log(f"[PHASE] scrape_section filmes/quero-ver...")
    watchlist = scrape_section(session, username, "filmes", "quero-ver", errors)
    log(f"[PHASE] filmes/quero-ver done: {len(watchlist)} movies in {time.time() - t:.1f}s")

    t = time.time()
    log(f"[PHASE] scrape_section filmes/favoritos...")
    favorites = scrape_section(session, username, "filmes", "favoritos", errors)
    log(f"[PHASE] filmes/favoritos done: {len(favorites)} movies in {time.time() - t:.1f}s")

    t = time.time()
    log(f"[PHASE] scrape_lists...")
    user_lists = scrape_lists(session, username, errors)
    log(f"[PHASE] scrape_lists done: {len(user_lists)} lists in {time.time() - t:.1f}s")

    log(f"[PHASE] Total scrape time: {time.time() - t_start:.1f}s")

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
    print(msg, file=sys.stderr, flush=True)


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
