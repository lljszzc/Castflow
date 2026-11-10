package dev.podcast.nativeapp

// Only editorial UI headings are normalized; programme names retain their original spelling.
fun editorialTitle(title: String): String {
    val traditional = "熱門節單訂閱繼續聽發現體學習歷藝術與會類別為愛歡薦選訊聞商業運動閒遊戲電視應該儲存"
    val simplified = "热门节单订阅继续听发现体学习历艺术与会类别为爱欢荐选讯闻商业运动闲游戏电视应该储存"
    return buildString(title.length) {
        for (c in title.trim()) {
            val index = traditional.indexOf(c)
            append(if (index >= 0) simplified[index] else c)
        }
    }
}
fun uniqueEditorialSections(sections: List<EditorialSection>): List<EditorialSection> {
    val result = linkedMapOf<String,EditorialSection>()
    sections.forEach { section ->
        val title = editorialTitle(section.title)
        val previous = result[title]
        result[title] = if(previous == null) section.copy(title = title) else previous.copy(items = (previous.items+section.items).distinctBy { it.type+":"+it.id })
    }
    return result.values.toList()
}

enum class DiscoveryChart(val shelfId: String, val section: String, val title: String) {
    Shows("chart-shows", "节目", "热门节目排行"),
    Episodes("chart-episodes", "单集", "热门单集排行"),
    Subscribers("chart-subscribers", "订阅节目", "订阅节目排行")
}

fun mergeDiscovery(previews: List<EditorialSection>, editorial: List<EditorialSection>): List<EditorialSection> {
    val byTitle = editorial.associateBy { editorialTitle(it.title) }
    val titles = previews.map { it.title }.toSet()
    return previews.map { preview ->
        if (preview.items.isEmpty()) preview.copy(items = byTitle[preview.title]?.items.orEmpty().take(6)) else preview
    } + editorial.filter { editorialTitle(it.title) !in titles }
}
