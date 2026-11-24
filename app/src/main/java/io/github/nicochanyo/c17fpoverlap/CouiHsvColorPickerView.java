package io.github.nicochanyo.c17fpoverlap;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * Composite ColorOS 17 styled HSV (+ optional Alpha) color picker containing a capsule
 * preview bar, Hue / Saturation / Value (/ Alpha) donut sliders, and tappable HEX readout.
 */
public class CouiHsvColorPickerView extends LinearLayout {
    public interface OnColorChangeListener {
        void onColorChanged(int argbColor, boolean fromUser);

        void onTrackingStopped(int argbColor);
    }

    private CouiColorSliderView previewBar;
    private CouiColorSliderView hueBar;
    private CouiColorSliderView satBar;
    private CouiColorSliderView valBar;
    private CouiColorSliderView alphaBar;
    private LinearLayout hexRow;
    private TextView tvHexValue;

    private final float[] hsvTmp = new float[3];
    private float hue = 199f;
    private float sat = 0.68f;
    private float val = 0.97f;
    private int alpha = 255;
    private boolean showAlpha;
    private int currentArgb = ConfigStore.DEF_CUSTOM_COLOR;
    private boolean initialized;
    private boolean suppressSliderCallbacks;
    private OnColorChangeListener listener;

    public CouiHsvColorPickerView(Context context) {
        this(context, null);
    }

    public CouiHsvColorPickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CouiHsvColorPickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context ctx) {
        setOrientation(VERTICAL);
        setClipChildren(false);
        setClipToPadding(false);

        float d = ctx.getResources().getDisplayMetrics().density;

        previewBar = new CouiColorSliderView(ctx);
        previewBar.setMode(CouiColorSliderView.MODE_PREVIEW);
        addView(previewBar, new LayoutParams(
                LayoutParams.MATCH_PARENT, Math.round(30f * d)));

        hueBar = createSlider(ctx, CouiColorSliderView.MODE_HUE, Math.round(10f * d), d);
        satBar = createSlider(ctx, CouiColorSliderView.MODE_SATURATION, Math.round(6f * d), d);
        valBar = createSlider(ctx, CouiColorSliderView.MODE_VALUE, Math.round(6f * d), d);
        alphaBar = createSlider(ctx, CouiColorSliderView.MODE_ALPHA, Math.round(6f * d), d);
        alphaBar.setVisibility(GONE);

        boolean livingFlow = ConfigStore.isLivingFlowUi(ctx);
        int textPrimary = livingFlow
                ? ctx.getColor(R.color.lf_text_primary)
                : ConfigStore.resolveThemeColor(ctx, R.attr.couiTextPrimary, R.color.coui_text_primary);
        int textSecondary = livingFlow
                ? ctx.getColor(R.color.lf_text_secondary)
                : ConfigStore.resolveThemeColor(ctx, R.attr.couiTextSecondary, R.color.coui_text_secondary);

        hexRow = new LinearLayout(ctx);
        hexRow.setOrientation(VERTICAL);
        hexRow.setBackgroundResource(livingFlow ? R.drawable.bg_lf_list_item_press : R.drawable.bg_coui_item_ripple);
        hexRow.setClickable(true);
        hexRow.setFocusable(true);
        hexRow.setPadding(Math.round(2f * d), Math.round(6f * d), Math.round(2f * d), Math.round(4f * d));
        LayoutParams hexLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        hexLp.topMargin = Math.round(12f * d);
        addView(hexRow, hexLp);

        TextView tvLabel = new TextView(ctx);
        tvLabel.setText(R.string.label_color_value);
        tvLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        tvLabel.setTextColor(textSecondary);
        hexRow.addView(tvLabel, new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        LinearLayout valRow = new LinearLayout(ctx);
        valRow.setOrientation(HORIZONTAL);
        valRow.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams valRowLp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        valRowLp.topMargin = Math.round(2f * d);
        hexRow.addView(valRow, valRowLp);

        tvHexValue = new TextView(ctx);
        tvHexValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21f);
        tvHexValue.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tvHexValue.setTextColor(textPrimary);
        valRow.addView(tvHexValue, new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        ImageView ivEdit = new ImageView(ctx);
        ivEdit.setImageResource(R.drawable.ic_chip_edit);
        ivEdit.setColorFilter(textSecondary, PorterDuff.Mode.SRC_IN);
        LayoutParams editLp = new LayoutParams(Math.round(14f * d), Math.round(14f * d));
        editLp.setMarginStart(Math.round(6f * d));
        valRow.addView(ivEdit, editLp);

        hexRow.setOnClickListener(v -> {
            if (!isEnabled()) {
                return;
            }
            openHexInputDialog();
        });

        attachChannelListeners();
        syncChildren();
    }

    private CouiColorSliderView createSlider(Context ctx, int mode, int topMarginPx, float d) {
        CouiColorSliderView slider = new CouiColorSliderView(ctx);
        slider.setMode(mode);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, Math.round(36f * d));
        lp.topMargin = topMarginPx;
        addView(slider, lp);
        return slider;
    }

    private void attachChannelListeners() {
        hueBar.setOnValueChangeListener(new CouiColorSliderView.OnValueChangeListener() {
            @Override
            public void onValueChanged(float value, boolean fromUser) {
                if (!fromUser || suppressSliderCallbacks) {
                    return;
                }
                hue = clampHue(value);
                recomputeArgbAndNotify();
            }

            @Override
            public void onTrackingStopped(float value) {
                if (listener != null) {
                    listener.onTrackingStopped(currentArgb);
                }
            }
        });

        satBar.setOnValueChangeListener(new CouiColorSliderView.OnValueChangeListener() {
            @Override
            public void onValueChanged(float value, boolean fromUser) {
                if (!fromUser || suppressSliderCallbacks) {
                    return;
                }
                sat = clamp01(value);
                recomputeArgbAndNotify();
            }

            @Override
            public void onTrackingStopped(float value) {
                if (listener != null) {
                    listener.onTrackingStopped(currentArgb);
                }
            }
        });

        valBar.setOnValueChangeListener(new CouiColorSliderView.OnValueChangeListener() {
            @Override
            public void onValueChanged(float value, boolean fromUser) {
                if (!fromUser || suppressSliderCallbacks) {
                    return;
                }
                val = clamp01(value);
                recomputeArgbAndNotify();
            }

            @Override
            public void onTrackingStopped(float value) {
                if (listener != null) {
                    listener.onTrackingStopped(currentArgb);
                }
            }
        });

        alphaBar.setOnValueChangeListener(new CouiColorSliderView.OnValueChangeListener() {
            @Override
            public void onValueChanged(float value, boolean fromUser) {
                if (!fromUser || suppressSliderCallbacks) {
                    return;
                }
                alpha = Math.max(0, Math.min(255, Math.round(clamp01(value) * 255f)));
                recomputeArgbAndNotify();
            }

            @Override
            public void onTrackingStopped(float value) {
                if (listener != null) {
                    listener.onTrackingStopped(currentArgb);
                }
            }
        });
    }

    public void setShowAlpha(boolean showAlpha) {
        this.showAlpha = showAlpha;
        alphaBar.setVisibility(showAlpha ? VISIBLE : GONE);
        previewBar.setShowCheckerboard(showAlpha);
        if (!showAlpha) {
            alpha = 255;
            currentArgb = 0xFF000000 | (currentArgb & 0x00FFFFFF);
        }
        syncChildren();
    }

    public void setColor(int argb) {
        int normalized = showAlpha ? argb : (0xFF000000 | (argb & 0x00FFFFFF));
        if (initialized && normalized == currentArgb) {
            syncChildren();
            return;
        }
        initialized = true;
        currentArgb = normalized;
        alpha = showAlpha ? Color.alpha(normalized) : 255;
        Color.colorToHSV(normalized | 0xFF000000, hsvTmp);
        if (hsvTmp[2] > 0.0005f) {
            if (hsvTmp[1] > 0.0005f) {
                hue = hsvTmp[0];
            }
            sat = hsvTmp[1];
        }
        val = hsvTmp[2];
        syncChildren();
    }

    public int getColor() {
        return currentArgb;
    }

    public void setOnColorChangeListener(OnColorChangeListener listener) {
        this.listener = listener;
    }

    private void recomputeArgbAndNotify() {
        initialized = true;
        hsvTmp[0] = hue;
        hsvTmp[1] = sat;
        hsvTmp[2] = val;
        int outAlpha = showAlpha ? alpha : 255;
        currentArgb = Color.HSVToColor(outAlpha, hsvTmp);
        syncChildren();
        if (listener != null) {
            listener.onColorChanged(currentArgb, true);
        }
    }

    private void syncChildren() {
        suppressSliderCallbacks = true;
        try {
            previewBar.setPreviewColor(currentArgb);

            hueBar.setPreviewColor(currentArgb);
            hueBar.setHsvContext(hue, sat, val, alpha);
            hueBar.setValue(hue);

            satBar.setPreviewColor(currentArgb);
            satBar.setHsvContext(hue, sat, val, alpha);
            satBar.setValue(sat);

            valBar.setPreviewColor(currentArgb);
            valBar.setHsvContext(hue, sat, val, alpha);
            valBar.setValue(val);

            alphaBar.setPreviewColor(currentArgb);
            alphaBar.setHsvContext(hue, sat, val, alpha);
            alphaBar.setValue(alpha / 255f);

            tvHexValue.setText(formatHex());
        } finally {
            suppressSliderCallbacks = false;
        }
    }

    private String formatHex() {
        if (showAlpha) {
            return String.format(Locale.US, "#%08X", currentArgb);
        }
        return String.format(Locale.US, "#%06X", currentArgb & 0x00FFFFFF);
    }

    private void openHexInputDialog() {
        final Context ctx = getContext();
        CouiBottomSheet.Palette pal = CouiBottomSheet.Palette.fromModuleTheme(ctx);
        final CouiBottomSheet.SheetHandle sheet = CouiBottomSheet.create(ctx, pal);
        final Runnable[] confirmRef = new Runnable[1];
        String hint = ctx.getString(
                showAlpha ? R.string.dialog_hex_alpha_input_hint : R.string.dialog_hex_input_hint);
        if (pal.livingFlow) {
            float d = ctx.getResources().getDisplayMetrics().density;
            CouiBottomSheet.addLivingFlowTopBar(
                    ctx, sheet,
                    ctx.getString(R.string.label_color_value),
                    ctx.getString(R.string.btn_cancel),
                    ctx.getString(R.string.btn_confirm),
                    1,
                    v -> {
                        if (confirmRef[0] != null) {
                            confirmRef[0].run();
                        }
                    });
            android.widget.TextView tvHint = new android.widget.TextView(ctx);
            tvHint.setText(hint + "  ·  " + formatHex());
            tvHint.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f);
            tvHint.setLineSpacing(0f, 1.25f);
            tvHint.setTextColor(pal.textSecondary);
            tvHint.setPadding(Math.round(4f * d), 0, Math.round(4f * d), 0);
            LinearLayout.LayoutParams hintLp = new LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            hintLp.topMargin = Math.round(2f * d);
            hintLp.bottomMargin = Math.round(10f * d);
            sheet.card.addView(tvHint, hintLp);
        } else {
            CouiBottomSheet.addHeader(
                    ctx, sheet.card, pal,
                    ctx.getString(R.string.label_color_value),
                    hint,
                    formatHex());
        }

        String rawHex = showAlpha
                ? String.format(Locale.US, "%08X", currentArgb)
                : String.format(Locale.US, "%06X", currentArgb & 0x00FFFFFF);
        final EditText et = pal.livingFlow
                ? CouiBottomSheet.addLivingFlowBorderlessInputCard(
                        ctx, sheet.card, pal, rawHex, showAlpha ? "FF4FC3F7" : "4FC3F7")
                : CouiBottomSheet.addPillInputBox(
                        ctx, sheet.card, pal, rawHex, showAlpha ? "FF4FC3F7" : "4FC3F7");
        et.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        et.setTypeface(Typeface.MONOSPACE);
        et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(showAlpha ? 9 : 7)});
        et.setSelection(0, et.getText() != null ? et.getText().length() : 0);

        final Runnable submit = new Runnable() {
            @Override
            public void run() {
                String text = et.getText() != null ? et.getText().toString().trim() : "";
                if (text.startsWith("#")) {
                    text = text.substring(1).trim();
                }
                Integer parsed = null;
                try {
                    if (text.length() == 6) {
                        int rgb = Integer.parseInt(text, 16) & 0x00FFFFFF;
                        parsed = showAlpha ? ((alpha << 24) | rgb) : (0xFF000000 | rgb);
                    } else if (showAlpha && text.length() == 8) {
                        parsed = (int) Long.parseLong(text, 16);
                    }
                } catch (Throwable ignored) {
                }
                sheet.dialog.dismiss();
                if (parsed != null) {
                    setColor(parsed);
                    if (listener != null) {
                        listener.onColorChanged(currentArgb, true);
                        listener.onTrackingStopped(currentArgb);
                    }
                }
            }
        };

        et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    submit.run();
                    return true;
                }
                return false;
            }
        });

        if (pal.livingFlow) {
            confirmRef[0] = submit;
        } else {
            CouiBottomSheet.addButtonRow(
                    ctx, sheet.card, pal,
                    ctx.getString(R.string.btn_cancel), 0, v -> sheet.dialog.dismiss(),
                    ctx.getString(R.string.btn_confirm), 1, v -> submit.run());
        }

        if (sheet.dialog.getWindow() != null) {
            sheet.dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        sheet.dialog.show();
        et.requestFocus();
    }

    private static float clampHue(float h) {
        if (Float.isNaN(h)) return 0f;
        return Math.max(0f, Math.min(360f, h));
    }

    private static float clamp01(float v) {
        if (Float.isNaN(v)) return 0f;
        return Math.max(0f, Math.min(1f, v));
    }
}
