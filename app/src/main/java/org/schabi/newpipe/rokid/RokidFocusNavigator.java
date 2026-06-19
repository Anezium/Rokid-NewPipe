package org.schabi.newpipe.rokid;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public final class RokidFocusNavigator {
    private RokidFocusNavigator() {
    }

    public static boolean handle(
            @NonNull final Activity activity,
            @NonNull final KeyEvent event
    ) {
        final RokidKeyMapper.Action action = RokidKeyMapper.map(event);
        if (action == RokidKeyMapper.Action.NONE) {
            return false;
        }
        if (action == RokidKeyMapper.Action.DUPLICATE) {
            return true;
        }

        final View root = activity.findViewById(android.R.id.content);
        final View current = activity.getCurrentFocus();
        switch (action) {
            case PREVIOUS:
                return moveFocus(root, current, View.FOCUS_BACKWARD);
            case NEXT:
                return moveFocus(root, current, View.FOCUS_FORWARD);
            case SELECT:
                return clickCurrent(activity, current);
            case BACK:
                return false;
            default:
                return false;
        }
    }

    private static boolean moveFocus(
            final View root,
            final View current,
            final int direction
    ) {
        if (root == null) {
            return false;
        }

        final View target;
        if (current == null) {
            target = findEdgeFocusable(root, direction == View.FOCUS_BACKWARD);
        } else {
            final View searched = current.focusSearch(direction);
            target = searched == null ? findEdgeFocusable(root, direction == View.FOCUS_BACKWARD)
                    : searched;
        }

        if (target == null || target == current || !target.isShown() || !target.isEnabled()) {
            return false;
        }
        target.requestFocus();
        return true;
    }

    private static View findEdgeFocusable(final View root, final boolean last) {
        final ArrayList<View> focusables = new ArrayList<>();
        root.addFocusables(focusables, View.FOCUS_FORWARD);
        if (focusables.isEmpty()) {
            return null;
        }
        return focusables.get(last ? focusables.size() - 1 : 0);
    }

    private static boolean clickCurrent(final Activity activity, final View current) {
        if (current == null || !current.isShown() || !current.isEnabled()) {
            return false;
        }
        if (current instanceof EditText) {
            return current.performClick();
        }
        return current.performClick();
    }
}
