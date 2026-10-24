package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputFilter;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Owner identity belongs to this TV's private storage, separate from themes and shared APKs. */
final class OwnerProfile {
    private static final String PREFS = "personal_home_owner_v1";
    private OwnerProfile() { }
    private static SharedPreferences prefs(Context context) { return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    static String name(Context context) { return CompactHomeText.owner(prefs(context).getString("name", "")); }
    static boolean configured(Context context) { return prefs(context).getBoolean("configured", false); }
    static AlertDialog edit(Activity activity, boolean firstSetup, Runnable changed) {
        float density = activity.getResources().getDisplayMetrics().density;
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * density);
        body.setPadding(padding, padding / 2, padding, 0);
        TextView note = new TextView(activity);
        note.setText("Tên này chỉ lưu trên TV/box hiện tại. Ví dụ nhập An để hiện XIN CHÀO, AN. Để trống sẽ chỉ hiện XIN CHÀO.");
        note.setTextSize(16);
        body.addView(note);
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setTextSize(21);
        input.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
        input.setHint("Tên chủ sở hữu");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});
        input.setText(name(activity));
        input.setSelection(input.length());
        body.addView(input, new LinearLayout.LayoutParams(-1, -2));
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(firstSetup ? "Chào mừng! TV này của ai?" : "Tên chủ sở hữu")
                .setView(body)
                .setPositiveButton("Lưu", null)
                .setNegativeButton(firstSetup ? "Để sau" : "Hủy", (d, w) -> {
                    if (firstSetup) prefs(activity).edit().putBoolean("configured", true).commit();
                }).create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = CompactHomeText.owner(input.getText().toString());
                if (!prefs(activity).edit().putString("name", name).putBoolean("configured", true).commit()) {
                    Toast.makeText(activity, "Chưa lưu được tên, hãy thử lại", Toast.LENGTH_LONG).show();
                    return;
                }
                SharedPreferences choices = activity.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
                choices.edit().putLong(Config.SETTINGS_REVISION_KEY, choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).commit();
                dialog.dismiss();
                changed.run();
            });
            input.requestFocus();
            if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        });
        dialog.show();
        return dialog;
    }
}
