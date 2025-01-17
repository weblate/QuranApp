package com.quranapp.android.utils.reader.factory

import android.content.Context
import android.content.Intent
import com.quranapp.android.activities.ActivityEditShare
import com.quranapp.android.activities.ActivityReader
import com.quranapp.android.activities.ActivityReference
import com.quranapp.android.activities.ActivityTafsir
import com.quranapp.android.components.ReferenceVerseModel
import com.quranapp.android.components.quran.QuranMeta
import com.quranapp.android.components.readHistory.ReadHistoryModel
import com.quranapp.android.readerhandler.ReaderParams.READER_READ_TYPE_CHAPTER
import com.quranapp.android.readerhandler.ReaderParams.READER_READ_TYPE_JUZ
import com.quranapp.android.utils.tafsir.TafsirUtils
import com.quranapp.android.utils.univ.Keys
import com.quranapp.android.utils.univ.Keys.KEY_REFERENCE_VERSE_MODEL
import java.util.*

object ReaderFactory {
    @JvmStatic
    fun startEmptyReader(context: Context) {
        context.startActivity(Intent().setClass(context, ActivityReader::class.java))
    }

    @JvmStatic
    fun startJuz(context: Context, juzNo: Int) {
        context.startActivity(prepareJuzIntent(juzNo).setClass(context, ActivityReader::class.java))
    }

    @JvmStatic
    fun startChapter(context: Context, chapterNo: Int) {
        context.startActivity(
            prepareChapterIntent(chapterNo).setClass(
                context,
                ActivityReader::class.java
            )
        )
    }

    @JvmStatic
    fun startChapter(
        context: Context,
        translSlugs: Array<String>,
        saveTranslChanges: Boolean,
        chapterNo: Int
    ) {
        context.startActivity(
            prepareChapterIntent(translSlugs, saveTranslChanges, chapterNo).setClass(
                context,
                ActivityReader::class.java
            )
        )
    }

    @JvmStatic
    fun startVerse(context: Context, chapterNo: Int, verseNo: Int) {
        context.startActivity(
            prepareSingleVerseIntent(chapterNo, verseNo).setClass(
                context,
                ActivityReader::class.java
            )
        )
    }


    @JvmStatic
    fun startVerseRange(context: Context, chapterNo: Int, fromVerse: Int, toVerse: Int) {
        context.startActivity(
            prepareVerseRangeIntent(chapterNo, fromVerse, toVerse).setClass(
                context,
                ActivityReader::class.java
            )
        )
    }

    @JvmStatic
    fun startVerseRange(context: Context, chapterNo: Int, range: IntArray) {
        context.startActivity(
            prepareVerseRangeIntent(chapterNo, range).setClass(
                context,
                ActivityReader::class.java
            )
        )
    }

    @JvmStatic
    fun prepareJuzIntent(juzNo: Int): Intent {
        val intent = Intent()
        intent.putExtra(Keys.READER_KEY_READ_TYPE, 5)
        intent.putExtra(Keys.READER_KEY_JUZ_NO, juzNo)
        return intent
    }

    @JvmStatic
    fun prepareChapterIntent(chapterNo: Int): Intent {
        val intent = Intent()
        intent.putExtra(Keys.READER_KEY_READ_TYPE, 3)
        intent.putExtra(Keys.READER_KEY_CHAPTER_NO, chapterNo)
        return intent
    }

    @JvmStatic
    fun prepareChapterIntent(
        translSlugs: Array<String>,
        saveTranslChanges: Boolean,
        chapterNo: Int
    ): Intent {
        val intent = Intent()
        intent.putExtra(Keys.READER_KEY_READ_TYPE, 3)
        intent.putExtra(Keys.READER_KEY_CHAPTER_NO, chapterNo)
        intent.putExtra(Keys.READER_KEY_TRANSL_SLUGS, translSlugs)
        intent.putExtra(Keys.READER_KEY_SAVE_TRANSL_CHANGES, saveTranslChanges)
        return intent
    }

    @JvmStatic
    fun prepareSingleVerseIntent(chapterNo: Int, verseNo: Int): Intent {
        return prepareVerseRangeIntent(chapterNo, verseNo, verseNo)
    }


    @JvmStatic
    fun prepareVerseRangeIntent(chapterNo: Int, fromVerse: Int, toVerse: Int): Intent {
        return prepareVerseRangeIntent(chapterNo, intArrayOf(fromVerse, toVerse))
    }

    @JvmStatic
    fun prepareVerseRangeIntent(chapterNo: Int, range: IntArray): Intent {
        val intent = Intent()
        intent.putExtra(Keys.READER_KEY_READ_TYPE, 4)
        intent.putExtra(Keys.READER_KEY_CHAPTER_NO, chapterNo)
        intent.putExtra(Keys.READER_KEY_VERSES, range)
        return intent
    }


    @JvmStatic
    fun startReferenceVerse(
        context: Context,
        showChapterSugg: Boolean,
        title: String,
        desc: String,
        translSlug: Array<String>,
        chapters: List<Int>,
        verses: List<String>
    ) {
        val intent = prepareReferenceVerseIntent(showChapterSugg, title, desc, translSlug, chapters, verses)
        intent.setClass(context, ActivityReference::class.java)
        context.startActivity(intent)
    }


    @JvmStatic
    fun startReferenceVerse(context: Context, referenceVerseModel: ReferenceVerseModel) {
        val intent = prepareReferenceVerseIntent(referenceVerseModel)
        intent.setClass(context, ActivityReference::class.java)
        context.startActivity(intent)
    }


    @JvmStatic
    fun prepareReferenceVerseIntent(
        showChapterSugg: Boolean,
        title: String,
        desc: String,
        translSlug: Array<String>,
        chapters: List<Int>,
        verses: List<String>
    ): Intent {
        val referenceVerseModel = ReferenceVerseModel(showChapterSugg, title, desc, translSlug, chapters, verses)
        return prepareReferenceVerseIntent(referenceVerseModel)
    }


    @JvmStatic
    fun prepareReferenceVerseIntent(referenceVerseModel: ReferenceVerseModel): Intent {
        val intent = Intent()
        intent.putExtra(KEY_REFERENCE_VERSE_MODEL, referenceVerseModel)
        return intent
    }

    @JvmStatic
    fun prepareLastVersesIntent(
        quranMeta: QuranMeta,
        juzNo: Int,
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int,
        readType: Int,
        readerStyle: Int
    ): Intent? {
        var intent: Intent? = null
        if (readType == READER_READ_TYPE_CHAPTER && QuranMeta.isChapterValid(chapterNo)) {
            intent = prepareChapterIntent(chapterNo)
            intent.putExtra(Keys.READER_KEY_PENDING_SCROLL, intArrayOf(chapterNo, fromVerse))
        } else if (readType == READER_READ_TYPE_JUZ && QuranMeta.isJuzValid(juzNo)) {
            intent = prepareJuzIntent(juzNo)
            intent.putExtra(Keys.READER_KEY_PENDING_SCROLL, intArrayOf(chapterNo, fromVerse))
        } else if (quranMeta.isVerseRangeValid4Chapter(chapterNo, fromVerse, toVerse)) {
            intent = prepareVerseRangeIntent(chapterNo, fromVerse, toVerse)
        }

        if (intent != null) {
            if (readerStyle != -1) {
                intent.putExtra(Keys.READER_KEY_READER_STYLE, readerStyle)
            }
        }

        return intent
    }

    @JvmStatic
    fun prepareLastVersesIntent(quranMeta: QuranMeta, lastVersesModel: ReadHistoryModel): Intent? {
        return prepareLastVersesIntent(
            quranMeta,
            lastVersesModel.juzNo,
            lastVersesModel.chapterNo,
            lastVersesModel.fromVerseNo,
            lastVersesModel.toVerseNo,
            lastVersesModel.readType,
            lastVersesModel.readerStyle
        )
    }

    @JvmStatic
    fun startQuickEditShare(context: Context, chapterNo: Int, verseNo: Int) {
        val intent = Intent(context, ActivityEditShare::class.java)
        intent.putExtra(Keys.READER_KEY_CHAPTER_NO, chapterNo)
        intent.putExtra(Keys.READER_KEY_VERSE_NO, verseNo)
        context.startActivity(intent)
    }

    @JvmStatic
    fun prepareTafsirIntent(context: Context, chapterNo: Int, verseNo: Int): Intent {
        val intent = Intent(context, ActivityTafsir::class.java)
        val slug: String = if ("ur".equals(Locale.getDefault().language, ignoreCase = true)) {
            TafsirUtils.TAFSIR_SLUG_TAFSIR_IBN_KATHIR_UR
        } else {
            TafsirUtils.TAFSIR_SLUG_TAFSIR_IBN_KATHIR_EN
        }
        intent.putExtra(TafsirUtils.KEY_TAFSIR_SLUG, slug)
        intent.putExtra(Keys.READER_KEY_CHAPTER_NO, chapterNo)
        intent.putExtra(Keys.READER_KEY_VERSE_NO, verseNo)
        return intent
    }
}