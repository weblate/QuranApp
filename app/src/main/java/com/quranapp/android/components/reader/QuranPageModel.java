package com.quranapp.android.components.reader;

import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.readerhandler.ReaderParams.RecyclerItemViewTypeConst;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class QuranPageModel {
    private int pageNo;
    private String chaptersName;
    private ArrayList<QuranPageSectionModel> sections;
    private int[] chaptersOnPage;
    private final Map<Integer, int[]> fromToVerses = new TreeMap<>();
    private int juzNo;
    private int scrollHighlightPendingChapterNo = -1;
    private int scrollHighlightPendingVerseNo = -1;
    @RecyclerItemViewTypeConst
    private int viewType;

    public QuranPageModel() {
    }

    public QuranPageModel(int pageNo, int juzNo, int[] chaptersOnPage, String chaptersName, ArrayList<QuranPageSectionModel> sections) {
        this.pageNo = pageNo;
        this.juzNo = juzNo;
        this.chaptersOnPage = chaptersOnPage;
        this.chaptersName = chaptersName;
        this.sections = sections;

        for (QuranPageSectionModel section : sections) {
            ArrayList<Verse> verses = section.getVerses();
            int[] verseNos = {verses.get(0).getVerseNo(), verses.get(verses.size() - 1).getVerseNo()};
            fromToVerses.put(section.getChapterNo(), verseNos);
        }
    }

    public int getPageNo() {
        return pageNo;
    }

    public ArrayList<QuranPageSectionModel> getSections() {
        return sections;
    }

    public int getJuzNo() {
        return juzNo;
    }

    public String getChaptersName() {
        return chaptersName;
    }

    public QuranPageModel setViewType(@RecyclerItemViewTypeConst int viewType) {
        this.viewType = viewType;
        return this;
    }

    @RecyclerItemViewTypeConst
    public int getViewType() {
        return viewType;
    }

    public boolean hasChapter(int chapterNo) {
        return chaptersOnPage[0] <= chapterNo && chapterNo <= chaptersOnPage[1];
    }

    public boolean hasVerse(int chapterNo, int verseNo) {
        boolean hasVerse = false;
        if (hasChapter(chapterNo)) {
            for (Integer chapterN : fromToVerses.keySet()) {
                int[] verses = fromToVerses.get(chapterN);
                if (verses != null) {
                    hasVerse = verses[0] <= verseNo && verseNo <= verses[1];
                    if (hasVerse) {
                        break;
                    }
                }
            }
        }
        return hasVerse;
    }

    public void setScrollHighlightPendingChapterNo(int scrollHighlightPendingChapterNo) {
        this.scrollHighlightPendingChapterNo = scrollHighlightPendingChapterNo;
    }

    public int getScrollHighlightPendingChapterNo() {
        return scrollHighlightPendingChapterNo;
    }

    public void setScrollHighlightPendingVerseNo(int scrollHighlightPendingVerseNo) {
        this.scrollHighlightPendingVerseNo = scrollHighlightPendingVerseNo;
    }

    public int getScrollHighlightPendingVerseNo() {
        return scrollHighlightPendingVerseNo;
    }
}
