package com.quranapp.android.readerhandler;

import static com.quranapp.android.readerhandler.ReaderParams.RecyclerItemViewType.BISMILLAH;
import static com.quranapp.android.readerhandler.ReaderParams.RecyclerItemViewType.CHAPTER_INFO;
import static com.quranapp.android.readerhandler.ReaderParams.RecyclerItemViewType.CHAPTER_TITLE;
import static com.quranapp.android.readerhandler.ReaderParams.RecyclerItemViewType.IS_VOTD;
import static com.quranapp.android.readerhandler.ReaderParams.RecyclerItemViewType.NO_TRANSL_SELECTED;
import static com.quranapp.android.readerhandler.ReaderParams.RecyclerItemViewType.READER_FOOTER;
import static com.quranapp.android.readerhandler.ReaderParams.RecyclerItemViewType.READER_PAGE;
import static com.quranapp.android.readerhandler.ReaderParams.RecyclerItemViewType.VERSE;

import android.content.Context;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.quranapp.android.activities.ReaderPossessingActivity;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.subcomponents.Chapter;
import com.quranapp.android.utils.quran.QuranUtils;
import com.quranapp.android.utils.sp.SPReader;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

public class ReaderParams {
    public static final int READER_STYLE_TRANSLATION = 0x1;
    public static final int READER_STYLE_READING = 0x2;
    public static final int READER_STYLE_DEFAULT = READER_STYLE_TRANSLATION;

    public static final int READER_READ_TYPE_CHAPTER = 0x3;

    public static final int READER_READ_TYPE_VERSES = 0x4;
    public static final int READER_READ_TYPE_JUZ = 0x5;

    private final ReaderPossessingActivity mActivity;

    @ReaderStyle
    private int readerStyle = READER_STYLE_TRANSLATION;
    @ReaderReadType
    public int readType = READER_READ_TYPE_CHAPTER;
    public String readerScript;
    public float arTextSizeMult;
    public float translTextSizeMult;
    public Chapter currChapter;
    public int currJuzNo;
    public int[] verseRange;
    private Set<String> visibleTranslSlugs;
    public boolean saveTranslChanges;

    public ReaderParams(ReaderPossessingActivity activity) {
        mActivity = activity;
    }

    public void setCurrChapter(Chapter chapter) {
        currChapter = chapter;
    }

    public int getReaderStyle() {
        return readerStyle;
    }

    public void setReaderStyle(Context context, int readerStyle) {
        this.readerStyle = readerStyle;
        SPReader.setSavedReaderStyle(context, readerStyle);
    }

    @Nullable
    public Set<String> getVisibleTranslSlugs() {
        return visibleTranslSlugs;
    }

    public void setVisibleTranslSlugs(Set<String> slugs) {
        visibleTranslSlugs = slugs;
    }

    public boolean isSingleVerse() {
        return readType == READER_READ_TYPE_VERSES && QuranUtils.doesRangeDenoteSingle(verseRange);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isVerseInValidRange(int chapterNo, int verseNo) {
        QuranMeta quranMeta = mActivity.mQuranMetaRef.get();
        switch (readType) {
            case READER_READ_TYPE_VERSES:
            case READER_READ_TYPE_CHAPTER:
                return currChapter.getChapterNumber() == chapterNo && quranMeta.isVerseValid4Chapter(chapterNo, verseNo);
            case READER_READ_TYPE_JUZ:
                return quranMeta.isVerseValid4Juz(currJuzNo, chapterNo, verseNo);
        }

        return false;
    }

    public int defaultStyle(Context context) {
        return SPReader.getSavedReaderStyle(context);
    }

    public int defaultReadType() {
        return READER_READ_TYPE_CHAPTER;
    }

    public void resetTextSizesStates() {
        arTextSizeMult = SPReader.getSavedTextSizeMultTransl(mActivity);
        translTextSizeMult = SPReader.getSavedTextSizeMultTransl(mActivity);
    }


    @IntDef({READER_STYLE_TRANSLATION, READER_STYLE_READING})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ReaderStyle {}

    @IntDef({READER_READ_TYPE_CHAPTER, READER_READ_TYPE_VERSES, READER_READ_TYPE_JUZ})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ReaderReadType {}

    @IntDef({BISMILLAH, CHAPTER_TITLE, VERSE, READER_FOOTER, READER_PAGE, CHAPTER_INFO, IS_VOTD, NO_TRANSL_SELECTED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RecyclerItemViewTypeConst {}

    public static class RecyclerItemViewType {
        private RecyclerItemViewType() {
        }

        public static final int BISMILLAH = 0x0;
        public static final int CHAPTER_TITLE = 0x1;
        public static final int VERSE = 0x2;
        public static final int READER_FOOTER = 0x3;
        public static final int READER_PAGE = 0x4;
        public static final int CHAPTER_INFO = 0x5;
        public static final int IS_VOTD = 0x6;
        public static final int NO_TRANSL_SELECTED = 0x7;
    }
}
