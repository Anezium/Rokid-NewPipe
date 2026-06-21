package org.schabi.newpipe.rokid;

import android.os.SystemClock;
import android.view.KeyEvent;

public final class RokidKeyMapper {
    private static final long DIRECTION_DEBOUNCE_MS = 240L;
    private static long lastDirectionAt;

    private RokidKeyMapper() {
    }

    public enum Action {
        NONE,
        DUPLICATE,
        PREVIOUS,
        NEXT,
        SELECT,
        BACK
    }

    public static Action map(final KeyEvent event) {
        if (event == null || event.getAction() != KeyEvent.ACTION_DOWN) {
            return Action.NONE;
        }
        if (event.getRepeatCount() > 0) {
            return Action.DUPLICATE;
        }

        return map(event.getKeyCode());
    }

    public static Action map(final int keyCode) {
        return map(keyCode, SystemClock.elapsedRealtime());
    }

    static Action map(final int keyCode, final long now) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case 184:
                return debounce(Action.PREVIOUS, now);
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case 183:
                return debounce(Action.NEXT, now);
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_SPACE:
            case 202:
                return Action.SELECT;
            case KeyEvent.KEYCODE_BACK:
                return Action.BACK;
            default:
                return Action.NONE;
        }
    }

    public static boolean isDirectionalKey(final int keyCode) {
        return isPreviousKey(keyCode) || isNextKey(keyCode);
    }

    public static boolean isPreviousKey(final int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case 184:
                return true;
            default:
                return false;
        }
    }

    public static boolean isNextKey(final int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case 183:
                return true;
            default:
                return false;
        }
    }

    static void resetDebounceForTesting() {
        lastDirectionAt = 0L;
    }

    private static Action debounce(final Action action, final long now) {
        if (lastDirectionAt > 0L && now - lastDirectionAt < DIRECTION_DEBOUNCE_MS) {
            return Action.DUPLICATE;
        }
        lastDirectionAt = now;
        return action;
    }
}
