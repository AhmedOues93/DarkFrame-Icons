package com.darkframe.icons.engine

import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.IconCacheKey
import com.darkframe.icons.engine.domain.IconStyleCatalog
import com.darkframe.icons.engine.domain.StableHash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconCacheKeyTest {

    private val identity = AppIdentity("com.spotify.music", "com.spotify.music.MainActivity", "Spotify", 100L)
    private val style = IconStyleCatalog.noir

    private fun key(
        id: AppIdentity = identity,
        s: com.darkframe.icons.engine.domain.IconStyle = style,
        size: Int = 192,
        curated: Boolean = false,
    ) = IconCacheKey.of(id, s, size, curated)

    @Test
    fun theSameInputsAlwaysProduceTheSameKey() {
        assertEquals(key(), key())
        assertEquals(IconCacheKey.diskName(key()), IconCacheKey.diskName(key()))
    }

    @Test
    fun anAppUpdateProducesANewKey() {
        // This is what makes app updates correct with no explicit invalidation call: a new
        // lastUpdateTime simply misses the cache.
        assertNotEquals(key(), key(id = identity.copy(versionStamp = 101L)))
    }

    @Test
    fun styleSizeComponentAndCuratedFlagAllDiscriminate() {
        val base = key()
        assertNotEquals(base, key(s = IconStyleCatalog.frost))
        assertNotEquals(base, key(size = 96))
        assertNotEquals(base, key(curated = true))
        assertNotEquals(base, key(id = identity.copy(activityName = "com.spotify.music.Other")))
        assertNotEquals(base, key(id = identity.copy(packageName = "com.other")))
    }

    @Test
    fun theKeyCarriesTheRenderVersionSoARendererChangeInvalidatesEverything() {
        assertTrue(key().startsWith("v${com.darkframe.icons.engine.domain.IconStyle.RENDER_VERSION}|"))
    }

    @Test
    fun twoAppsThatDifferOnlyByLabelShareARenderedIconKey() {
        // Labels are not part of rendering, so they must not fragment the cache.
        assertEquals(key(), key(id = identity.copy(label = "Spotify Music")))
    }

    @Test
    fun diskNamesAreFilesystemSafeAndFixedLength() {
        val name = IconCacheKey.diskName(key())
        assertTrue(name.endsWith(".png"))
        assertEquals(20, name.length)
        assertTrue(name.removeSuffix(".png").all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun packagesAreShardedSoAnUninstallIsASingleDirectoryDelete() {
        assertEquals(IconCacheKey.packageShard("com.a"), IconCacheKey.packageShard("com.a"))
        assertNotEquals(IconCacheKey.packageShard("com.a"), IconCacheKey.packageShard("com.b"))
    }

    @Test
    fun stableHashIsPinnedBecauseItIsWrittenToDisk() {
        // Hard-coded on purpose: these values name files that must still be found after an
        // app upgrade or a JVM change, so the algorithm is not free to drift.
        assertEquals("af63dc4c8601ec8c", StableHash.hex("a"))
        assertEquals("cbf29ce484222325", StableHash.hex(""))
        assertEquals(16, StableHash.hex("com.example.someapp").length)
    }
}
