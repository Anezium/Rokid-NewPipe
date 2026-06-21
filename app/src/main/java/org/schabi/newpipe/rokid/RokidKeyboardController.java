package org.schabi.newpipe.rokid;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.schabi.newpipe.R;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

public final class RokidKeyboardController {
    private static final String[] LETTER_KEYS = {
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
            "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
            "space", "del", "clear", "go", "123"
    };
    private static final String[] SYMBOL_KEYS = {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            ".", "-", "_", "@", "/", ":", "#", "+", "space", "del", "clear", "go", "abc"
    };
    private static final WeakHashMap<Activity, RokidKeyboardController> INSTANCES =
            new WeakHashMap<>();

    private final WeakReference<Activity> activityReference;
    @Nullable
    private EditText target;
    @Nullable
    private Runnable enterAction;
    @Nullable
    private FrameLayout overlay;
    @Nullable
    private TextView preview;
    @Nullable
    private TextView selectedKey;
    @Nullable
    private TextView strip;
    private String[] keys = LETTER_KEYS;
    private int selectedIndex = 0;

    private RokidKeyboardController(@NonNull final Activity activity) {
        activityReference = new WeakReference<>(activity);
    }

    @NonNull
    public static RokidKeyboardController forActivity(@NonNull final Activity activity) {
        RokidKeyboardController controller = INSTANCES.get(activity);
        if (controller == null) {
            controller = new RokidKeyboardController(activity);
            INSTANCES.put(activity, controller);
        }
        return controller;
    }

    public static void hideAll() {
        for (final RokidKeyboardController controller
                : new java.util.ArrayList<>(INSTANCES.values())) {
            controller.hide(null);
        }
    }

    public static void hideAll(@Nullable final Activity activity) {
        hideAll();
        if (activity == null) {
            return;
        }

        View overlayView = activity.findViewById(R.id.rokidKeyboardOverlay);
        while (overlayView != null) {
            final ViewGroup parent = (ViewGroup) overlayView.getParent();
            if (parent == null) {
                break;
            }
            parent.removeView(overlayView);
            overlayView = activity.findViewById(R.id.rokidKeyboardOverlay);
        }
    }

    public boolean isVisible() {
        return overlay != null && overlay.getVisibility() == View.VISIBLE;
    }

    public void show(@NonNull final EditText editText, @NonNull final Runnable onEnter) {
        final Activity activity = activityReference.get();
        if (activity == null) {
            return;
        }
        ensureOverlay(activity);
        if (target != editText) {
            keys = LETTER_KEYS;
            selectedIndex = 0;
        }
        target = editText;
        enterAction = onEnter;
        selectedIndex = Math.min(selectedIndex, keys.length - 1);
        editText.requestFocus();
        hideAndroidKeyboard(activity, editText);
        overlay.setVisibility(View.VISIBLE);
        updateText();
    }

    public void hide(@Nullable final EditText editText) {
        if (target != null && editText != null && target != editText) {
            return;
        }
        if (overlay != null) {
            final ViewGroup parent = (ViewGroup) overlay.getParent();
            if (parent != null) {
                parent.removeView(overlay);
            }
            overlay = null;
        }
        if (target != null) {
            target.clearFocus();
        }
        preview = null;
        selectedKey = null;
        strip = null;
        target = null;
        enterAction = null;
    }

    public boolean handleKeyEvent(@NonNull final KeyEvent event) {
        if (!isVisible()) {
            return false;
        }
        final RokidKeyMapper.Action action = RokidKeyMapper.map(event);
        switch (action) {
            case DUPLICATE:
                return true;
            case PREVIOUS:
                selectOffset(-1);
                return true;
            case NEXT:
                selectOffset(1);
                return true;
            case SELECT:
                activateSelected();
                return true;
            case BACK:
                hide(target);
                return true;
            default:
                return false;
        }
    }

    private void ensureOverlay(@NonNull final Activity activity) {
        if (overlay != null) {
            return;
        }

        overlay = new FrameLayout(activity);
        overlay.setId(R.id.rokidKeyboardOverlay);
        overlay.setVisibility(View.GONE);
        overlay.setFocusable(true);
        overlay.setFocusableInTouchMode(true);

        final LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 14));
        panel.setBackground(outline(Color.WHITE, 2));

        preview = makeText(activity, 18, Typeface.NORMAL);
        preview.setSingleLine(true);
        selectedKey = makeText(activity, 34, Typeface.BOLD);
        selectedKey.setGravity(Gravity.CENTER);
        strip = makeText(activity, 14, Typeface.NORMAL);
        strip.setGravity(Gravity.CENTER);
        strip.setSingleLine(true);

        panel.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(selectedKey, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(strip, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 164), Gravity.BOTTOM);
        panelParams.setMargins(dp(activity, 10), 0, dp(activity, 10), dp(activity, 10));
        overlay.addView(panel, panelParams);

        final ViewGroup decor = activity.findViewById(android.R.id.content);
        decor.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private TextView makeText(
            @NonNull final Activity activity,
            final int sp,
            final int typefaceStyle
    ) {
        final TextView textView = new TextView(activity);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(sp);
        textView.setTypeface(Typeface.DEFAULT, typefaceStyle);
        textView.setIncludeFontPadding(false);
        textView.setPadding(0, dp(activity, 5), 0, dp(activity, 5));
        return textView;
    }

    private GradientDrawable outline(final int strokeColor, final int strokeDp) {
        final Activity activity = activityReference.get();
        final GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.BLACK);
        drawable.setStroke(activity == null ? strokeDp : dp(activity, strokeDp), strokeColor);
        drawable.setCornerRadius(0);
        return drawable;
    }

    private void selectOffset(final int offset) {
        selectedIndex = (selectedIndex + offset + keys.length) % keys.length;
        updateText();
    }

    private void activateSelected() {
        if (target == null) {
            return;
        }
        final String key = keys[selectedIndex];
        switch (key) {
            case "space":
                insert(" ");
                break;
            case "del":
                delete();
                break;
            case "clear":
                target.getText().clear();
                break;
            case "go":
                if (enterAction != null) {
                    enterAction.run();
                }
                break;
            case "123":
                keys = SYMBOL_KEYS;
                selectedIndex = 0;
                break;
            case "abc":
                keys = LETTER_KEYS;
                selectedIndex = 0;
                break;
            default:
                insert(key);
                break;
        }
        updateText();
    }

    private void insert(final String value) {
        final int start = Math.max(target.getSelectionStart(), 0);
        final int end = Math.max(target.getSelectionEnd(), 0);
        final int from = Math.min(start, end);
        final int to = Math.max(start, end);
        target.getText().replace(from, to, value);
        target.setSelection(from + value.length());
    }

    private void delete() {
        final int start = Math.max(target.getSelectionStart(), 0);
        final int end = Math.max(target.getSelectionEnd(), 0);
        if (start != end) {
            target.getText().delete(Math.min(start, end), Math.max(start, end));
        } else if (start > 0) {
            target.getText().delete(start - 1, start);
        }
    }

    private void updateText() {
        if (target == null || preview == null || selectedKey == null || strip == null) {
            return;
        }
        preview.setText(target.getText());
        selectedKey.setText(keys[selectedIndex]);
        strip.setText(buildStrip());
    }

    private String buildStrip() {
        final StringBuilder builder = new StringBuilder();
        for (int offset = -4; offset <= 4; offset++) {
            final int index = (selectedIndex + offset + keys.length) % keys.length;
            if (offset == 0) {
                builder.append(" [").append(keys[index]).append("] ");
            } else {
                builder.append(' ').append(keys[index]).append(' ');
            }
        }
        return builder.toString();
    }

    private static void hideAndroidKeyboard(
            @NonNull final Activity activity,
            @NonNull final EditText editText
    ) {
        final InputMethodManager imm = ContextCompat.getSystemService(activity,
                InputMethodManager.class);
        if (imm != null) {
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
    }

    private static int dp(@NonNull final Activity activity, final int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }
}
