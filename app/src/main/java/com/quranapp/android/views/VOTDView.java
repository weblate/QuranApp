package com.quranapp.android.views;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static com.quranapp.android.utils.reader.TranslUtils.TRANSL_TRANSLITERATION;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.ResUtils;
import com.peacedesign.android.utils.ViewUtils;
import com.peacedesign.android.utils.span.LineHeightSpan2;
import com.peacedesign.android.widget.dialog.loader.ProgressDialog;
import com.quranapp.android.R;
import com.quranapp.android.activities.ActivityBookmark;
import com.quranapp.android.components.bookmark.BookmarkModel;
import com.quranapp.android.components.quran.Quran;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.components.quran.subcomponents.Chapter;
import com.quranapp.android.components.quran.subcomponents.QuranTranslBookInfo;
import com.quranapp.android.components.quran.subcomponents.Translation;
import com.quranapp.android.components.quran.subcomponents.Verse;
import com.quranapp.android.databinding.LytVotdBinding;
import com.quranapp.android.databinding.LytVotdContentBinding;
import com.quranapp.android.db.bookmark.BookmarkDBHelper;
import com.quranapp.android.interfaceUtils.BookmarkCallbacks;
import com.quranapp.android.interfaceUtils.Destroyable;
import com.quranapp.android.readerhandler.VerseDecorator;
import com.quranapp.android.suppliments.BookmarkViewer;
import com.quranapp.android.utils.reader.ScriptUtils;
import com.quranapp.android.utils.reader.TranslUtils;
import com.quranapp.android.utils.reader.factory.QuranTranslFactory;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.utils.sp.SPReader;
import com.quranapp.android.utils.thread.runner.CallableTaskRunner;
import com.quranapp.android.utils.thread.tasks.BaseCallableTask;
import com.quranapp.android.utils.univ.StringUtils;
import com.quranapp.android.utils.verse.VerseUtils;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class VOTDView extends FrameLayout implements Destroyable, BookmarkCallbacks {
    private final CallableTaskRunner<Pair<QuranTranslBookInfo, Translation>> taskRunner = new CallableTaskRunner<>();
    private final BookmarkDBHelper mBookmarkDBHelper;
    private final BookmarkViewer mBookmarkViewer;
    private final LytVotdBinding mBinding;
    private final VerseDecorator mVerseDecorator;
    private final int mColorNormal;
    private final int mColorBookmarked;
    private LytVotdContentBinding mContent;
    private int mChapterNo = -1;
    private int mVerseNo = -1;
    private String mLastScript;
    private String mLastTranslSlug;
    private CharSequence mArText;

    public VOTDView(@NonNull Context context) {
        this(context, null);
    }

    public VOTDView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VOTDView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mBookmarkDBHelper = new BookmarkDBHelper(context);
        mBookmarkViewer = new BookmarkViewer(context, new AtomicReference<>(null), mBookmarkDBHelper, this);
        mVerseDecorator = new VerseDecorator(context);
        mColorNormal = ContextCompat.getColor(context, R.color.white3);
        mColorBookmarked = ContextCompat.getColor(context, R.color.colorPrimary);

        mBinding = LytVotdBinding.inflate(LayoutInflater.from(context));
        mBinding.getRoot().setVisibility(GONE);

        addView(mBinding.getRoot());
    }

    private void initContent() {
        if (mContent == null) {
            mContent = LytVotdContentBinding.inflate(LayoutInflater.from(getContext()));
            mBinding.container.addView(mContent.getRoot());
        }
    }

    private void initActions(QuranMeta quranMeta) {
        mBinding.read.setVisibility(VISIBLE);

        int pad = Dimen.dp2px(getContext(), 5);

        /*mContent.btnQuickEdit.setImageResource(R.drawable.dr_icon_quick_edit);
        ViewUtils.setPaddings(mContent.btnQuickEdit, pad);*/
        ViewUtils.setPaddings(mContent.votdBookmark, pad);

        /*mContent.btnQuickEdit.setOnClickListener(v -> ReaderFactory.startQuickEditShare(getContext(), mChapterNo, mVerseNo));*/
        mContent.votdBookmark.setOnClickListener(v -> bookmark(mChapterNo, mVerseNo));
        mContent.votdBookmark.setOnLongClickListener(v -> {
            v.getContext().startActivity(new Intent(v.getContext(), ActivityBookmark.class));
            return true;
        });

        mBinding.read.setOnClickListener(v -> {
            if (!QuranMeta.isChapterValid(mChapterNo) || !quranMeta.isVerseValid4Chapter(mChapterNo, mVerseNo)) {
                return;
            }

            ReaderFactory.startVerse(getContext(), mChapterNo, mVerseNo);
        });
    }

    @Override
    public void destroy() {
        mBookmarkDBHelper.close();
    }

    public synchronized void refresh(QuranMeta quranMeta) {
        if (mBookmarkViewer != null) {
            mBookmarkViewer.setQuranMeta(quranMeta);
        }

        installVotdContents(quranMeta);
    }

    private void initVotd(QuranMeta quranMeta, Quran quran, Runnable runnable) {
        mBinding.getRoot().setVisibility(GONE);
        initActions(quranMeta);

        VerseUtils.getVOTD(getContext(), quranMeta, quran, (chapterNo, verseNo) -> {
            mChapterNo = chapterNo;
            mVerseNo = verseNo;
            if (runnable != null) runnable.run();
        });
    }

    public void installVotdContents(QuranMeta quranMeta) {
        initContent();

        if (!QuranMeta.isChapterValid(mChapterNo) || !quranMeta.isVerseValid4Chapter(mChapterNo, mVerseNo)) {
            initVotd(quranMeta, null, () -> installVotdContents(quranMeta));
            return;
        }

        setupVOTD(quranMeta);
        mBinding.getRoot().setVisibility(VISIBLE);
    }

    private void setupVOTD(QuranMeta quranMeta) {
        if (mChapterNo == -1 || mVerseNo == -1) {
            return;
        }

        prepareQuran(getContext(), quranMeta);
        setupVOTDBookmarkIcon(mBookmarkDBHelper.isBookmarked(mChapterNo, mVerseNo, mVerseNo));
    }

    private void prepareQuran(Context context, QuranMeta quranMeta) {
        if (!Objects.equals(mLastScript, SPReader.getSavedScript(context))) {
            Quran.prepareInstance(context, quranMeta, quran -> setupQuran(quranMeta, quran));
        } else {
            prepareTransl(getContext());
        }
    }

    private void setupQuran(QuranMeta quranMeta, Quran quran) {
        if (!quran.getVerse(mChapterNo, mVerseNo).isIdealForVOTD()) {
            initVotd(quranMeta, quran, () -> installVotdContents(quranMeta));
            return;
        }
        mLastScript = quran.getScript();

        Chapter chapter = quran.getChapter(mChapterNo);

        String info = getContext().getString(R.string.strLabelVerseWithChapNameAndNo, chapter.getName(), mChapterNo, mVerseNo);
        mContent.verseInfo.setText(info);

        mVerseDecorator.onSharedPrefChanged();

        final int txtSizeRes = ScriptUtils.getScriptTextSizeSmall2Res(quran.getScript());
        int textSize = ResUtils.getDimenPx(getContext(), txtSizeRes);

        Verse verse = quran.getChapter(mChapterNo).getVerse(mVerseNo);
        mArText = mVerseDecorator.setupArabicText(verse.getArabicText(), mVerseNo, textSize);
        prepareTransl(getContext());
    }

    private void prepareTransl(Context context) {
        AtomicReference<ProgressDialog> progressDialog = new AtomicReference<>();

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = () -> {
            ProgressDialog dialog = new ProgressDialog(context);
            progressDialog.set(dialog);
            dialog.show();
        };

        taskRunner.callAsync(new BaseCallableTask<Pair<QuranTranslBookInfo, Translation>>() {
            QuranTranslFactory factory;

            @Override
            public void preExecute() {
                factory = new QuranTranslFactory(context);
                handler.postDelayed(runnable, 1500);
            }

            @Override
            public void postExecute() {
                if (factory != null) factory.close();
                if (progressDialog.get() != null) progressDialog.get().dismiss();

                handler.removeCallbacks(runnable);
            }

            @Override
            public Pair<QuranTranslBookInfo, Translation> call() {
                QuranTranslBookInfo bookInfo = obtainOptimalSlug(context, factory);
                if (Objects.equals(mLastTranslSlug, bookInfo.getSlug())) {
                    return null;
                }

                Translation translation = factory.getTranslationsSingleSlugVerse(bookInfo.getSlug(), mChapterNo, mVerseNo);
                return new Pair<>(bookInfo, translation);
            }

            @Override
            public void onComplete(@Nullable Pair<QuranTranslBookInfo, Translation> result) {
                if (result != null) {
                    setupTranslation(mArText, result.first, result.second);
                }
            }
        });
    }

    private QuranTranslBookInfo obtainOptimalSlug(Context ctx, QuranTranslFactory factory) {
        Set<String> savedTranslations = SPReader.getSavedTranslations(ctx);

        QuranTranslBookInfo bookInfo = null;
        for (String savedSlug : savedTranslations) {
            if (!TRANSL_TRANSLITERATION.equals(savedSlug)) {
                bookInfo = factory.getTranslationBookInfo(savedSlug);
                break;
            }
        }

        if (bookInfo == null) {
            bookInfo = factory.getTranslationBookInfo(TranslUtils.TRANSL_SLUG_DEFAULT);
        }

        return bookInfo;
    }

    private void setupTranslation(CharSequence mArText, QuranTranslBookInfo bookInfo, Translation translation) {
        mLastTranslSlug = translation.getBookSlug();

        if (TextUtils.isEmpty(translation.getText())) {
            mContent.text.setVisibility(View.GONE);
            return;
        }

        String transl = StringUtils.removeHTML(translation.getText(), false);
        int txtSize = ResUtils.getDimenPx(getContext(), R.dimen.dmnCommonSize1_5);
        SpannableString translText = mVerseDecorator.setupTranslText(transl, -1, txtSize, translation.isUrdu());

        SpannableString authorText = null;
        String author = bookInfo.getDisplayName(true);
        if (!TextUtils.isEmpty(author)) {
            author = StringUtils.HYPHEN + " " + author;
            authorText = mVerseDecorator.setupAuthorText(author, translation.isUrdu());
        }

        showText(mArText, translText, authorText);
    }

    private void showText(CharSequence arText, SpannableString transl, SpannableString author) {
        SpannableStringBuilder sb = new SpannableStringBuilder();

        if (!TextUtils.isEmpty(arText)) {
            sb.append(arText);
        }

        if (!TextUtils.isEmpty(transl)) {
            if (!TextUtils.isEmpty(arText)) {
                sb.append("\n\n");
            }
            sb.append(transl);
        }

        if (!TextUtils.isEmpty(author)) {
            if (!TextUtils.isEmpty(transl) || !TextUtils.isEmpty(arText)) {
                sb.append("\n");
            }
            author.setSpan(new LineHeightSpan2(15, true, false), 0, author.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append(author);
        }

        mContent.text.setText(sb, TextView.BufferType.SPANNABLE);

        ViewUtils.removeView(findViewById(R.id.loader));
    }

    private void setupVOTDBookmarkIcon(boolean isBookmarked) {
        int iconRes = isBookmarked ? R.drawable.dr_icon_bookmark_added : R.drawable.dr_icon_bookmark_outlined;
        mContent.votdBookmark.setImageResource(iconRes);
        mContent.votdBookmark.setColorFilter(isBookmarked ? mColorBookmarked : mColorNormal);
    }

    private void bookmark(int chapterNo, int verseNo) {
        boolean isBookmarked = mBookmarkDBHelper.isBookmarked(chapterNo, verseNo, verseNo);

        if (isBookmarked) {
            mBookmarkViewer.view(chapterNo, verseNo, verseNo);
        } else {
            mBookmarkDBHelper.addToBookmark(chapterNo, verseNo, verseNo, null, model -> {
                setupVOTDBookmarkIcon(true);
                mBookmarkViewer.edit(model);
            });
        }
    }

    @Override
    public void onBookmarkRemoved(BookmarkModel model) {
        setupVOTDBookmarkIcon(false);
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        if (params instanceof MarginLayoutParams) {
            ((MarginLayoutParams) params).bottomMargin = Dimen.dp2px(getContext(), 2);
        }
        super.setLayoutParams(params);
    }
}
