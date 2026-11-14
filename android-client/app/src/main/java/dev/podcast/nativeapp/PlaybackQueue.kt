package dev.podcast.nativeapp

internal fun buildPlaybackQueue(current: PodcastItem, candidates: List<PodcastItem>): List<PodcastItem> {
    val queue = linkedMapOf<String, PodcastItem>()
    val currentIsInCandidates = candidates.any { it.id == current.id }
    if (!currentIsInCandidates && current.id.isNotBlank() && current.audio.isNotBlank()) queue[current.id] = current
    candidates.forEach { candidate ->
        if (candidate.type != "podcast-episodes" || candidate.id.isBlank() || candidate.audio.isBlank()) return@forEach
        if (candidate.id == current.id && current.audio.isNotBlank()) queue[candidate.id] = current
        else queue.putIfAbsent(candidate.id, candidate)
    }
    if (current.id.isNotBlank() && current.audio.isNotBlank()) queue.putIfAbsent(current.id, current)
    return queue.values.toList().ifEmpty { listOf(current) }
}
