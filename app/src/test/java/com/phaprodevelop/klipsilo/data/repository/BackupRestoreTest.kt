package com.phaprodevelop.klipsilo.data.repository

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.data.db.ContentType
import com.phaprodevelop.klipsilo.data.model.ClipboardEntry
import com.phaprodevelop.klipsilo.fakes.FakeClipboardItemDao
import com.phaprodevelop.klipsilo.util.MainDispatcherRule
import com.phaprodevelop.klipsilo.viewmodel.ClipboardViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
class BackupRestoreTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dao: FakeClipboardItemDao
    private lateinit var repo: ClipboardRepository
    private lateinit var context: Application

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dao = FakeClipboardItemDao()
        repo = ClipboardRepository(dao, context)
    }

    @After
    fun tearDown() {
        if (!::context.isInitialized) return
        listOf("images", "pdfs", "files").forEach { name ->
            File(context.filesDir, name).deleteRecursively()
        }
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".zip") || file.name.startsWith("backup_import_")) {
                file.deleteRecursively()
            }
        }
        File(context.cacheDir, "evil.txt").delete()
        File(context.filesDir, "evil.txt").delete()
    }

    @Test
    fun backup_textClip_manifestContainsFields() =
        runTest {
            // Arrange
            val entry =
                textEntry(
                    id = 10L,
                    text = "clip body",
                    isPinned = true,
                    isLocked = true,
                )
            repo.insertEntry(entry)
            val zip = createEmptyZipFile("text-backup.zip")

            // Act
            val result = repo.backupClipsToZip(Uri.fromFile(zip), listOf(entry))
            val manifest = zipManifest(zip)
            val clip = manifest.getJSONArray("clips").getJSONObject(0)

            // Assert
            assertEquals(1, result.exported)
            assertEquals(0, result.missingFiles)
            assertEquals("KlipSilo", manifest.getString("app"))
            assertEquals(1, manifest.getInt("version"))
            assertEquals("TEXT", clip.getString("contentType"))
            assertEquals("clip body", clip.getString("textContent"))
            assertTrue(clip.getBoolean("isLocked"))
            assertTrue(clip.getBoolean("isPinned"))
            assertFalse(clip.has("latitude"))
            assertFalse(clip.has("longitude"))
        }

    @Test
    fun backup_image_includesFilesEntry_andCountsMissing() =
        runTest {
            // Arrange
            val imageBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
            val existingFile = createStoredFile("images", "photo.jpg", imageBytes)
            val existing = imageEntry(existingFile, id = 11L)
            val missing =
                imageEntry(
                    File(File(context.filesDir, "images"), "gone.jpg"),
                    id = 12L,
                    preview = "gone",
                )
            assertFalse(File(missing.filePath!!).exists())
            repo.insertEntry(existing)
            repo.insertEntry(missing)
            val zip = createEmptyZipFile("image-backup.zip")

            // Act
            val result = repo.backupClipsToZip(Uri.fromFile(zip), listOf(existing, missing))
            val names = zipEntryNames(zip)
            val clips = zipManifest(zip).getJSONArray("clips")
            val existingObj = clips.getJSONObject(0)
            val missingObj = clips.getJSONObject(1)

            // Assert
            assertEquals(2, result.exported)
            assertEquals(1, result.missingFiles)
            assertTrue(names.contains("files/0_photo.jpg"))
            assertEquals("files/0_photo.jpg", existingObj.getString("fileName"))
            assertFalse(missingObj.has("fileName"))
        }

    @Test
    fun restore_roundTrip_textAndImage() =
        runTest {
            // Arrange
            val imageBytes = byteArrayOf(1, 2, 3, 4, 5)
            val imageFile = createStoredFile("images", "photo.jpg", imageBytes)
            val text = textEntry(id = 21L, text = "round-trip text")
            val image = imageEntry(imageFile, id = 22L)
            repo.insertEntry(text)
            repo.insertEntry(image)
            val zip = createEmptyZipFile("round-trip.zip")
            val backup = repo.backupClipsToZip(Uri.fromFile(zip), listOf(text, image))
            assertEquals(2, backup.exported)
            assertEquals(0, backup.missingFiles)

            val restoreDao = FakeClipboardItemDao()
            val restoreRepo = ClipboardRepository(restoreDao, context)

            // Act
            val result = restoreRepo.restoreClipsFromZip(Uri.fromFile(zip))
            val restored = restoreRepo.getAllClips().first()

            // Assert
            assertEquals(2, result.loaded)
            assertEquals(0, result.skipped)
            assertFalse(result.invalid)
            assertEquals(2, restored.size)
            restored.forEach { clip ->
                assertNotEquals(text.id, clip.id)
                assertNotEquals(image.id, clip.id)
            }
            val restoredText = restored.single { it.contentType == ContentType.TEXT }
            assertEquals(null, restoredText.textContent)
            assertEquals("round-trip text", restoreRepo.getClip(restoredText.id)?.textContent)
            val restoredImage = restored.single { it.contentType == ContentType.IMAGE }
            assertArrayEquals(imageBytes, File(restoredImage.filePath!!).readBytes())
        }

    @Test
    fun backupRestore_fileBackedText_keepsTextTypeAndBody() =
        runTest {
            // Arrange
            val body = "x".repeat(300 * 1024)
            assertEquals(SaveClipResult.Saved, repo.saveNewClip(body, null, null, null))
            val listRow = repo.getAllClips().first().single()
            val zip = createEmptyZipFile("file-backed-text.zip")

            // Act
            val backup = repo.backupClipsToZip(Uri.fromFile(zip), listOf(listRow))
            val names = zipEntryNames(zip)
            val clip = zipManifest(zip).getJSONArray("clips").getJSONObject(0)

            val restoreDao = FakeClipboardItemDao()
            val restoreRepo = ClipboardRepository(restoreDao, context)
            val restore = restoreRepo.restoreClipsFromZip(Uri.fromFile(zip))
            val restoredList = restoreRepo.getAllClips().first().single()
            val restored = restoreRepo.getClip(restoredList.id)!!

            // Assert
            assertEquals(1, backup.exported)
            assertEquals(0, backup.missingFiles)
            assertEquals("TEXT", clip.getString("contentType"))
            assertFalse(clip.has("textContent"))
            assertTrue(names.any { it.startsWith("files/") && it.endsWith(".txt") })
            assertEquals(1, restore.loaded)
            assertEquals(0, restore.skipped)
            assertEquals(ContentType.TEXT, restored.contentType)
            assertEquals(null, restoredList.textContent)
            assertTrue(restored.filePath != null)
            assertTrue(File(restored.filePath!!).exists())
            assertEquals(body, restoreRepo.readTextBody(restoredList))
        }

    @Test
    fun restore_legacySnipdockManifest_loads() =
        runTest {
            val zip = File(context.cacheDir, "legacy-snipdock.zip")
            writeZip(
                zip,
                "snipdock_manifest.json" to
                    manifestJson(
                        app = "Snipdock",
                        clips =
                            listOf(
                                JSONObject()
                                    .put("contentType", "TEXT")
                                    .put("textContent", "legacy clip")
                                    .put("previewText", "legacy clip")
                                    .put("timestamp", 1L),
                            ),
                    ).toByteArray(),
            )

            val result = repo.restoreClipsFromZip(Uri.fromFile(zip))

            assertFalse(result.invalid)
            assertEquals(1, result.loaded)
            assertEquals("legacy clip", dao.getAllOnce().single().textContent)
        }

    @Test
    fun restore_wrongApp_invalid() =
        runTest {
            // Arrange
            val zip = File(context.cacheDir, "wrong-app.zip")
            writeZip(
                zip,
                "klipsilo_manifest.json" to
                    manifestJson(app = "Other", clips = listOf(textClipJson())).toByteArray(),
            )

            // Act
            val result = repo.restoreClipsFromZip(Uri.fromFile(zip))

            // Assert
            assertTrue(result.invalid)
            assertEquals(0, result.loaded)
            assertTrue(dao.getAllOnce().isEmpty())
        }

    @Test
    fun restore_futureVersion_invalid() =
        runTest {
            // Arrange
            val zip = File(context.cacheDir, "future-version.zip")
            writeZip(
                zip,
                "klipsilo_manifest.json" to
                    manifestJson(version = 99, clips = listOf(textClipJson())).toByteArray(),
            )

            // Act
            val result = repo.restoreClipsFromZip(Uri.fromFile(zip))

            // Assert
            assertTrue(result.invalid)
            assertEquals(0, result.loaded)
            assertTrue(dao.getAllOnce().isEmpty())
        }

    @Test
    fun restore_missingManifest_invalid() =
        runTest {
            // Arrange
            val zip = File(context.cacheDir, "no-manifest.zip")
            writeZip(zip, "readme.txt" to "not a backup".toByteArray())

            // Act
            val result = repo.restoreClipsFromZip(Uri.fromFile(zip))

            // Assert
            assertTrue(result.invalid)
            assertEquals(0, result.loaded)
            assertTrue(dao.getAllOnce().isEmpty())
        }

    @Test
    fun restore_unknownContentType_skipped() =
        runTest {
            // Arrange
            val zip = File(context.cacheDir, "unknown-type.zip")
            writeZip(
                zip,
                "klipsilo_manifest.json" to
                    manifestJson(
                        clips =
                            listOf(
                                JSONObject()
                                    .put("contentType", "VIDEO")
                                    .put("previewText", "clip")
                                    .put("timestamp", 1L),
                            ),
                    ).toByteArray(),
            )

            // Act
            val result = repo.restoreClipsFromZip(Uri.fromFile(zip))

            // Assert
            assertFalse(result.invalid)
            assertEquals(0, result.loaded)
            assertEquals(1, result.skipped)
            assertEquals(1, result.total)
            assertTrue(dao.getAllOnce().isEmpty())
        }

    @Test
    fun restore_fileBasedWithoutFile_skipped() =
        runTest {
            // Arrange
            val zip = File(context.cacheDir, "missing-file-entry.zip")
            writeZip(
                zip,
                "klipsilo_manifest.json" to
                    manifestJson(
                        clips =
                            listOf(
                                JSONObject()
                                    .put("contentType", "IMAGE")
                                    .put("fileName", "files/missing.jpg")
                                    .put("previewText", "missing")
                                    .put("timestamp", 1L),
                            ),
                    ).toByteArray(),
            )

            // Act
            val result = repo.restoreClipsFromZip(Uri.fromFile(zip))

            // Assert
            assertFalse(result.invalid)
            assertEquals(0, result.loaded)
            assertEquals(1, result.skipped)
            assertEquals(1, result.total)
            assertTrue(dao.getAllOnce().isEmpty())
        }

    @Test
    fun restore_exceptionAfterDestCopy_deletesCopiedFiles() =
        runTest {
            // Arrange: first clip copies a dest file, then a later clip throws while parsing
            val imageBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
            val zip = File(context.cacheDir, "restore-rollback.zip")
            writeZip(
                zip,
                "files/0_photo.jpg" to imageBytes,
                "klipsilo_manifest.json" to
                    manifestJson(
                        clips =
                            listOf(
                                JSONObject()
                                    .put("contentType", "IMAGE")
                                    .put("fileName", "files/0_photo.jpg")
                                    .put("previewText", "photo")
                                    .put("timestamp", 1L),
                                JSONObject()
                                    .put("contentType", "TEXT")
                                    .put("textContent", "x")
                                    .put("previewText", "x")
                                    .put("timestamp", 2L)
                                    .put("latitude", JSONArray().put("bad")),
                            ),
                    ).toByteArray(),
            )
            val before =
                listOf("images", "pdfs", "files").flatMap { name ->
                    File(context.filesDir, name).listFiles().orEmpty().map { it.canonicalPath }
                }.toSet()

            // Act
            val result = repo.restoreClipsFromZip(Uri.fromFile(zip))
            val after =
                listOf("images", "pdfs", "files").flatMap { name ->
                    File(context.filesDir, name).listFiles().orEmpty().map { it.canonicalPath }
                }.toSet()

            // Assert
            assertEquals(0, result.loaded)
            assertEquals(0, result.skipped)
            assertEquals(0, result.total)
            assertFalse(result.invalid)
            assertTrue(dao.getAllOnce().isEmpty())
            assertEquals(before, after)
        }

    @Test
    fun restore_zipSlip_doesNotWriteOutsideTemp() =
        runTest {
            // Restore only copies entries whose names start with "files/". The dest name is
            // the last path segment with ".." stripped: substringAfterLast('/').replace("..", "").
            // So files/../../evil.txt is written as cacheDir/backup_import_*/evil.txt (deleted
            // in finally), not cacheDir/evil.txt or filesDir/evil.txt.
            val zipName = "files/../../evil.txt"
            val sanitized = zipName.substringAfterLast('/').replace("..", "")
            assertEquals("evil.txt", sanitized)

            val zip = File(context.cacheDir, "zip-slip.zip")
            writeZip(
                zip,
                zipName to "pwned".toByteArray(),
                "klipsilo_manifest.json" to manifestJson(clips = emptyList()).toByteArray(),
            )

            // Act
            val result = repo.restoreClipsFromZip(Uri.fromFile(zip))

            // Assert
            assertFalse(result.invalid)
            assertFalse(File(context.cacheDir, "evil.txt").exists())
            assertFalse(File(context.filesDir, "evil.txt").exists())
            assertTrue(dao.getAllOnce().isEmpty())
        }

    @Test
    fun backupClips_emptyClips_toastsNoClipsToBackup() {
        // Arrange
        val viewModel =
            ClipboardViewModel(
                context,
                repo,
                ioDispatcher = mainDispatcherRule.dispatcher,
            )
        assertTrue(viewModel.clips.value.isEmpty())

        // Act
        viewModel.backupClips(Uri.fromFile(File(context.cacheDir, "unused.zip")))

        // Assert
        assertEquals(
            context.getString(R.string.no_clips_to_backup),
            viewModel.toastMessage.value,
        )
    }

    private fun textEntry(
        id: Long,
        text: String = "hello",
        preview: String = text,
        timestamp: Long = 1_700_000_000_000L,
        isPinned: Boolean = false,
        isLocked: Boolean = false,
        latitude: Double? = null,
        longitude: Double? = null,
    ) = ClipboardEntry(
        id = id,
        contentType = ContentType.TEXT,
        textContent = text,
        filePath = null,
        previewText = preview,
        timestamp = timestamp,
        isPinned = isPinned,
        isLocked = isLocked,
        latitude = latitude,
        longitude = longitude,
    )

    private fun imageEntry(
        file: File,
        id: Long,
        preview: String = file.nameWithoutExtension,
        timestamp: Long = 1_700_000_000_000L,
        isPinned: Boolean = false,
        isLocked: Boolean = false,
    ) = ClipboardEntry(
        id = id,
        contentType = ContentType.IMAGE,
        textContent = null,
        filePath = file.absolutePath,
        previewText = preview,
        timestamp = timestamp,
        isPinned = isPinned,
        isLocked = isLocked,
    )

    private fun createStoredFile(
        subdir: String,
        name: String,
        content: ByteArray = "abc".toByteArray(),
    ): File {
        val dir = File(context.filesDir, subdir).apply { mkdirs() }
        return File(dir, name).also { it.writeBytes(content) }
    }

    private fun createEmptyZipFile(name: String): File {
        val file = File(context.cacheDir, name)
        file.parentFile?.mkdirs()
        if (file.exists()) file.delete()
        file.createNewFile()
        return file
    }

    private fun writeZip(
        file: File,
        vararg entries: Pair<String, ByteArray>,
    ) {
        file.parentFile?.mkdirs()
        ZipOutputStream(file.outputStream().buffered()).use { zos ->
            for ((name, bytes) in entries) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
        }
    }

    private fun zipManifest(file: File): JSONObject {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("klipsilo_manifest.json")
            checkNotNull(entry) { "missing klipsilo_manifest.json" }
            val bytes = zip.getInputStream(entry).use { it.readBytes() }
            return JSONObject(bytes.toString(Charsets.UTF_8))
        }
    }

    private fun zipEntryNames(file: File): Set<String> {
        ZipFile(file).use { zip ->
            return zip.entries().toList().map { it.name }.toSet()
        }
    }

    private fun manifestJson(
        app: String = "KlipSilo",
        version: Int = 1,
        clips: List<JSONObject>,
    ): String {
        val clipsArray = JSONArray()
        clips.forEach { clipsArray.put(it) }
        return JSONObject()
            .put("app", app)
            .put("version", version)
            .put("clips", clipsArray)
            .toString()
    }

    private fun textClipJson(): JSONObject =
        JSONObject()
            .put("contentType", "TEXT")
            .put("textContent", "should not load")
            .put("previewText", "should not load")
            .put("timestamp", 1L)
}
