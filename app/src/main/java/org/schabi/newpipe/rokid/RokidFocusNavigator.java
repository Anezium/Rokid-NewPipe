package org.schabi.newpipe.rokid;

import android.app.Activity;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
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
    private static final WeakHashMap<Activity, Integer> ROKID_RAIL_INDICES = new WeakHashMap<>();

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
        if (isViewWithIdShown(root, R.id.rokidActionRail)) {
            switch (action) {
                case PREVIOUS:
                    return moveRokidRailFocus(activity, root, current, false);
                case NEXT:
                    return moveRokidRailFocus(activity, root, current, true);
                case SELECT:
                    return clickRokidRailSelection(activity, root, current);
                case BACK:
                    ROKID_RAIL_INDICES.remove(activity);
                    return false;
                default:
                    return false;
            }
        }
        ROKID_RAIL_INDICES.remove(activity);
        switch (action) {
            case PREVIOUS:
                return moveFocus(activity, root, current, View.FOCUS_BACKWARD);
            case NEXT:
                return moveFocus(activity, root, current, View.FOCUS_FORWARD);
            case SELECT:
                if (clickVisibleVideoPlayer(activity, root)) {
                    return true;
                }
                return clickCurrent(activity, current);
            case BACK:
                return false;
            default:
                return false;
        }
    }

    private static boolean moveRokidRailFocus(
            final Activity activity,
            final View root,
            final View current,
            final boolean forward
    ) {
        final ArrayList<View> actions = collectRokidRailTargets(root);
        if (actions.isEmpty()) {
            return false;
        }

        final int currentIndex = getRokidRailIndex(activity, current, actions);
        final int targetIndex;
        if (currentIndex < 0) {
            targetIndex = forward ? 0 : actions.size() - 1;
        } else if (forward) {
            targetIndex = currentIndex == actions.size() - 1 ? 0 : currentIndex + 1;
        } else {
            targetIndex = currentIndex == 0 ? actions.size() - 1 : currentIndex - 1;
        }

        return focusRokidRailTarget(activity, actions, targetIndex);
    }

    private static boolean clickRokidRailSelection(
            final Activity activity,
            final View root,
            final View current
    ) {
        final ArrayList<View> actions = collectRokidRailTargets(root);
        if (actions.isEmpty()) {
            return false;
        }

        final int currentIndex = getRokidRailIndex(activity, current, actions);
        if (currentIndex < 0) {
            return focusRokidRailTarget(activity, actions, 0);
        }

        final View target = actions.get(currentIndex);
        LAST_TARGETS.put(activity, target);
        return target.performClick();
    }

    private static int getRokidRailIndex(
            final Activity activity,
            final View current,
            final ArrayList<View> actions
    ) {
        final Integer storedIndex = ROKID_RAIL_INDICES.get(activity);
        if (storedIndex != null && storedIndex >= 0 && storedIndex < actions.size()) {
            return storedIndex;
        }

        int index = findCurrentIndex(current, actions);
        if (index < 0) {
            index = findCurrentIndex(LAST_TARGETS.get(activity), actions);
        }
        return index;
    }

    private static boolean focusRokidRailTarget(
            final Activity activity,
            final ArrayList<View> actions,
            final int targetIndex
    ) {
        final View target = actions.get(targetIndex);
        final boolean focused = requestRokidFocus(target);
        applyRokidRailSelection(actions, targetIndex);
        LAST_TARGETS.put(activity, target);
        ROKID_RAIL_INDICES.put(activity, targetIndex);
        return focused || target.isFocused();
    }

    private static void applyRokidRailSelection(
            final ArrayList<View> actions,
            final int selectedIndex
    ) {
        for (int i = 0; i < actions.size(); i++) {
            actions.get(i).setSelected(i == selectedIndex);
        }
    }

    private static ArrayList<View> collectRokidRailTargets(final View root) {
        final ArrayList<View> railTargets = new ArrayList<>();
        final ArrayList<View> targets = collectTargets(root);
        for (final View target : targets) {
            if (hasAncestorWithId(target, R.id.rokidActionRail)) {
                railTargets.add(target);
            }
        }
        return railTargets;
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
        final boolean playerControlsVisible = isViewWithIdShown(root, R.id.playbackControlRoot);
        targets.sort(Comparator
                .comparingInt((View view) -> getPriority(
                        view, errorPanelVisible, playerControlsVisible))
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

    private static boolean clickVisibleVideoPlayer(final Activity activity, final View root) {
        final View target = findVisibleViewById(root, R.id.detail_thumbnail_root_layout);
        if (target == null || !target.isEnabled()) {
            return false;
        }

        dispatchTap(activity, target);
        focusFirstRokidRailTarget(activity);
        return true;
    }

    private static void dispatchTap(final Activity activity, final View target) {
        final View touchRoot = activity.getWindow().getDecorView();
        final Rect targetRect = getScreenRect(target);
        final Rect rootRect = getScreenRect(touchRoot);
        final long downTime = SystemClock.uptimeMillis();
        final float x = targetRect.centerX() - rootRect.left;
        final float y = targetRect.centerY() - rootRect.top;
        final MotionEvent down = MotionEvent.obtain(
                downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
        final MotionEvent up = MotionEvent.obtain(
                downTime, downTime + 50, MotionEvent.ACTION_UP, x, y, 0);
        try {
            touchRoot.dispatchTouchEvent(down);
            touchRoot.dispatchTouchEvent(up);
        } finally {
            down.recycle();
            up.recycle();
        }
    }

    private static View findVisibleViewById(final View view, final int id) {
        if (view == null || !view.isShown()) {
            return null;
        }
        if (view.getId() == id) {
            return view;
        }
        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                final View target = findVisibleViewById(group.getChildAt(i), id);
                if (target != null) {
                    return target;
                }
            }
        }
        return null;
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
            final int toolbarTarget = findRightmostToolbarTarget(targets);
            if (toolbarTarget >= 0) {
                return toolbarTarget;
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

    private static int findRightmostToolbarTarget(final ArrayList<View> targets) {
        int targetIndex = -1;
        int right = Integer.MIN_VALUE;
        for (int i = 0; i < targets.size(); i++) {
            final View target = targets.get(i);
            if (hasAncestorWithId(target, R.id.toolbar_layout)) {
                final int targetRight = getScreenRect(target).right;
                if (targetRight > right) {
                    right = targetRight;
                    targetIndex = i;
                }
            }
        }
        return targetIndex;
    }

    private static Rect getScreenRect(final View view) {
        final Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);
        return rect;
    }

    private static int getPriority(
            final View view,
            final boolean errorPanelVisible,
            final boolean playerControlsVisible
    ) {
        if (playerControlsVisible && hasAncestorWithId(view, R.id.rokidActionRail)) {
            return 0;
        }
        if (playerControlsVisible && hasAncestorWithId(view, R.id.playbackControlRoot)) {
            return 1;
        }
        if (errorPanelVisible && hasAncestorWithId(view, R.id.error_panel)) {
            return playerControlsVisible ? 2 : 0;
        }
        if (hasAncestorWithId(view, R.id.toolbar_layout) || view.getId() == R.id.action_search) {
            return errorPanelVisible || playerControlsVisible ? 3 : 0;
        }
        return errorPanelVisible || playerControlsVisible ? 4 : 1;
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
        final boolean clicked = target.performClick();
        if (clicked && !(target instanceof EditText)
                && !hasAncestorWithId(target, R.id.rokidActionRail)) {
            focusFirstRokidRailTarget(activity);
        }
        return clicked;
    }

    private static void focusFirstRokidRailTarget(final Activity activity) {
        final View root = activity.findViewById(android.R.id.content);
        if (!isViewWithIdShown(root, R.id.rokidActionRail)) {
            return;
        }

        final ArrayList<View> targets = collectRokidRailTargets(root);
        if (!targets.isEmpty()) {
            focusRokidRailTarget(activity, targets, 0);
        }
    }
}
