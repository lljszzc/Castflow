package dev.podcast.nativeapp

// Only editorial UI headings are normalized; programme names retain their original spelling.
fun editorialTitle(title: String): String {
    val traditional = "熱門節單訂閱繼續聽發現體學習歷藝術與會類別為愛歡薦選訊聞商業運動閒遊戲電視應該儲存"
    val simplified = "热门节单订阅继续听发现体学习历艺术与会类别为爱欢荐选讯闻商业运动闲游戏电视应该储存"
    return title.trim().map { c -> traditional.indexOf(c).let { if(it >= 0) simplified[it] else c } }.joinToString("")
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
