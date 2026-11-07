# API contracts (observed, undocumented)

Host: https://amp-api.podcasts.apple.com. Public authorization is discovered from the official web client's public assets at runtime; do not hardcode a captured bearer. Account requests use the media-user-token header from CookieManager. Do not log header values or account response bodies. Prefer the app's official Apple login flow to manual token entry.

| Purpose | Observed path / behavior |
|---|---|
| Account storefront | GET /v1/me/storefront |
| Followed shows | GET /v1/me/library/podcasts |
| Saved episodes | GET /v1/me/library/podcast-episodes |
| New/latest episodes | GET /v1/me/library/new-episodes or /library/latest-episodes |
| Continue/recent | GET /v1/me/continue-playing/podcast-episodes or /recent/played/podcast-episodes |
| Follow/save | POST /v1/me/library/{podcasts or podcast-episodes}/{id} -> 202 |
| Unfollow/unsave | DELETE same singleton path -> 204 |
| Position read | GET /v1/me/playback/positions/podcast-episodes?ids={id} |
| Position write | PUT /v1/me/playback/positions/podcast-episodes/{id} -> 204 |
| Charts | GET /v1/catalog/{storefront}/charts |
| Search | GET /v1/catalog/{storefront}/search |
| Show episodes | GET /v1/catalog/{storefront}/podcasts/{id}?include=episodes |
| Episode/audio | GET /v1/catalog/{storefront}/podcast-episodes/{id} |
| Editorial/search categories | GET /v1/editorial/{storefront}/groupings, names podcasts / search-landing |

Pagination may expose multiple next links; preserve pending cursors. Write paths are singletons, not query-id collections. Position attributes include positionInMilliseconds, completed and recordedAtTimestamp. Respect account storefront and provider access restrictions. An attempted generic recommendations endpoint was unavailable; do not replace personalized recommendations with charts under a misleading label.

Tests do not prove future endpoint availability. Revalidate changes with an authorized account without checking responses or tokens into git.
