package com.quranapp.android.views.recitation

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.TypefaceSpan
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.core.content.ContextCompat
import com.peacedesign.android.utils.DrawableUtils
import com.peacedesign.android.utils.WindowUtils
import com.quranapp.android.R
import com.quranapp.android.activities.readerSettings.ActivitySettings
import com.quranapp.android.databinding.LytAudioOptionExplorerBinding
import com.quranapp.android.databinding.LytRecitationMenuBinding
import com.quranapp.android.utils.extensions.disableView
import com.quranapp.android.utils.extensions.dp2px
import com.quranapp.android.utils.extensions.drawable
import com.quranapp.android.utils.reader.recitation.RecitationManager
import com.quranapp.android.utils.reader.recitation.RecitationUtils
import com.quranapp.android.utils.sharedPrefs.SPReader
import com.quranapp.android.utils.univ.PopupWindow2
import com.quranapp.android.utils.univ.RelativePopupWindow.HorizontalPosition
import com.quranapp.android.utils.univ.RelativePopupWindow.VerticalPosition
import com.quranapp.android.widgets.bottomSheet.PeaceBottomSheet

class RecitationPlayerMenu(private val player: RecitationPlayer) {
    private val popup = createPopup()
    private var mBinding: LytRecitationMenuBinding? = null

    init {
        val inflater = AsyncLayoutInflater(player.context)
        inflater.inflate(R.layout.lyt_recitation_menu, null) { view, _, _ ->
            mBinding = LytRecitationMenuBinding.bind(view)
            popup.contentView = mBinding!!.root

            setupView(mBinding!!)
        }
    }

    val context: Context get() = player.context

    private fun createPopup(): PopupWindow2 {
        return PopupWindow2().apply {
            width = context.dp2px(220f)
            height = ViewGroup.LayoutParams.WRAP_CONTENT

            isFocusable = true
            elevation = context.dp2px(3f).toFloat()

            val bgColor = ContextCompat.getColor(context, R.color.colorBGRecitationMenu)
            setBackgroundDrawable(DrawableUtils.createBackground(bgColor, context.dp2px(10f).toFloat()))
        }
    }

    private fun setupView(binding: LytRecitationMenuBinding) {
        binding.repeat.setOnClickListener { binding.repeatCheckbox.toggle() }
        binding.autoplay.setOnClickListener { binding.autoplayCheckbox.toggle() }
        binding.selectReciter.setOnClickListener {
            close()
            player.activity.mBinding.readerHeader.openReaderSetting(ActivitySettings.SETTINGS_RECITER)
        }

        val drawable = context.drawable(
            if (WindowUtils.isRTL(context)) R.drawable.dr_icon_chevron_left
            else R.drawable.dr_icon_chevron_right
        )

        binding.selectReciter.setDrawables(
            context.drawable(R.drawable.dr_icon_recitation),
            null,
            drawable,
            null
        )

        binding.manageAudio.setOnClickListener {
            close()
            Toast.makeText(context, "Coming soon, InshaAllah", Toast.LENGTH_SHORT).show()
        }

        binding.manageAudio.setDrawables(
            context.drawable(R.drawable.dr_icon_download),
            null,
            drawable,
            null
        )

        binding.audioOption.setOnClickListener {
            close()
            openAudioOptionSheet()
        }

        binding.audioOption.setDrawables(
            context.drawable(R.drawable.dr_icon_settings),
            null,
            drawable,
            null
        )

        binding.repeatCheckbox.setOnCheckedChangeListener { _, isChecked ->
            player.setRepeat(isChecked)
            binding.autoplay.disableView(isChecked)
        }

        binding.autoplayCheckbox.setOnCheckedChangeListener { _, isChecked ->
            player.setContinueChapter(isChecked)
        }
    }

    private fun setup(binding: LytRecitationMenuBinding) {
        val repeatEnabled = SPReader.getRecitationRepeatVerse(context)

        binding.repeatCheckbox.isChecked = repeatEnabled
        binding.autoplayCheckbox.isChecked = SPReader.getRecitationContinueChapter(context)
        binding.autoplay.disableView(repeatEnabled)
        binding.audioOption.text = prepareTitle(
            context.getString(R.string.audioOption),
            RecitationUtils.resolveAudioOptionText(context)
        )
        binding.selectReciter.text = prepareTitle(context.getString(R.string.strTitleSelectReciter), null)

        RecitationManager.prepare(context, false) {
            binding.selectReciter.text = prepareTitle(
                context.getString(R.string.strTitleSelectReciter),
                RecitationUtils.getReciterName(SPReader.getSavedRecitationSlug(context))
            )
        }
    }

    private fun prepareTitle(title: String, subtitle: String?): CharSequence {
        val ssb = SpannableStringBuilder(title)

        if (subtitle.isNullOrEmpty()) return ssb

        val flag = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

        return ssb.append("\n").append(SpannableString(subtitle).apply {
            setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.colorText2)), 0, subtitle.length, flag)
            setSpan(RelativeSizeSpan(0.93f), 0, subtitle.length, flag)
            setSpan(TypefaceSpan("sans-serif"), 0, subtitle.length, flag)
        })
    }

    private fun openAudioOptionSheet() {
        val sheetDialog = PeaceBottomSheet()
        sheetDialog.params.apply {
            headerTitle = context.getString(R.string.audioOption)
            contentView = LytAudioOptionExplorerBinding.inflate(player.activity.layoutInflater).apply {
                audioOption.check(RecitationUtils.resolveAudioOptionId(context))
                audioOption.onCheckChangedListener = { _, checkedId ->
                    val audioOption = RecitationUtils.resolveAudioOptionFromId(checkedId)
                    SPReader.setRecitationAudioOption(context, audioOption)
                    player.onAudioOptionChanged(audioOption)
                    sheetDialog.dismiss()
                }
            }.root
        }

        sheetDialog.show(player.activity.supportFragmentManager)
    }

    fun open(anchorView: View) {
        if (anchorView.windowToken != null) {
            if (mBinding != null) {
                setup(mBinding!!)
            }

            popup.showOnAnchor(anchorView, VerticalPosition.ABOVE, HorizontalPosition.ALIGN_RIGHT)
        }
    }

    fun close() {
        popup.dismiss()
    }
}