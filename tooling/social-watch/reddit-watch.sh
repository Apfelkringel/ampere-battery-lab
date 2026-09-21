#!/usr/bin/env bash
# Append-only Reddit/Google News/HN mention log for Ampere Battery Lab.
# Designed to run from cron; exits 0 silently when there is nothing new.
set -uo pipefail

LOG=/Volumes/MacSSD/02_PROJECTS/Active/AccuBattery/tooling/social-watch/mentions.log
TMP=$(mktemp)
trap 'rm -f "$TMP"' EXIT

QUERY='ampere+battery+lab'
TS=$(date -u +%Y-%m-%dT%H:%M:%SZ)

# Google News RSS
curl -sS --max-time 10 "https://news.google.com/rss/search?q=%22$QUERY%22&hl=en"   | awk -v ts="$TS" '/<item>/{flag=1} flag && /<title>/{gsub(/<![CDATA[|]]>/,""); print ts"|news|"$0; flag=0}'   >> "$TMP" 2>/dev/null || true

# Hacker News
curl -sS --max-time 10 "https://hn.algolia.com/api/v1/search?query=$QUERY&tags=story&hitsPerPage=20"   | python3 -c "import json,sys
data = json.load(sys.stdin)
for h in data.get('hits', []):
    print(f"{sys.argv[1]}|hn|{h.get('title','')}|{h.get('url','')}")" "$TS"   >> "$TMP" 2>/dev/null || true

# DuckDuckGo HTML — Reddit-only
curl -sS --max-time 10 -A 'Mozilla/5.0' "https://duckduckgo.com/html/?q=site%3Areddit.com+%22ampere+battery+lab%22"   | grep -oE 'reddit.com/r/[^/]+/comments/[0-9a-z]+'   | awk -v ts="$TS" '{print ts"|reddit|"$0}' >> "$TMP" 2>/dev/null || true

# Filter out duplicates that already exist in mentions.log
if [ -f "$LOG" ]; then
  sort -u "$LOG" "$TMP" -o "$TMP.tmp" && mv "$TMP.tmp" "$TMP"
fi

if [ -s "$TMP" ]; then
  cat "$TMP" >> "$LOG"
  echo "[$TS] $(wc -l < "$TMP" | tr -d ' ') new mention(s) appended"
else
  exit 0
fi
