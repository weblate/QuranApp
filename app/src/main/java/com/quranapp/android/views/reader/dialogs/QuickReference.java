/*
 * Created by Faisal Khan on (c) 29/8/2021.
 */

package com.quranapp.android.views.reader.dialogs;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.quranapp.android.utils.univ.RegexPattern.VERSE_RANGE_PATTERN;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.ViewUtils;
import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.peacedesign.android.widget.sheet.PeaceBottomSheet;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityReader;
import com.quranapp.android.activities.ReaderPossessingActivity;
import com.quranapp.android.activities.readerSettings.ActivitySettings;
import com.quranapp.android.adapters.ADPQuickReference;
import com.quranapp.android.components.bookmark.BookmarkModel;
import com.quranapp.android.components.quran.Quran;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.subcomponents.Chapter;
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo;
import com.quranapp.android.components.quran.subcomponents.Translation;
import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.databinding.LytSheetVerseReferenceBinding;
import com.quranapp.android.databinding.LytSheetVerseReferenceHeaderBinding;
import com.quranapp.android.interfaceUtils.BookmarkCallbacks;
import com.quranapp.android.interfaceUtils.Destroyable;
import com.quranapp.android.utils.Logger;
import com.quranapp.android.utils.quran.QuranUtils;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.thread.tasks.BaseCallableTask;
import com.quranapp.android.views.CardMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class QuickReference extends PeaceBottomSheet implements BookmarkCallbacks, Destroyable {
    private final CallableTaskRunner<List<Verse>> mTaskRunner = new CallableTaskRunner<>();
    private ReaderPossessingActivity mActivity;
    private LytSheetVerseReferenceBinding mBinding;

    private Set<String> mTranslSlugs;
    private int mChapterNo;
    private int[] mVerses;
    private int[] mVerseRange;
    private boolean mIsVerseRange;

    public QuickReference() {
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putStringArray("translSlugs", mTranslSlugs.toArray(new String[0]));
        outState.putInt("chapterNo", mChapterNo);
        outState.putIntArray("verses", mVerses);
        outState.putIntArray("verseRange", mVerseRange);
        outState.putBoolean("isVerseRange", mIsVerseRange);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mTranslSlugs = Arrays.stream(savedInstanceState.getStringArray("translSlugs")).collect(Collectors.toSet());
            mChapterNo = savedInstanceState.getInt("chapterNo");
            mVerses = savedInstanceState.getIntArray("verses");
            mVerseRange = savedInstanceState.getIntArray("verseRange");
            mIsVerseRange = savedInstanceState.getBoolean("isVerseRange");
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ReaderPossessingActivity) {
            mActivity = (ReaderPossessingActivity) context;
        }
    }

    @Override
    public void destroy() {
        mTaskRunner.cancel();

        mTranslSlugs = null;
        mChapterNo = -1;
        mIsVerseRange = false;
        mVerseRange = null;
        mVerses = null;

        if (mBinding == null) {
            return;
        }

        ViewUtils.removeView(mBinding.getRoot().findViewById(R.id.message));

        mBinding.referenceVerses.setAdapter(null);

        LytSheetVerseReferenceHeaderBinding header = mBinding.header;
        header.title.setText(null);
        header.btnBookmark.setVisibility(GONE);
        header.btnOpen.setVisibility(GONE);
    }

    @Override
    protected void setupDialogInternal(Dialog dialog, int style, PeaceBottomSheetParams params) {
        if (mActivity == null || (mIsVerseRange && mVerseRange == null) || (!mIsVerseRange && mVerses == null)) {
            return;
        }

        if (mBinding == null) {
            AsyncLayoutInflater inflater = new AsyncLayoutInflater(dialog.getContext());
            inflater.inflate(R.layout.lyt_sheet_verse_reference, null, (view, resid, parent) -> {
                mBinding = LytSheetVerseReferenceBinding.bind(view);
                setup(mActivity, mBinding);
                setupContent(mActivity, dialog, mBinding, params);
            });
        } else {
            setupContent(mActivity, dialog, mBinding, params);
        }
    }

    private void setupContent(ReaderPossessingActivity actvt, Dialog dialog,
                              LytSheetVerseReferenceBinding binding, PeaceBottomSheetParams params) {
        binding.referenceVerses.setVisibility(GONE);
        binding.loader.setVisibility(VISIBLE);

        ViewUtils.removeView(binding.getRoot());
        dialog.setContentView(binding.getRoot());
        setupDialogStyles(dialog, binding.getRoot(), params);


        if (mIsVerseRange) {
            initializeShowSingleVerseOrRange(actvt, binding, mTranslSlugs, mChapterNo, mVerseRange);
        } else {
            initializeShowVerses(actvt, binding, mTranslSlugs, mChapterNo, mVerses);
        }
    }

    private void setup(Context ctx, LytSheetVerseReferenceBinding binding) {
        binding.referenceVerses.setLayoutManager(new LinearLayoutManager(ctx));
        binding.header.closeReference.setOnClickListener(v -> dismiss());
        binding.header.getRoot().setElevation(Dimen.dp2px(ctx, 4));
    }

    private void initActions(ReaderPossessingActivity actvt, LytSheetVerseReferenceBinding binding, int chapterNo, int[] verseRange, boolean isRange) {
        LytSheetVerseReferenceHeaderBinding header = binding.header;
        if (!isRange) {
            header.btnBookmark.setVisibility(GONE);
            header.btnOpen.setVisibility(GONE);
            return;
        }

        header.btnBookmark.setVisibility(VISIBLE);
        header.btnOpen.setVisibility(VISIBLE);

        header.btnBookmark.setOnClickListener(v -> {
            if (chapterNo == -1 || verseRange == null) {
                return;
            }

            boolean isBookmarked = actvt.isBookmarked(chapterNo, verseRange[0], verseRange[1]);

            if (isBookmarked) {
                actvt.onBookmarkView(chapterNo, verseRange[0], verseRange[1], this);
            } else {
                actvt.addVerseToBookmark(chapterNo, verseRange[0], verseRange[1], this);
            }
        });

        header.btnOpen.setOnClickListener(v -> {
            if (chapterNo == -1 || verseRange == null) {
                return;
            }

            actvt.mActionController.openVerseReference(chapterNo, verseRange);
            dismiss();
        });
    }

    private void preShow(ReaderPossessingActivity actvt, LytSheetVerseReferenceHeaderBinding header, int chapterNo, int[] verseRange) {
        if (verseRange == null) {
            header.btnBookmark.setVisibility(GONE);
            return;
        }

        header.btnBookmark.setVisibility(VISIBLE);
        setupBookmarkIcon(header.btnBookmark, actvt.isBookmarked(chapterNo, verseRange[0], verseRange[1]));
    }

    private void initializeShowVerses(ReaderPossessingActivity actvt, LytSheetVerseReferenceBinding binding, Set<String> translSlugs, int chapterNo, int[] verses) {
        preShow(actvt, binding.header, chapterNo, null);
        makeReferenceVerses(actvt, binding, translSlugs, chapterNo, verses, false);
    }

    private void initializeShowSingleVerseOrRange(ReaderPossessingActivity actvt, LytSheetVerseReferenceBinding binding, Set<String> translSlugs, int chapterNo, int[] verseRange) {
        preShow(actvt, binding.header, chapterNo, verseRange);
        makeReferenceVerses(actvt, binding, translSlugs, chapterNo, verseRange, true);
    }

    private void setupBookmarkIcon(ImageView btnBookmark, boolean isBookmarked) {
        int filter = ContextCompat.getColor(getContext(), isBookmarked ? R.color.colorPrimary : R.color.colorIcon);
        btnBookmark.setColorFilter(filter);
        btnBookmark.setImageResource(isBookmarked ? R.drawable.dr_icon_bookmark_added : R.drawable.dr_icon_bookmark_outlined);
    }

    private void setupReferenceTitle(TextView titleView, int chapterNo, int[] verseInts, boolean isRange) {
        StringBuilder title = new StringBuilder("Quran ").append(chapterNo).append(":");
        if (isRange) {
            title.append(verseInts[0]);
            if (verseInts[0] != verseInts[1]) {
                title.append("-").append(verseInts[1]);
            }
        } else {
            for (int i = 0, l = verseInts.length; i < l; i++) {
                title.append(verseInts[i]);
                if (i < l - 1) {
                    title.append(", ");
                }
            }
        }
        titleView.setText(title.toString());
    }

    private void makeReferenceVerses(ReaderPossessingActivity actvt, LytSheetVerseReferenceBinding binding, Set<String> translSlugs,
                                     int chapterNo, int[] versesInts, boolean isRange) {
        QuranMeta.prepareInstance(actvt, quranMeta -> {
            if (!QuranMeta.isChapterValid(chapterNo)) {
                binding.loader.setVisibility(GONE);
                return;
            }
            // --
            Quran.prepareInstance(actvt, quranMeta, quran -> {
                makeReferenceVersesAsync(actvt, binding, quranMeta, quran,
                        translSlugs, chapterNo, versesInts, isRange);
            });
        });
    }

    private void makeReferenceVersesAsync(ReaderPossessingActivity actvt, LytSheetVerseReferenceBinding binding,
                                          QuranMeta quranMeta, Quran quran,
                                          Set<String> translSlugs, int chapterNo, int[] versesInts, boolean isRange) {
        mTaskRunner.cancel();

        mTaskRunner.callAsync(new BaseCallableTask<List<Verse>>() {
            @Override
            public void preExecute() {
                binding.referenceVerses.setVisibility(GONE);
                binding.loader.setVisibility(VISIBLE);
            }

            @Override
            public List<Verse> call() throws Exception {
                List<Verse> verses = new ArrayList<>();
                Chapter chapter = quran.getChapter(chapterNo);
                if (chapter == null) {
                    throw new Exception("could not get chapter object from quranMeta for the chapterNo. [QuickReference]");
                }

                Map<String, QuranTranslBookInfo> booksInfo = actvt.mTranslFactory.getTranslationBooksInfoValidated(
                        translSlugs);
                if (isRange) {
                    List<List<Translation>> transls = actvt.mTranslFactory.getTranslationsVerseRange(translSlugs, chapterNo,
                            versesInts[0], versesInts[1]);
                    QuranUtils.intRangeIterateWithIndex(versesInts, (index, verseNo) -> {
                        // --
                        prepareVerse(chapter, verseNo, verses, transls.get(index), booksInfo);
                    });
                } else {
                    List<List<Translation>> transls = actvt.mTranslFactory.getTranslationsDistinctVerses(translSlugs, chapterNo,
                            versesInts);
                    for (int i = 0, l = versesInts.length; i < l; i++) {
                        int verseNo = versesInts[i];
                        if (quranMeta.isVerseValid4Chapter(chapterNo, verseNo)) {
                            prepareVerse(chapter, verseNo, verses, transls.get(i), booksInfo);
                        }
                    }
                }

                return verses;
            }

            private void prepareVerse(Chapter chapter, int verseNo, List<Verse> verses,
                                      List<Translation> transls, Map<String, QuranTranslBookInfo> booksInfo) {
                Verse verse = chapter.getVerse(verseNo).copy();
                if (verse == null) {
                    return;
                }

                verse.setIncludeChapterNameInSerial(true);
                verse.setTranslations(transls);
                verse.setTranslTextSpannable(actvt.prepareTranslSpannable(verse, transls, booksInfo));

                verses.add(verse);
            }

            @Override
            public void onComplete(List<Verse> verses) {
                ADPQuickReference adp = new ADPQuickReference(actvt);
                adp.setVerses(verses);
                binding.referenceVerses.setAdapter(adp);

                setupReferenceTitle(binding.header.title, chapterNo, versesInts, isRange);
                initActions(actvt, binding, chapterNo, versesInts, isRange);


                ViewUtils.removeView(mBinding.getRoot().findViewById(R.id.message));
                if (translSlugs == null || translSlugs.isEmpty()) {
                    if (!(binding.getRoot().getChildAt(0) instanceof CardMessage)) {
                        CardMessage msgView = CardMessage.warning(actvt, R.string.strMsgTranslNoneSelected);
                        msgView.setId(R.id.message);
                        if (actvt instanceof ActivityReader) {
                            msgView.setActionText(actvt.str(R.string.strTitleSettings), () -> {
                                // --
                                ((ActivityReader) actvt).mBinding.readerHeader.openReaderSetting(ActivitySettings.SETTINGS_TRANSL);
                            });
                        }

                        int headerPos = binding.getRoot().indexOfChild(binding.header.getRoot());
                        binding.getRoot().addView(msgView, headerPos + 1);
                    }
                }
            }

            @Override
            public void onFailed(@NonNull Exception e) {
                super.onFailed(e);
                Logger.reportError(e);
            }

            @Override
            public void postExecute() {
                binding.loader.setVisibility(GONE);
                binding.referenceVerses.setVisibility(VISIBLE);
            }
        });
    }

    /**
     * @param versesStr could be of different patterns.
     *                  eg., -
     *                  7,8 (Verses)
     *                  7-8 (Verse range)
     *                  7 (Single verse)
     */
    public void show(ReaderPossessingActivity actvt, Set<String> translSlugs, int chapterNo, String versesStr) {
        if (TextUtils.isEmpty(versesStr)) {
            showReferenceChapter(actvt, translSlugs, chapterNo);
            return;
        }

        final int[] verseRange;

        String[] verses = versesStr.split(",");
        if (verses.length > 1) {
            int[] verseInts = Arrays.stream(verses).mapToInt(Integer::parseInt).sorted().toArray();
            showReferenceVerses(actvt, translSlugs, chapterNo, verseInts);
        } else {
            Matcher matcher = VERSE_RANGE_PATTERN.matcher(versesStr);
            MatchResult result;
            if (matcher.find() && (result = matcher.toMatchResult()).groupCount() >= 2) {
                final int fromVerse = Integer.parseInt(result.group(1));
                final int toVerse = Integer.parseInt(result.group(2));

                verseRange = new int[]{fromVerse, toVerse};
            } else {
                int verseNo = Integer.parseInt(versesStr);
                verseRange = new int[]{verseNo, verseNo};
            }

            showSingleVerseOrRange(actvt, translSlugs, chapterNo, verseRange);
        }
    }

    /**
     * Pattern <reference juz="\d+">()</reference>
     */
    private void showReferenceJuz(Set<String> translSlugs, int juzNo) {
    }

    /**
     * Pattern \<reference chapter="\d+">()</reference>
     */
    private void showReferenceChapter(ReaderPossessingActivity actvt, Set<String> translSlugs, int chapterNo) {
        QuranMeta quranMeta = actvt.mQuranMetaRef.get();

        if (!QuranMeta.isChapterValid(chapterNo)) {
            return;
        }

        PeaceDialog.Builder builder = PeaceDialog.newBuilder(actvt);
        builder.setTitle("Open chapter?");
        builder.setMessage("Surah " + quranMeta.getChapterName(actvt, chapterNo));
        builder.setTitleTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setMessageTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setNeutralButton(R.string.strLabelCancel, null);
        builder.setPositiveButton(R.string.strLabelOpen,
                (dialog, which) -> ReaderFactory.startChapter(actvt, translSlugs.toArray(new String[0]), false, chapterNo));
        builder.show();
    }

    /**
     * Call it to show verses of same chapter but with different numbers.
     *
     * @param verses comma separated verses from pattern <reference chapter="\d+" verses="\d+,\d+,...">()</reference>
     */
    private void showReferenceVerses(ReaderPossessingActivity actvt, Set<String> translSlugs, int chapterNo, int[] verses) {
        if (!QuranMeta.isChapterValid(chapterNo)) {
            return;
        }

        dismiss();

        mActivity = actvt;
        mTranslSlugs = translSlugs;
        mChapterNo = chapterNo;
        mVerses = verses;
        mIsVerseRange = false;

        show(actvt.getSupportFragmentManager());
    }

    /**
     * Call it show single verse of or a  verse range.
     *
     * @param verseRange contains two number. If both are same, means single verse otherwise a range.
     *                   Pattern <reference chapter="\d+" verses="\d+-\d+">()</reference>
     *                   or Pattern <reference chapter="\d+" verses="\d+">()</reference>
     */
    public void showSingleVerseOrRange(ReaderPossessingActivity actvt, Set<String> translSlugs, int chapterNo, int[] verseRange) {
        QuranUtils.correctVerseInRange(actvt.mQuranMetaRef.get(), chapterNo, verseRange);
        if (!QuranMeta.isChapterValid(chapterNo)
                || !actvt.mQuranMetaRef.get().isVerseRangeValid4Chapter(chapterNo, verseRange[0], verseRange[1])) {
            return;
        }
        dismiss();

        mActivity = actvt;
        mTranslSlugs = translSlugs;
        mChapterNo = chapterNo;
        mVerseRange = verseRange;
        mIsVerseRange = true;

        show(actvt.getSupportFragmentManager());
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (!isShowing()) {
            if (mBinding != null) {
                mBinding.referenceVerses.setAdapter(null);
            }
            mBinding = null;
        }
    }

    @Override
    public void dismiss() {
        try {
            dismissAllowingStateLoss();
        } catch (Exception ignored) {}
        destroy();
    }

    @Override
    public void onBookmarkRemoved(BookmarkModel model) {
        if (mBinding == null) {
            return;
        }
        setupBookmarkIcon(mBinding.header.btnBookmark, false);
    }

    @Override
    public void onBookmarkAdded(BookmarkModel model) {
        if (mBinding == null) {
            return;
        }
        setupBookmarkIcon(mBinding.header.btnBookmark, true);
    }
}
