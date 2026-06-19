package org.schabi.newpipe.rokid;

import android.app.Activity;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;

import org.schabi.newpipe.R;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.WeakHashMap;

public final class RokidFocusNavigator {
    private static final WeakHashMap<Activity, View> LAST_TARGETS = new WeakHashMap<>();

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
                return moveFocus(activity, root, current, View.FOCUS_BACKWARD);
            case NEXT:
                return moveFocus(activity, root, current, View.FOCUS_FORWARD);
            case SELECT:
                return clickCurrent(activity, current);
            case BACK:
                return false;
            default:
                return false;
        }
    }

    private static boolean moveFocus(
            final Activity activity,
            final View root,
            final View current,
            final int direction
    ) {
        if (root == null) {
            return false;
        }

        final ArrayList<View> targets = collectTargets(root);
        if (targets.isEmpty()) {
            return false;
        }

        int currentIndex = findCurrentIndex(current, targets);
        if (currentIndex < 0) {
            currentIndex = findCurrentIndex(LAST_TARGETS.get(activity), targets);
        }

        final int targetIndex;
        if (currentIndex < 0) {
            targetIndex = findInitialIndex(targets, direction);
        } else if (direction == View.FOCUS_BACKWARD) {
            targetIndex = currentIndex == 0 ? targets.size() - 1 : currentIndex - 1;
        } else {
            targetIndex = currentIndex == targets.size() - 1 ? 0 : currentIndex + 1;
        }

        final View target = targets.get(targetIndex);
        if (target == current || !target.isShown() || !target.isEnabled()) {
            return false;
        }
        final boolean focused = requestRokidFocus(target);
        LAST_TARGETS.put(activity, target);
        return focused || target.isFocused();
    }

    private static ArrayList<View> collectTargets(final View root) {
        final ArrayList<View> targets = new ArrayList<>();
        collectTargets(root, targets);
        final boolean errorPanelVisible = isViewWithIdShown(root, R.id.error_panel);
        targets.sort(Comparator
                .comparingInt((View view) -> getPriority(view, errorPanelVisible))
                .thenComparingInt(view -> getScreenRect(view).top)
                .thenComparingInt(view -> getScreenRect(view).left));
        return targets;
    }

    private static void collectTargets(final View view, final ArrayList<View> targets) {
        if (view == null || !view.isShown() || !view.isEnabled()
                || view.getWidth() <= 1 || view.getHeight() <= 1) {
            return;
        }

        if (isRokidTarget(view)) {
            makeFocusable(view);
            targets.add(view);
            return;
        }

        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                collectTargets(group.getChildAt(i), targets);
            }
        }
    }

    private static boolean isRokidTarget(final View view) {
        if (view.getId() == android.R.id.content) {
            return false;
        }

        return view instanceof EditText || view.isClickable() || view.isLongClickable();
    }

    private static void makeFocusable(final View view) {
        if (!view.isFocusable()) {
            view.setFocusable(true);
        }
        if (!view.isFocusableInTouchMode()) {
            view.setFocusableInTouchMode(true);
        }
    }

    private static int findCurrentIndex(final View current, final ArrayList<View> targets) {
        if (current == null) {
            return -1;
        }
        for (int i = 0; i < targets.size(); i++) {
            final View target = targets.get(i);
            if (current == target || isDescendantOf(current, target)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isDescendantOf(final View child, final View possibleParent) {
        View current = child;
        while (current.getParent() instanceof View) {
            current = (View) current.getParent();
            if (current == possibleParent) {
                return true;
            }
        }
        return false;
    }

    private static boolean requestRokidFocus(final View target) {
        makeFocusable(target);
        target.requestFocusFromTouch();
        return target.requestFocus();
    }

    private static int findInitialIndex(final ArrayList<View> targets, final int direction) {
        if (direction == View.FOCUS_BACKWARD) {
            for (int i = 0; i < targets.size(); i++) {
                if (targets.get(i).getId() == R.id.action_search) {
                    return i;
                }
            }
            return targets.size() - 1;
        }
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).getId() == R.id.error_wifi_settings_button) {
                return i;
            }
        }
        return 0;
    }

    private static Rect getScreenRect(final View view) {
        final Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);
        return rect;
    }

    private static int getPriority(final View view, final boolean errorPanelVisible) {
        if (errorPanelVisible && hasAncestorWithId(view, R.id.error_panel)) {
            return 0;
        }
        if (hasAncestorWithId(view, R.id.toolbar_layout) || view.getId() == R.id.action_search) {
            return errorPanelVisible ? 1 : 0;
        }
        return errorPanelVisible ? 2 : 1;
    }

    private static boolean hasAncestorWithId(final View child, final int id) {
        View current = child;
        while (current != null) {
            if (current.getId() == id) {
                return true;
            }
            if (!(current.getParent() instanceof View)) {
                return false;
            }
            current = (View) current.getParent();
        }
        return false;
    }

    private static boolean isViewWithIdShown(final View view, final int id) {
        if (view == null || !view.isShown()) {
            return false;
        }
        if (view.getId() == id) {
            return true;
        }
        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (isViewWithIdShown(group.getChildAt(i), id)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean clickCurrent(final Activity activity, final View current) {
        View target = current;
        if (target == null || !target.isShown() || !target.isEnabled() || !target.isClickable()) {
            target = LAST_TARGETS.get(activity);
        }

        if (target == null || !target.isShown() || !target.isEnabled()) {
            return false;
        }
        if (target instanceof EditText) {
            return target.performClick();
        }
        return target.performClick();
    }
}
