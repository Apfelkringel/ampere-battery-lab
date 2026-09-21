# Social and store monitoring

Reddit now requires OAuth-authenticated API calls for any real coverage of
subreddits. Without credentials our scrapers see only the auth-gate page.
This directory documents the next-best sources and a watchdog script.

## Sources that work without auth

| Source | Endpoint | Coverage |
|---|---|---|
| Google News | news.google.com/rss/search?q=%22ampere+battery+lab%22 | Broad mentions in news-shaped sources (mostly trade press). |
| Hacker News Algolia | hn.algolia.com/api/v1/search?query=… | Tech community; useful for indie reviews. |
| DuckDuckGo HTML | duckduckgo.com/html/?q=… | Acts as a Reddit search engine; rate-limited but works. |
| Play Store comments | play.google.com/store/apps/details?id=com.ampere.batterylab | Public reviews. Pages are JS-rendered so plain curl sees only the bootstrap; use a headless browser when review scraping becomes worthwhile. |

## Watchdog

social-watch/reddit-watch.sh queries the RSS/JSON sources above and
appends mentions to social-watch/mentions.log with timestamp, title and
URL. Run it from cron (every 6h is plenty) on any host that can reach
the public web.

    */15 * * * * /path/to/repo/tooling/social-watch/reddit-watch.sh

The script is dependency-free — only curl + a tiny bit of jq is needed.

## What counts as a "mention"

We are looking for:
- direct links to github.com/Apfelkringel/ampere-battery-lab or to the
  Play Store page
- reviews or first-hand usage reports of Ampere on real devices
- screenshots or recordings of the dashboard

Reports that merely cite the term "ampere" without any link or specific
context to the battery app are not actionable for this project.
