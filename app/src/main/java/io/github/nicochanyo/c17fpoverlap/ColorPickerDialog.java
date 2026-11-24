package io.github.nicochanyo.c17fpoverlap;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * COUI-styled HSV / Alpha / HEX picker dialog reusing {@link CouiHsvColorPickerView}.
 */
public final class ColorPickerDialog {
    public interface OnColorSelectedListener {
        void onColorSelected(int argbColor);
    }

    private final boolean showAlpha;
    private int currentColor;

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
        CouiBottomSheet.Palette pal = CouiBottomSheet.Palette.fromModuleTheme(context);
        CouiBottomSheet.SheetHandle sheet = CouiBottomSheet.create(context, pal);
        final Dialog dialog = sheet.dialog;

        if (pal.livingFlow) {
            String title = titleRes != 0
                    ? context.getString(titleRes)
                    : context.getString(R.string.dialog_color_title);
            CouiBottomSheet.addLivingFlowTopBar(
                    context, sheet, title,
                    context.getString(R.string.btn_cancel),
                    context.getString(R.string.btn_apply_color),
                    1,
                    v -> {
                        dialog.dismiss();
                        if (listener != null) {
                            listener.onColorSelected(currentColor);
                        }
                    });
            float d = context.getResources().getDisplayMetrics().density;
            LinearLayout cardGroup = CouiBottomSheet.createSubcardGroup(context, pal);
            int pad = Math.round(18f * d);
            cardGroup.setPadding(pad, pad, pad, pad);
            CouiHsvColorPickerView hsvPicker = new CouiHsvColorPickerView(context);
            hsvPicker.setShowAlpha(showAlpha);
            hsvPicker.setColor(currentColor);
            hsvPicker.setOnColorChangeListener(new CouiHsvColorPickerView.OnColorChangeListener() {
                @Override
                public void onColorChanged(int argbColor, boolean fromUser) {
                    currentColor = argbColor;
                }

                @Override
                public void onTrackingStopped(int argbColor) {
                    currentColor = argbColor;
                }
            });
            cardGroup.addView(hsvPicker, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = Math.round(6f * d);
            sheet.card.addView(cardGroup, lp);
            DynamicThemeHelper.applyAccent(sheet.card, context);
            dialog.show();
            return;
        }

        View root = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, sheet.card, false);
        root.setBackground(null);
        root.setPadding(0, 0, 0, 0);
        sheet.card.addView(root);
        DynamicThemeHelper.applyAccent(sheet.card, context);

        TextView tvTitle = root.findViewById(R.id.tvPickerTitle);
        if (tvTitle != null && titleRes != 0) {
            tvTitle.setText(titleRes);
        }

        CouiHsvColorPickerView hsvPicker = root.findViewById(R.id.hsvColorPicker);
        hsvPicker.setShowAlpha(showAlpha);
        hsvPicker.setColor(currentColor);
        hsvPicker.setOnColorChangeListener(new CouiHsvColorPickerView.OnColorChangeListener() {
            @Override
            public void onColorChanged(int argbColor, boolean fromUser) {
                currentColor = argbColor;
            }

            @Override
            public void onTrackingStopped(int argbColor) {
                currentColor = argbColor;
            }
        });

        Button btnCancel = root.findViewById(R.id.btnPickerCancel);
        Button btnApply = root.findViewById(R.id.btnPickerApply);
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

        dialog.show();
    }
}
