package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/** Parent controls always require the current time PIN, so a child cannot cancel/extend a running timer. */
public final class ChildLockSettingsActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Button> protectedButtons = new ArrayList<>();
    private ChildLockPinPad gate;
    private TextView status;
    private boolean authenticated;
    private long lastInteraction;
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            if (authenticated && SystemClock.elapsedRealtime() - lastInteraction > 60_000) showGate();
            if (gate != null) gate.updateCooldown();
            else refreshStatus();
            handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
    @Override protected void onResume() {
        super.onResume();
        if (!authenticated) showGate();
        handler.removeCallbacks(refresh);
        handler.post(refresh);
    }
    @Override protected void onPause() {
        handler.removeCallbacks(refresh);
        authenticated = false;
        super.onPause();
    }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy(); }

    private void showGate() {
        authenticated = false;
        gate = new ChildLockPinPad(this, false, () -> {
            authenticated = true;
            lastInteraction = SystemClock.elapsedRealtime();
            showMenu();
        }, this::finish);
        setContentView(gate);
    }

    private void showMenu() {
        gate = null;
        protectedButtons.clear();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xff0d1420);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(42), dp(22), dp(42), dp(22));
        scroll.addView(body);
        body.addView(text("Kh\u00f3a tr\u1ebb em", 27));
        status = text("", 17);
        status.setPadding(0, dp(8), 0, dp(10));
        body.addView(status);
        body.addView(text(ChildLockState.fixedPin(this)
                ? "PIN hiện tại: Cố định 6 số do bạn đặt."
                : "PIN hiện tại: Theo giờ Việt Nam (ddMMHH).", 18));
        button(body, "Chế độ PIN: Theo giờ / Cố định", this::choosePinMode);
        if (ChildLockState.fixedPin(this)) button(body, "Đổi PIN cố định", () -> confirmCurrentPin(() -> createFixedPin(null)));
        body.addView(text(ChildLockState.fixedPin(this)
                ? "Mở khóa bằng PIN riêng của bạn. Mã theo giờ không mở được khi chọn chế độ cố định."
                : "Mã mở: ddMMHH theo giờ Việt Nam (24h). Đây là mã theo quy luật, không phải OTP ngẫu nhiên.", 16));
        body.addView(text("Khi m\u00e0n h\u00ecnh \u0111en: b\u1ea5m nhanh 5 l\u1ea7n Mute HO\u1eb6C OK, "
                + "r\u1ed3i nh\u1eadp 6 s\u1ed1. Back s\u1ebd \u1ea9n b\u00e0n ph\u00edm.\n"
                + "H\u00e3y th\u1eed kh\u00f3a/m\u1edf b\u1eb1ng remote th\u1eadt v\u00e0 ki\u1ec3m tra ti\u1ebfng tr\u01b0\u1edbc khi d\u00f9ng cho tr\u1ebb.", 16));
        Button permission = button(body, "B\u1eadt d\u1ecbch v\u1ee5 trong Tr\u1ee3 n\u0103ng", this::openAccessibility);
        addProtected(body, "Th\u1eed h\u1ebft gi\u1edd sau 1 ph\u00fat", () -> startTimer(1));
        for (int minutes : new int[]{15, 30, 45, 60, 90, 120}) {
            addProtected(body, "Cho xem " + minutes + " ph\u00fat", () -> startTimer(minutes));
        }
        addProtected(body, "Kh\u00f3a ngay", () -> new AlertDialog.Builder(this)
                .setTitle("Kh\u00f3a ngay?")
                .setMessage("M\u00e0n h\u00ecnh s\u1ebd chuy\u1ec3n \u0111en. "
                        + "Bấm nhanh 5 lần Mute hoặc OK để nhập PIN của chế độ đang chọn.")
                .setPositiveButton("Kh\u00f3a", (d, w) -> {
                    if (!requireService()) return;
                    if (ChildLockState.lock(this)) finish();
                    else toast("Kh\u00f4ng l\u01b0u \u0111\u01b0\u1ee3c tr\u1ea1ng th\u00e1i");
                }).setNegativeButton("H\u1ee7y", null).show());
        button(body, "H\u1ee7y h\u1eb9n gi\u1edd", () -> {
            boolean saved = ChildLockState.cancelTimer(this);
            refreshStatus();
            toast(saved ? "\u0110\u00e3 h\u1ee7y h\u1eb9n gi\u1edd" : "Kh\u00f4ng l\u01b0u \u0111\u01b0\u1ee3c thay \u0111\u1ed5i");
        });
        AudioManager audio = getSystemService(AudioManager.class);
        if (audio != null && audio.isVolumeFixed()) {
            body.addView(text("Box b\u00e1o \u00e2m l\u01b0\u1ee3ng c\u1ed1 \u0111\u1ecbnh. "
                    + "HDMI/passthrough c\u00f3 th\u1ec3 kh\u00f4ng t\u1eaft ti\u1ebfng theo l\u1ec7nh Android. "
                    + "Ph\u1ea3i ki\u1ec3m tra \u00e2m thanh th\u1ef1c t\u1ebf.", 16));
        }
        body.addView(text("Kh\u00f3a ch\u1ec9 \u00e1p d\u1ee5ng cho box. N\u00fat ngu\u1ed3n TV, "
                + "CEC/IR v\u00e0 giao di\u1ec7n h\u1ec7 th\u1ed1ng c\u00f3 th\u1ec3 v\u1eabn ho\u1ea1t \u0111\u1ed9ng. "
                + "Kh\u00f4ng t\u1eaft d\u1ecbch v\u1ee5 trong l\u00fac h\u1eb9n gi\u1edd.", 15));
        button(body, "Quay l\u1ea1i Home", this::finish);
        setContentView(scroll);
        refreshStatus();
        permission.requestFocus();
    }

    private void openAccessibility() {
        try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
        catch (ActivityNotFoundException ignored) { toast("H\u00e3y m\u1edf C\u00e0i \u0111\u1eb7t Android > Tr\u1ee3 n\u0103ng"); }
    }
    private void choosePinMode() {
        boolean fixed = ChildLockState.fixedPin(this);
        new AlertDialog.Builder(this).setTitle("Chọn chế độ PIN")
                .setSingleChoiceItems(new String[]{"Theo giờ Việt Nam — ddMMHH", "Cố định — tự đặt 6 số"}, fixed ? 1 : 0, (dialog, which) -> {
                    dialog.dismiss();
                    if ((which == 1) == fixed) return;
                    confirmCurrentPin(() -> {
                        if (which == 1) createFixedPin(null);
                        else new AlertDialog.Builder(this).setTitle("Dùng PIN theo giờ?")
                                .setMessage("PIN cố định sẽ được xóa. Mã mới là ddMMHH theo giờ Việt Nam 24h.")
                                .setPositiveButton("Đổi chế độ", (d, w) -> {
                                    if (!authenticated || SystemClock.elapsedRealtime() - lastInteraction > 60_000) { showGate(); return; }
                                    boolean saved = ChildLockState.useTimePin(this);
                                    toast(saved ? "Đã chuyển sang PIN theo giờ" : "Không lưu được chế độ PIN");
                                    showGate();
                                }).setNegativeButton("Hủy", (d, w) -> showMenu()).show();
                    });
                }).setNegativeButton("Quay lại", null).show();
    }
    private void confirmCurrentPin(Runnable next) {
        authenticated = false;
        toast("Nhập lại PIN đang dùng để xác nhận thay đổi");
        gate = new ChildLockPinPad(this, false, () -> {
            authenticated = true;
            lastInteraction = SystemClock.elapsedRealtime();
            next.run();
        }, this::showGate);
        setContentView(gate);
    }
    private void createFixedPin(String firstPin) {
        gate = new ChildLockPinPad(this, firstPin == null ? "Đặt PIN mới — 6 số" : "Nhập lại PIN mới", pin -> {
            if (!authenticated) { showGate(); return; }
            if (firstPin == null) { createFixedPin(pin); return; }
            if (!firstPin.equals(pin)) {
                toast("Hai lần nhập không giống nhau. Hãy đặt lại PIN mới.");
                createFixedPin(null);
                return;
            }
            boolean saved = ChildLockState.setFixedPin(this, pin);
            toast(saved ? "Đã lưu PIN cố định. Nhập PIN mới để vào menu." : "Không lưu được PIN mới");
            showGate();
        }, this::showMenu);
        setContentView(gate);
    }
    private boolean requireService() {
        if (ChildLockService.ready()) return true;
        toast("B\u1eadt d\u1ecbch v\u1ee5 Kh\u00f3a tr\u1ebb em trong Tr\u1ee3 n\u0103ng tr\u01b0\u1edbc");
        return false;
    }
    private void startTimer(int minutes) {
        if (!requireService()) return;
        if (ChildLockState.start(this, minutes)) {
            toast("\u0110\u00e3 h\u1eb9n kh\u00f3a sau " + minutes + " ph\u00fat");
            finish();
        } else toast("Kh\u00f4ng l\u01b0u \u0111\u01b0\u1ee3c h\u1eb9n gi\u1edd");
    }
    private void refreshStatus() {
        if (status == null) return;
        boolean ready = ChildLockService.ready();
        for (Button button : protectedButtons) { button.setEnabled(ready); button.setAlpha(ready ? 1f : 0.45f); }
        String value = ready ? "D\u1ecbch v\u1ee5: \u0111ang b\u1eadt" : "D\u1ecbch v\u1ee5: ch\u01b0a k\u1ebft n\u1ed1i";
        if (ChildLockService.keyFilterConflict(this)) value += "\nC\u00f3 d\u1ecbch v\u1ee5 kh\u00e1c \u0111ang ch\u1eb7n ph\u00edm. "
                + "T\u1eaft ph\u1ea7n ch\u1eb7n ph\u00edm c\u1ee7a d\u1ecbch v\u1ee5 \u0111\u00f3 tr\u01b0\u1edbc khi th\u1eed kh\u00f3a.";
        if (ChildLockState.timed(this)) value += " \u00b7 C\u00f2n " + ((ChildLockState.remaining(this) + 59_999) / 60_000) + " ph\u00fat";
        else value += " \u00b7 Ch\u01b0a h\u1eb9n gi\u1edd";
        String error = ChildLockState.prefs(this).getString("error", "");
        if (!error.isEmpty()) value += "\n" + error;
        status.setText(value);
    }
    private void addProtected(LinearLayout body, String label, Runnable click) {
        protectedButtons.add(button(body, label, click));
    }
    private Button button(LinearLayout body, String label, Runnable click) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(17);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setOnClickListener(v -> click.run());
        View.OnFocusChangeListener focus = (v, selected) -> {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(selected ? 0xff2563eb : 0xff253247);
            bg.setCornerRadius(dp(7));
            v.setBackground(bg);
        };
        button.setOnFocusChangeListener(focus);
        focus.onFocusChange(button, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(46));
        lp.setMargins(0, dp(5), 0, 0);
        body.addView(button, lp);
        return button;
    }
    private TextView text(String value, int size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(Color.WHITE);
        text.setTextSize(size);
        text.setPadding(0, dp(5), 0, dp(5));
        return text;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_LONG).show(); }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        lastInteraction = SystemClock.elapsedRealtime();
        return super.dispatchKeyEvent(event);
    }
}
