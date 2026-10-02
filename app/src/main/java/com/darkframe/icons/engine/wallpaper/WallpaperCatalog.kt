package com.darkframe.icons.engine.wallpaper

import com.darkframe.icons.model.ContentTier
import com.darkframe.icons.model.WallpaperCategory

/**
 * Every wallpaper DarkFrame ships.
 *
 * One rule governs this list: **a category exists only if it has wallpapers in it.** An empty
 * category on a browse screen is a promise the product does not keep, so [categoriesWithContent] is
 * what the UI iterates, not the enum.
 */
object WallpaperCatalog {

    val all: List<WallpaperSpec> = listOf(
        // AMOLED — true black, where the panel genuinely switches pixels off.
        WallpaperSpec(
            "amoled_void", "Void", WallpaperCategory.AMOLED, WallpaperPattern.SOLID,
            base = 0xFF000000L, accent = 0xFF000000L, intensity = 0f, tier = ContentTier.FREE,
        ),
        WallpaperSpec(
            "amoled_horizon", "Horizon", WallpaperCategory.AMOLED, WallpaperPattern.GLOW,
            base = 0xFF000000L, accent = 0xFF14161AL, intensity = 0.5f, tier = ContentTier.FREE,
        ),
        WallpaperSpec(
            "amoled_frame", "Frame", WallpaperCategory.AMOLED, WallpaperPattern.FRAME,
            base = 0xFF000000L, accent = 0xFF2A2D32L, intensity = 0.7f, tier = ContentTier.FREE,
        ),

        // Carbon — fine weave, the texture that gives a dark screen depth without colour.
        WallpaperSpec(
            "carbon_weave", "Weave", WallpaperCategory.CARBON, WallpaperPattern.WEAVE,
            base = 0xFF0C0D10L, accent = 0xFF191C21L, intensity = 0.55f, tier = ContentTier.FREE,
        ),
        WallpaperSpec(
            "carbon_deep", "Deep Carbon", WallpaperCategory.CARBON, WallpaperPattern.WEAVE,
            base = 0xFF08090BL, accent = 0xFF14161AL, intensity = 0.8f, tier = ContentTier.PRO,
        ),

        // Graphite — the neutral mid-dark ground most icons sit best on.
        WallpaperSpec(
            "graphite_wash", "Graphite", WallpaperCategory.GRAPHITE, WallpaperPattern.GLOW,
            base = 0xFF121418L, accent = 0xFF262A31L, intensity = 0.6f, tier = ContentTier.FREE,
        ),
        WallpaperSpec(
            "graphite_arcs", "Slate Arcs", WallpaperCategory.GRAPHITE, WallpaperPattern.ARCS,
            base = 0xFF101216L, accent = 0xFF272C33L, intensity = 0.45f, tier = ContentTier.PRO,
        ),

        // Titanium — brushed metal, matching the Titanium collection.
        WallpaperSpec(
            "titanium_brushed", "Brushed", WallpaperCategory.TITANIUM, WallpaperPattern.BRUSHED,
            base = 0xFF1C1F24L, accent = 0xFF3A3F47L, intensity = 0.5f, tier = ContentTier.FREE,
        ),
        WallpaperSpec(
            "titanium_edge", "Edge", WallpaperCategory.TITANIUM, WallpaperPattern.BRUSHED,
            base = 0xFF24282EL, accent = 0xFF4A505AL, intensity = 0.35f, tier = ContentTier.PRO,
        ),

        // Glass — restrained translucency, matching the Glass collection.
        WallpaperSpec(
            "glass_panes", "Panes", WallpaperCategory.GLASS, WallpaperPattern.PANES,
            base = 0xFF0D1014L, accent = 0xFF2E3A46L, intensity = 0.5f, tier = ContentTier.FREE,
        ),
        WallpaperSpec(
            "glass_frost", "Frosted", WallpaperCategory.GLASS, WallpaperPattern.PANES,
            base = 0xFF141A20L, accent = 0xFF43525FL, intensity = 0.75f, tier = ContentTier.PRO,
        ),

        // Minimal — one hairline, nothing else.
        WallpaperSpec(
            "minimal_hairline", "Hairline", WallpaperCategory.MINIMAL, WallpaperPattern.FRAME,
            base = 0xFF0A0B0DL, accent = 0xFF3A3F47L, intensity = 0.5f, tier = ContentTier.FREE,
        ),
        WallpaperSpec(
            "minimal_paper", "Off-White", WallpaperCategory.MINIMAL, WallpaperPattern.GLOW,
            base = 0xFFEDEBE6L, accent = 0xFFFFFFFFL, intensity = 0.4f, tier = ContentTier.PRO,
        ),

        // Abstract — the only place DarkFrame allows a drawn form.
        WallpaperSpec(
            "abstract_arcs", "Arcs", WallpaperCategory.ABSTRACT, WallpaperPattern.ARCS,
            base = 0xFF0B0D11L, accent = 0xFF31404EL, intensity = 0.6f, tier = ContentTier.FREE,
        ),
        WallpaperSpec(
            "abstract_drift", "Drift", WallpaperCategory.ABSTRACT, WallpaperPattern.GLOW,
            base = 0xFF0A0C10L, accent = 0xFF2B3340L, intensity = 0.75f, tier = ContentTier.PRO,
        ),

        // Fold — composed for a tall cover screen and a near-square inner screen.
        WallpaperSpec(
            "fold_seam", "Seam", WallpaperCategory.FOLD, WallpaperPattern.SEAM,
            base = 0xFF0A0B0EL, accent = 0xFF1C2026L, intensity = 0.5f, tier = ContentTier.FREE,
        ),
        WallpaperSpec(
            "fold_duo", "Duo", WallpaperCategory.FOLD, WallpaperPattern.SEAM,
            base = 0xFF101318L, accent = 0xFF262C35L, intensity = 0.7f, tier = ContentTier.PRO,
        ),
    )

    private val byId = all.associateBy { it.id }

    fun byId(id: String?): WallpaperSpec? = id?.let { byId[it] }

    /** Categories that actually contain something, in catalog order. */
    fun categoriesWithContent(): List<WallpaperCategory> =
        all.map { it.category }.distinct()

    fun inCategory(category: WallpaperCategory): List<WallpaperSpec> =
        all.filter { it.category == category }

    val default: WallpaperSpec get() = all.first()
}
