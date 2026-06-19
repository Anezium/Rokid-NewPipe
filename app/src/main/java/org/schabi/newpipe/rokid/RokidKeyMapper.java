package org.schabi.newpipe.rokid;

import android.os.SystemClock;
import android.view.KeyEvent;

public final class RokidKeyMapper {
    private static final long DIRECTION_DEBOUNCE_MS = 260L;
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
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case 184:
                return debounce(Action.PREVIOUS);
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case 183:
                return debounce(Action.NEXT);
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

    private static Action debounce(final Action action) {
        final long now = SystemClock.elapsedRealtime();
        if (now - lastDirectionAt < DIRECTION_DEBOUNCE_MS) {
            lastDirectionAt = now;
            return Action.DUPLICATE;
        }
        lastDirectionAt = now;
        return action;
    }
}
