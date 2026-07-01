package com.novel.app.utils
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EpubGenerator(private val context: Context) {
    @Throws(Exception::class)
    fun generate(title: String, author: String, intro: String,
                 chapters: List<Map<String, String>>, coverBytes: ByteArray?): Uri {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // mimetype
            val mimeEntry = ZipEntry("mimetype").apply {
                method = ZipEntry.STORED
                size = 24
                crc = 0x2CAB616F
            }
            zos.putNextEntry(mimeEntry)
            zos.write("application/epub+zip".toByteArray())
            zos.closeEntry()

            // container.xml
            addEntry(zos, "META-INF/container.xml", """
                <?xml version="1.0"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                    <rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
                </container>
            """.trimIndent().toByteArray())

            // content.opf
            val opf = StringBuilder()
            opf.append("""<?xml version="1.0"?><package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="BookId"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="BookId">cooks-${System.currentTimeMillis()}</dc:identifier><dc:title>$title</dc:title><dc:creator>$author</dc:creator></metadata><manifest><item id="style" href="style.css" media-type="text/css"/>""")
            chapters.indices.forEach { i -> opf.append("""<item id="chap${i+1}" href="chap${i+1}.xhtml" media-type="application/xhtml+xml"/>""") }
            opf.append("""</manifest><spine toc="ncx">""")
            chapters.indices.forEach { i -> opf.append("""<itemref idref="chap${i+1}"/>""") }
            opf.append("""</spine></package>""")
            addEntry(zos, "OEBPS/content.opf", opf.toString().toByteArray())

            addEntry(zos, "OEBPS/style.css", "body { direction: rtl; text-align: justify; font-family: sans-serif; }".toByteArray())
            chapters.forEachIndexed { i, ch ->
                val content = """<?xml version="1.0"?><html xmlns="http://www.w3.org/1999/xhtml"><head><link rel="stylesheet" href="style.css"/></head><body><h2>${ch["name"]}</h2><p>${ch["content"]?.replace("\n", "<br/>")}</p></body></html>"""
                addEntry(zos, "OEBPS/chap${i+1}.xhtml", content.toByteArray())
            }
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, title.replace("[\\\\/*?:\"<>|]".toRegex(), "") + ".epub")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/epub+zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
            ?: throw Exception("فشل إنشاء الملف")
        context.contentResolver.openOutputStream(uri)?.use { it.write(baos.toByteArray()) }
            ?: throw Exception("فشل فتح دفق الكتابة")
        return uri
    }
    private fun addEntry(zos: ZipOutputStream, name: String, data: ByteArray) {
        zos.putNextEntry(ZipEntry(name)); zos.write(data); zos.closeEntry()
    }
}
