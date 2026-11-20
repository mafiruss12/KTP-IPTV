# KTP-IPTV

Application Android IPTV moderne écrite en **Kotlin** + **Jetpack Compose**.

## Fonctionnalités

- Lecture des playlists **M3U / M3U8** et comptes **Xtream Codes**
- Interface Material 3 claire et intuitive
- Filtres : **pays**, **catégorie**, **langue**
- Recherche rapide
- Liste de **favoris** persistante (Room)
- Support proxy HTTP (pour contourner blocages / problèmes HTTP-HTTPS)
- Cleartext HTTP autorisé (beaucoup de flux IPTV sont en HTTP pur)
- Lecteur **Media3 / ExoPlayer** (HLS, DASH, TS…)
- Source publique par défaut : [iptv-org/iptv](https://github.com/iptv-org/iptv)

## Sources de données analysées

Le dépôt [iptv-org/iptv](https://github.com/iptv-org/iptv) fournit notamment :

| Type              | URL |
|-------------------|-----|
| Toutes les chaînes | https://iptv-org.github.io/iptv/index.m3u |
| Par pays          | https://iptv-org.github.io/iptv/index.country.m3u |
| Par catégorie     | https://iptv-org.github.io/iptv/index.category.m3u |
| Par langue        | https://iptv-org.github.io/iptv/index.language.m3u |
| Pays spécifique   | `https://iptv-org.github.io/iptv/countries/{code}.m3u` |

L'application charge par défaut `index.m3u` et extrait les métadonnées (`tvg-country`, `tvg-language`, `group-title`, logos…).

## Proxy & HTTP / HTTPS

Un problème fréquent sur Android est le refus du trafic **cleartext** (HTTP).  
L'app inclut :

1. `android:usesCleartextTraffic="true"`
2. Un `network_security_config.xml` permissif
3. Un module OkHttp configurable avec **proxy HTTP** (hôte + port) depuis les paramètres

## Build local

```bash
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/
```

Ouvre le projet dans **Android Studio** (Ladybug / Koala ou plus récent).

## CI/CD

Workflow GitHub Actions (`.github/workflows/android.yml`) :

- Build debug + release sur chaque push / PR
- Upload des APK en artefacts

## Avertissement légal

Cette application est un **lecteur** uniquement.  
Elle ne fournit aucun contenu piraté. Les listes publiques iptv-org contiennent des liens vers des flux volontairement publiés par leurs propriétaires. Utilisez uniquement des sources légales.

## Licence

MIT
