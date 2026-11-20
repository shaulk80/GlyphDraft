package com.glyphdraft.core

import com.glyphdraft.core.stub.StubModelCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelCatalogTest {
    private val latin = ModelPack("latin", "Latin", setOf(Language.English, Language.German), 100)
    private val russian = ModelPack("eslav", "Russian", setOf(Language.Russian), 50)
    private val russianToo = ModelPack("cyrillic", "Cyrillic", setOf(Language.Russian), 60)

    private fun catalog(installed: Set<String>) =
        StubModelCatalog(packs = listOf(latin, russian, russianToo), installed = installed)

    @Test
    fun usableLanguagesAreThoseOfInstalledPacks() {
        val cases =
            listOf(
                emptySet<String>() to emptySet(),
                setOf("latin") to setOf(Language.English, Language.German),
                setOf("latin", "eslav") to setOf(Language.English, Language.German, Language.Russian),
            )

        for ((installed, expected) in cases) {
            assertEquals(expected, catalog(installed).usableLanguages(), "installed=$installed")
        }
    }

    @Test
    fun knownLanguagesIgnoreInstallState() {
        assertEquals(
            setOf(Language.English, Language.German, Language.Russian),
            catalog(emptySet()).knownLanguages(),
        )
    }

    @Test
    fun packForPrefersInstalledThenFirstListed() {
        val cases =
            listOf(
                Triple(emptySet<String>(), Language.Russian, russian),
                Triple(setOf("cyrillic"), Language.Russian, russianToo),
                Triple(setOf("eslav", "cyrillic"), Language.Russian, russian),
                Triple(emptySet<String>(), Language.English, latin),
            )

        for ((installed, language, expected) in cases) {
            assertEquals(expected, catalog(installed).packFor(language), "installed=$installed, language=$language")
        }
    }

    @Test
    fun packForUnknownLanguageIsNull() {
        assertNull(catalog(emptySet()).packFor(Language.Hebrew))
    }

    @Test
    fun installedPacksKeepCatalogOrderAndIsInstalledMatches() {
        val catalog = catalog(setOf("cyrillic", "latin"))

        assertEquals(listOf(latin, russianToo), catalog.installedPacks())
        assertTrue(catalog.isInstalled(latin))
        assertFalse(catalog.isInstalled(russian))
    }

    @Test
    fun installProgressFraction() {
        val cases =
            listOf(
                InstallProgress(0, 0) to null,
                InstallProgress(10, 0) to null,
                InstallProgress(0, 200) to 0.0,
                InstallProgress(50, 200) to 0.25,
                InstallProgress(200, 200) to 1.0,
                InstallProgress(300, 200) to 1.0,
            )

        for ((progress, expected) in cases) {
            assertEquals(expected, progress.fraction, "progress=$progress")
        }
    }
}
