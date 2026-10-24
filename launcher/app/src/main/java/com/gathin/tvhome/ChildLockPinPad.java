package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** TV-remote keypad; no IME, no editable PIN text and no code hints on the blackout screen. */
final class ChildLockPinPad extends FrameLayout {
    private final Context context;
    private final boolean unlock;
    private final Runnable success, back;
    private Consumer<String> entry;
    private final StringBuilder digits = new StringBuilder();
    private final TextView mask, status;
    private Button first;
    private final List<Button> buttons = new ArrayList<>();
    private long lastInteraction = SystemClock.elapsedRealtime();

    ChildLockPinPad(Context context, String title, Consumer<String> entry, Runnable back) {
        this(context, false, () -> { }, back, title);
        this.entry = entry;
    }

    ChildLockPinPad(Context context, boolean unlock, Runnable success, Runnable back) {
        this(context, unlock, success, back, unlock ? "Nhập mã mở khóa" : "Mã dành cho phụ huynh");
    }

    private ChildLockPinPad(Context context, boolean unlock, Runnable success, Runnable back, String title) {
        super(context);
        this.context = context;
        this.unlock = unlock;
        this.success = success;
        this.back = back;
        setBackgroundColor(Color.BLACK);
        setClickable(true);
        setFocusableInTouchMode(true);
        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(24), dp(18), dp(24), dp(18));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xff151922);
        bg.setCornerRadius(dp(14));
        panel.setBackground(bg);
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(dp(410), LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        addView(panel, pp);
        panel.addView(text(title, 23));
        mask = text("", 27);
        mask.setPadding(0, dp(10), 0, dp(4));
        panel.addView(mask);
        status = text("", 14);
        status.setMinHeight(dp(26));
        status.setTextColor(0xffffb4ab);
        panel.addView(status);
        String[][] keys = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"X\u00f3a", "0", "\u0110\u00f3ng"}};
        for (String[] rowKeys : keys) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            panel.addView(row, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(49)));
            for (String label : rowKeys) {
                Button button = new Button(context);
                button.setId(View.generateViewId());
                button.setText(label);
                button.setTextSize(19);
                button.setTextColor(Color.WHITE);
                button.setAllCaps(false);
                button.setMinWidth(0);
                button.setMinimumWidth(0);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
                lp.setMargins(dp(3), dp(2), dp(3), dp(2));
                row.addView(button, lp);
                buttons.add(button);
                button.setOnFocusChangeListener((v, focused) -> {
                    GradientDrawable color = new GradientDrawable();
                    color.setColor(focused ? 0xff2563eb : 0xff303744);
                    color.setCornerRadius(dp(8));
                    v.setBackground(color);
                });
                GradientDrawable normal = new GradientDrawable();
                normal.setColor(0xff303744);
                normal.setCornerRadius(dp(8));
                button.setBackground(normal);
                button.setOnClickListener(v -> {
                    touch();
                    if (label.length() == 1) append(label.charAt(0));
                    else if (label.equals("X\u00f3a")) erase();
                    else back.run();
                });
                if (first == null) first = button;
            }
        }
        updateMask();
        post(() -> first.requestFocus());
    }

    void touch() { lastInteraction = SystemClock.elapsedRealtime(); }
    long idleMillis() { return Math.max(0, SystemClock.elapsedRealtime() - lastInteraction); }

    void updateCooldown() {
        long wait = ChildLockState.cooldown(context);
        if (wait > 0) status.setText("Th\u1eed l\u1ea1i sau " + ((wait + 999) / 1000) + " gi\u00e2y");
        else if (status.getText().toString().startsWith("Th\u1eed l\u1ea1i sau ")) status.setText("");
    }

    private void append(char digit) {
        if (ChildLockState.cooldown(context) > 0) { updateCooldown(); return; }
        if (digits.length() >= 6) digits.setLength(0);
        digits.append(digit);
        status.setText("");
        updateMask();
        if (digits.length() != 6) return;
        if (entry != null) {
            String value = digits.toString();
            digits.setLength(0);
            updateMask();
            entry.accept(value);
            return;
        }
        ChildLockState.Result result = ChildLockState.submit(context, digits.toString(), unlock);
        digits.setLength(0);
        updateMask();
        if (result == ChildLockState.Result.OPEN) success.run();
        else if (result == ChildLockState.Result.WAIT) updateCooldown();
        else status.setText("M\u00e3 ch\u01b0a \u0111\u00fang");
    }

    private void erase() {
        if (digits.length() > 0) digits.deleteCharAt(digits.length() - 1);
        updateMask();
    }
    private void updateMask() {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < 6; i++) value.append(i < digits.length() ? "\u2022 " : "\u2013 ");
        mask.setText(value.toString().trim());
    }
    private TextView text(String value, int size) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(Color.WHITE);
        text.setGravity(Gravity.CENTER);
        return text;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    boolean handleRemote(KeyEvent event) {
        touch();
        int code = event.getKeyCode();
        if (code >= KeyEvent.KEYCODE_0 && code <= KeyEvent.KEYCODE_9) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) append((char) ('0' + code - KeyEvent.KEYCODE_0));
            return true;
        }
        if (code >= KeyEvent.KEYCODE_NUMPAD_0 && code <= KeyEvent.KEYCODE_NUMPAD_9) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) append((char) ('0' + code - KeyEvent.KEYCODE_NUMPAD_0));
            return true;
        }
        if (code == KeyEvent.KEYCODE_DEL) {
            if (event.getAction() == KeyEvent.ACTION_UP) erase();
            return true;
        }
        if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE) {
            if (event.getAction() == KeyEvent.ACTION_UP) back.run();
            return true;
        }
        if (code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN
                || code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                int index = buttons.indexOf(findFocus());
                if (index < 0) index = 0;
                int row = index / 3, column = index % 3;
                if (code == KeyEvent.KEYCODE_DPAD_UP) row = Math.max(0, row - 1);
                if (code == KeyEvent.KEYCODE_DPAD_DOWN) row = Math.min(3, row + 1);
                if (code == KeyEvent.KEYCODE_DPAD_LEFT) column = Math.max(0, column - 1);
                if (code == KeyEvent.KEYCODE_DPAD_RIGHT) column = Math.min(2, column + 1);
                buttons.get(row * 3 + column).requestFocus();
            }
            return true;
        }
        if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                View focus = findFocus();
                if (focus instanceof Button) focus.performClick();
                else first.requestFocus();
            }
            return true;
        }
        return false;
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        return handleRemote(event) || super.dispatchKeyEvent(event);
    }
}
