/*
 * (c) Faisal Khan. Created on 2/2/2022.
 */

package com.quranapp.android.adapters.quranIndex;

import static android.view.ViewGroup.LayoutParams;
import static android.view.ViewGroup.MarginLayoutParams;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.ViewUtils;
import com.quranapp.android.R;
import com.quranapp.android.components.quran.QuranMeta;
import com.quranapp.android.frags.readerindex.BaseFragReaderIndex;
import com.quranapp.android.utils.reader.factory.ReaderFactory;
import com.quranapp.android.widgets.chapterCard.ChapterCard;

import java.util.ArrayList;
import java.util.List;

public class ADPChaptersList extends ADPReaderIndexBase<ADPChaptersList.VHChapter> {
    private List<Integer> mChapterNos = new ArrayList<>();

    public ADPChaptersList(BaseFragReaderIndex fragment, Context ctx, boolean reverse) {
        super(fragment, reverse);

        initADP(ctx);
    }

    @Override
    protected void prepareList(Context ctx, boolean reverse) {
        mChapterNos = new ArrayList<>();

        int from = reverse ? QuranMeta.totalChapters() : 1;
        int to = reverse ? 1 : QuranMeta.totalChapters();
        int chapterNo = from;

        while (true) {
            mChapterNos.add(chapterNo);

            if (reverse) {
                chapterNo--;
                if (chapterNo < to) break;
            } else {
                chapterNo++;
                if (chapterNo > to) break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return mChapterNos.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mChapterNos.get(position);
    }

    @NonNull
    @Override
    public VHChapter onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ChapterCard chapterCard = new ChapterCard(parent.getContext());

        chapterCard.setBackgroundResource(R.drawable.dr_bg_chapter_card);
        chapterCard.setElevation(Dimen.dp2px(parent.getContext(), 2));

        LayoutParams params = chapterCard.getLayoutParams();
        if (params instanceof MarginLayoutParams) {
            ViewUtils.setMargins((MarginLayoutParams) params, Dimen.dp2px(parent.getContext(), 5));
        }

        return new VHChapter(chapterCard);
    }

    @Override
    public void onBindViewHolder(@NonNull VHChapter holder, int position) {
        holder.bind(mChapterNos.get(position));
    }

    class VHChapter extends RecyclerView.ViewHolder {
        private ChapterCard mChapterCard;

        public VHChapter(@NonNull View itemView) {
            super(itemView);

            if (itemView instanceof ChapterCard) {
                mChapterCard = (ChapterCard) itemView;
            }
        }

        public void bind(int chapterNo) {
            setupChapter(chapterNo);
        }

        private void setupChapter(int chapterNo) {
            if (mChapterCard == null) {
                return;
            }

            mChapterCard.setChapterNumber(chapterNo);

            String chapterName = mFragment.getQuranMeta().getChapterName(itemView.getContext(), chapterNo);
            String nameTranslation = mFragment.getQuranMeta().getChapterNameTranslation(chapterNo);
            mChapterCard.setName(chapterName, nameTranslation);

            mChapterCard.setOnClickListener(v -> ReaderFactory.startChapter(v.getContext(), chapterNo));
        }
    }
}
