package io.github.nicochanyo.c17fpoverlap;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * COUI-styled RGB / HEX / preset swatch picker dialog for Fluid Cloud & Dynamic Island tinting.
 */
public final class ColorPickerDialog {
    public interface OnColorSelectedListener {
        void onColorSelected(int argbColor);
    }

    private static final int[] PRESET_COLORS = new int[]{
            0xFF4FC3F7,
            0xFF3B82FF,
            0xFF7C4DFF,
            0xFFFF6B9D,
            0xFFFF8A65,
            0xFF26C6DA,
            0xFF66BB6A,
            0xFFFFFFFF
    };

    private final boolean showAlpha;
    private int currentColor;
    private boolean suppressText;
    private boolean suppressSeek;

    private ColorPickerDialog(int initialColor, boolean showAlpha) {
        this.showAlpha = showAlpha;
        if (showAlpha) {
            this.currentColor = initialColor;
        } else {
            int normalized = ConfigStore.normalizeColor(initialColor);
            this.currentColor = normalized != 0 ? normalized : ConfigStore.DEF_CUSTOM_COLOR;
        }
    }

    public static void show(Context context, int initialColor, final OnColorSelectedListener listener) {
        show(context, R.string.dialog_color_title, initialColor, false, listener);
    }

    public static void show(Context context, int titleRes, int initialColor, final OnColorSelectedListener listener) {
        show(context, titleRes, initialColor, false, listener);
    }

    public static void show(Context context, int titleRes, int initialColor, boolean showAlpha,
                            final OnColorSelectedListener listener) {
        new ColorPickerDialog(initialColor, showAlpha).open(context, titleRes, listener);
    }

    private void open(final Context context, int titleRes, final OnColorSelectedListener listener) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View root = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null, false);
        DynamicThemeHelper.applyAccent(root, context);
        TextView tvTitle = root.findViewById(R.id.tvPickerTitle);
        if (tvTitle != null && titleRes != 0) {
            tvTitle.setText(titleRes);
        }
        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = Math.round(context.getResources().getDisplayMetrics().widthPixels * 0.88f);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        final View vPreview = root.findViewById(R.id.vPickerPreview);
        final EditText etHex = root.findViewById(R.id.etColorHex);
        final LinearLayout presetsRow = root.findViewById(R.id.layoutPresets);
        final SeekBar sbR = root.findViewById(R.id.sbColorR);
        final SeekBar sbG = root.findViewById(R.id.sbColorG);
        final SeekBar sbB = root.findViewById(R.id.sbColorB);
        final TextView tvR = root.findViewById(R.id.tvColorRVal);
        final TextView tvG = root.findViewById(R.id.tvColorGVal);
        final TextView tvB = root.findViewById(R.id.tvColorBVal);
        final View layoutAlpha = root.findViewById(R.id.layoutColorAlpha);
        final SeekBar sbA = root.findViewById(R.id.sbColorA);
        final TextView tvA = root.findViewById(R.id.tvColorAVal);
        Button btnCancel = root.findViewById(R.id.btnPickerCancel);
        Button btnApply = root.findViewById(R.id.btnPickerApply);

        if (showAlpha) {
            layoutAlpha.setVisibility(View.VISIBLE);
            etHex.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
            etHex.setHint("CC4FC3F7");
        }

        final View[] swatchViews = new View[PRESET_COLORS.length];
        float d = context.getResources().getDisplayMetrics().density;
        int swatchSize = Math.round(28f * d);
        int swatchMargin = Math.round(4f * d);

        final Runnable syncViews = new Runnable() {
            @Override
            public void run() {
                updateSwatchDrawable(vPreview, currentColor, true, context);
                int a = Color.alpha(currentColor);
                int r = Color.red(currentColor);
                int g = Color.green(currentColor);
                int b = Color.blue(currentColor);
                tvR.setText(String.valueOf(r));
                tvG.setText(String.valueOf(g));
                tvB.setText(String.valueOf(b));
                if (showAlpha) {
                    tvA.setText(Math.round(a * 100f / 255f) + "%");
                }

                if (!suppressSeek) {
                    suppressSeek = true;
                    sbR.setProgress(r);
                    sbG.setProgress(g);
                    sbB.setProgress(b);
                    if (showAlpha) {
                        sbA.setProgress(a);
                    }
                    suppressSeek = false;
                }
                if (!suppressText) {
                    suppressText = true;
                    String hex = showAlpha
                            ? String.format("%08X", currentColor)
                            : String.format("%06X", currentColor & 0x00FFFFFF);
                    etHex.setText(hex);
                    etHex.setSelection(hex.length());
                    suppressText = false;
                }
                for (int i = 0; i < PRESET_COLORS.length; i++) {
                    if (swatchViews[i] != null) {
                        updateSwatchDrawable(swatchViews[i], PRESET_COLORS[i],
                                (currentColor & 0x00FFFFFF) == (PRESET_COLORS[i] & 0x00FFFFFF), context);
                    }
                }
            }
        };

        for (int i = 0; i < PRESET_COLORS.length; i++) {
            final int preset = PRESET_COLORS[i];
            View dot = new View(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(swatchSize, swatchSize);
            lp.setMargins(swatchMargin, 0, swatchMargin, 0);
            dot.setLayoutParams(lp);
            dot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentColor = showAlpha
                            ? ((currentColor & 0xFF000000) | (preset & 0x00FFFFFF))
                            : preset;
                    syncViews.run();
                }
            });
            swatchViews[i] = dot;
            presetsRow.addView(dot);
        }

        SeekBar.OnSeekBarChangeListener rgbListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser || suppressSeek) {
                    return;
                }
                if (showAlpha) {
                    currentColor = Color.argb(sbA.getProgress(), sbR.getProgress(), sbG.getProgress(), sbB.getProgress());
                } else {
                    currentColor = Color.rgb(sbR.getProgress(), sbG.getProgress(), sbB.getProgress());
                }
                syncViews.run();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        sbR.setOnSeekBarChangeListener(rgbListener);
        sbG.setOnSeekBarChangeListener(rgbListener);
        sbB.setOnSeekBarChangeListener(rgbListener);
        if (showAlpha) {
            sbA.setOnSeekBarChangeListener(rgbListener);
        }

        etHex.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (suppressText || s == null) {
                    return;
                }
                String clean = s.toString().trim().replace("#", "");
                if (showAlpha && clean.length() == 8) {
                    try {
                        int parsed = (int) Long.parseLong(clean, 16);
                        suppressText = true;
                        currentColor = parsed;
                        syncViews.run();
                        suppressText = false;
                    } catch (Throwable ignored) {
                    }
                    return;
                }
                if (clean.length() != 6) {
                    return;
                }
                try {
                    int parsed = Integer.parseInt(clean, 16);
                    suppressText = true;
                    currentColor = showAlpha
                            ? ((currentColor & 0xFF000000) | (parsed & 0x00FFFFFF))
                            : (0xFF000000 | parsed);
                    syncViews.run();
                    suppressText = false;
                } catch (Throwable ignored) {
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (listener != null) {
                    listener.onColorSelected(currentColor);
                }
            }
        });

        syncViews.run();
        dialog.show();
    }

    private static void updateSwatchDrawable(View v, int color, boolean selected, Context ctx) {
        float d = ctx.getResources().getDisplayMetrics().density;
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        gd.setStroke(Math.round((selected ? 2.5f : 1.0f) * d),
                selected
                        ? DynamicThemeHelper.getAccent(ctx)
                        : ConfigStore.resolveThemeColor(ctx, R.attr.couiDivider, R.color.coui_divider));
        v.setBackground(gd);
    }
}
