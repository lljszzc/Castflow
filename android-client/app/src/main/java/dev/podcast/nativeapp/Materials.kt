package dev.podcast.nativeapp

import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.chrisbanes.haze.HazeState

val LocalGlassState = staticCompositionLocalOf<HazeState> { error("Missing glass source") }
data class ArtworkColors(val background: Color, val foreground: Color, val playerBackground: Color = Color(0xFF615B68)) {
    val secondary get() = foreground.copy(alpha = .64f)
}
private val artworkColors = object : LinkedHashMap<String, ArtworkColors>(64, .75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ArtworkColors>?) = size > 64
}
@Composable fun rememberArtworkColors(url: String): ArtworkColors {
    val context = LocalContext.current
    val result by produceState(ArtworkColors(Color(0xFFECECF0), Color.Black), url) {
        value = artworkColors[url] ?: ArtworkColors(Color(0xFFECECF0), Color.Black)
        if (url.isBlank()) return@produceState
        artworkColors[url]?.let { value = it; return@produceState }
        val extracted = withContext(Dispatchers.IO) {
            runCatching {
                val image = context.imageLoader.execute(ImageRequest.Builder(context).data(url).size(96).allowHardware(false).build())
                val bitmap = (image.drawable as? BitmapDrawable)?.bitmap ?: return@runCatching null
                val palette = Palette.from(bitmap).maximumColorCount(16).generate()
                val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: return@runCatching null
                val bg = Color(swatch.rgb)
                // WCAG luminance threshold selects whichever of black/white has
                // the higher contrast. Accent saturation is kept from the cover.
                val dominant = Color(palette.dominantSwatch?.rgb ?: swatch.rgb)
                var playerBg = androidx.compose.ui.graphics.lerp(dominant, Color(0xFF77717C), .6f)
                while(playerBg.luminance() > .16f) playerBg = androidx.compose.ui.graphics.lerp(playerBg,Color.Black,.08f)
                ArtworkColors(bg, if (bg.luminance() > .179f) Color.Black else Color.White, playerBg)
            }.getOrNull()
        }
        if (extracted != null) { artworkColors[url] = extracted; value = extracted }
    }
    return result
}
