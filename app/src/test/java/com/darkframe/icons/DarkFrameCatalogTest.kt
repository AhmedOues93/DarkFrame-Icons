package com.darkframe.icons
import com.darkframe.icons.data.DarkFrameCatalog
import com.darkframe.icons.model.IconCollection
import org.junit.Assert.*
import org.junit.Test
class DarkFrameCatalogTest{
 @Test fun searchFindsChatGpt(){assertEquals("ChatGPT",DarkFrameCatalog.search("chatgpt").single().label)}
 @Test fun blankSearchReturnsCatalog(){assertEquals(DarkFrameCatalog.icons.size,DarkFrameCatalog.search(" ").size)}
 @Test fun collectionsAreUniqueStyles(){assertTrue(DarkFrameCatalog.styles.map{it.collection}.containsAll(IconCollection.entries))}
}
