package com.quranapp.android.components.quran

import android.content.Context
import com.quranapp.android.utils.quran.parser.QuranDuaParser
import java.util.concurrent.atomic.AtomicReference

object QuranDua {
    private val sQuranDuaRef = AtomicReference<List<VerseReference>>()
    fun prepareInstance(
        context: Context,
        quranMeta: QuranMeta,
        callback: (List<VerseReference>) -> Unit
    ) {
        if (sQuranDuaRef.get() == null) {
            prepare(context, quranMeta, callback)
        } else {
            callback(sQuranDuaRef.get())
        }
    }

    private fun prepare(
        context: Context,
        quranMeta: QuranMeta,
        callback: (List<VerseReference>) -> Unit
    ) {
        QuranDuaParser.parseDua(
            context,
            quranMeta,
            sQuranDuaRef
        ) { callback(sQuranDuaRef.get()) }
    }
}
