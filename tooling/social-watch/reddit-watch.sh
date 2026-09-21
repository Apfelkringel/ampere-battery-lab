#!/usr/bin/env bash
# Append-only mention log for Ampere Battery Lab. Curl-based, no auth.
# Designed for cron (every 15-60 minutes).
set -uo pipefail

REPO=/Volumes/MacSSD/02_PROJECTS/Active/AccuBattery
LOG=$REPO/tooling/social-watch/mentions.log
TMP=$(mktemp)
trap 'rm -f "$TMP"' EXIT

TS=$(date -u +%Y-%m-%dT%H:%M:%SZ)

# Google News RSS — items have <title>...</title>
curl -sS --max-time 10 'https://news.google.com/rss/search?q=%22ampere+battery+lab%22&hl=en'   | python3 -c "
import sys, re, datetime
ts = sys.argv[1]
xml = sys.stdin.read()
for item in re.findall(r'<item>(.*?)</item>', xml, re.DOTALL):
    t = re.search(r'<title>(.*?)</title>', item)
    l = re.search(r'<link>(.*?)</link>', item)
    if t:
        print(ts + '|news|' + t.group(1).strip() + '|' + (l.group(1).strip() if l else ''))
" "$TS" >> "$TMP" 2>/dev/null || true

# Hacker News
curl -sS --max-time 10 'https://hn.algolia.com/api/v1/search?query=ampere+battery+lab&tags=story&hitsPerPage=20'   | python3 -c "
import sys, json
ts = sys.argv[1]
data = json.load(sys.stdin)
for h in data.get('hits', []):
    title = h.get('title') or ''
    url = h.get('url') or ('https://news.ycombinator.com/item?id=' + str(h.get('objectID', '')))
    print(ts + '|hn|' + title + '|' + url)
" "$TS" >> "$TMP" 2>/dev/null || true

# DuckDuckGo HTML — Reddit-only filter via site:
curl -sS --max-time 10 -A 'Mozilla/5.0' 'https://duckduckgo.com/html/?q=site%3Areddit.com+%22ampere+battery+lab%22'   | grep -oE 'reddit.com/r/[^/]+/comments/[0-9a-z]+'   | awk -v ts="$TS" '{print ts"|reddit|"$0}' >> "$TMP" 2>/dev/null || true

# Deduplicate against the persistent log
if [ -f "$LOG" ]; then
  sort -u "$LOG" "$TMP" -o "$TMP.tmp"
  diff "$TMP" "$TMP.tmp" >> "$LOG" || true
  rm -f "$TMP.tmp"
else
  cat "$TMP" >> "$LOG"
fi

if [ -s "$TMP" ]; then
  size_before=$(wc -c < "$LOG" 2>/dev/null || echo 0)
  cat "$TMP" >> "$LOG"
  echo "[$TS] $(wc -l < "$TMP" | tr -d ' ') new mention(s) appended"
fi
