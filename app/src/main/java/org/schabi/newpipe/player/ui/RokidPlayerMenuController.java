package org.schabi.newpipe.player.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;

import org.schabi.newpipe.R;
import org.schabi.newpipe.rokid.RokidMode;

import java.util.ArrayList;

final class RokidPlayerMenuController {
    @NonNull
    private final Context context;
    @NonNull
    private final ViewGroup host;
    @NonNull
    private final PopupMenu.OnMenuItemClickListener itemClickListener;
    @NonNull
    private final PopupMenu.OnDismissListener dismissListener;
    @Nullable
    private FrameLayout overlay;
    @Nullable
    private LinearLayout panel;
    private final ArrayList<MenuItem> menuItems = new ArrayList<>();
    private final ArrayList<TextView> itemViews = new ArrayList<>();
    private int selectedIndex = -1;

    RokidPlayerMenuController(
            @NonNull final Context context,
            @NonNull final ViewGroup host,
            @NonNull final PopupMenu.OnMenuItemClickListener itemClickListener,
            @NonNull final PopupMenu.OnDismissListener dismissListener
    ) {
        this.context = context;
        this.host = host;
        this.itemClickListener = itemClickListener;
        this.dismissListener = dismissListener;
    }

    boolean show(@Nullable final PopupMenu popupMenu) {
        if (!RokidMode.enabled() || popupMenu == null || popupMenu.getMenu().size() == 0) {
            return false;
        }
        ensureOverlay();
        if (overlay == null || panel == null) {
            return false;
        }

        menuItems.clear();
        for (int index = 0; index < popupMenu.getMenu().size(); index++) {
            final MenuItem item = popupMenu.getMenu().getItem(index);
            if (item.isVisible() && item.isEnabled()) {
                menuItems.add(item);
            }
        }
        if (menuItems.isEmpty()) {
            return false;
        }

        panel.removeAllViews();
        itemViews.clear();
        for (int index = 0; index < menuItems.size(); index++) {
            final TextView itemView = makeItemView(index);
            itemViews.add(itemView);
            panel.addView(itemView, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        selectedIndex = 0;
        overlay.setVisibility(View.VISIBLE);
        updateSelection();
        final TextView firstItemView = itemViews.get(0);
        firstItemView.requestFocusFromTouch();
        firstItemView.requestFocus();
        return true;
    }

    boolean isVisible() {
        return overlay != null && overlay.getVisibility() == View.VISIBLE;
    }

    boolean moveFocus(final boolean forward) {
        if (!isVisible() || itemViews.isEmpty()) {
            return false;
        }

        if (selectedIndex < 0) {
            selectedIndex = forward ? 0 : itemViews.size() - 1;
        } else if (forward) {
            selectedIndex = selectedIndex == itemViews.size() - 1 ? 0 : selectedIndex + 1;
        } else {
            selectedIndex = selectedIndex == 0 ? itemViews.size() - 1 : selectedIndex - 1;
        }

        updateSelection();
        final TextView target = itemViews.get(selectedIndex);
        target.requestFocusFromTouch();
        target.requestFocus();
        return true;
    }

    boolean activateSelected() {
        if (!isVisible() || selectedIndex < 0 || selectedIndex >= menuItems.size()) {
            return false;
        }

        activateItem(selectedIndex);
        return true;
    }

    boolean dismiss(final boolean notifyDismiss) {
        if (!isVisible()) {
            return false;
        }

        overlay.setVisibility(View.GONE);
        menuItems.clear();
        itemViews.clear();
        selectedIndex = -1;
        if (notifyDismiss) {
            dismissListener.onDismiss(null);
        }
        return true;
    }

    void destroy() {
        dismiss(false);
        if (overlay != null && overlay.getParent() instanceof ViewGroup) {
            ((ViewGroup) overlay.getParent()).removeView(overlay);
        }
        overlay = null;
        panel = null;
    }

    private void ensureOverlay() {
        if (overlay == null) {
            overlay = new FrameLayout(context);
            overlay.setId(R.id.rokidPlayerMenuOverlay);
            overlay.setVisibility(View.GONE);
            overlay.setFocusable(false);
            overlay.setClickable(false);
            overlay.setContentDescription(context.getString(R.string.rokid_player_actions));

            panel = new LinearLayout(context);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setPadding(dp(8), dp(8), dp(8), dp(8));
            panel.setBackground(panelBackground());
            panel.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

            final FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                    dp(300), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
            overlay.addView(panel, panelParams);
        }

        if (overlay.getParent() == null) {
            host.addView(overlay, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    private TextView makeItemView(final int index) {
        final MenuItem menuItem = menuItems.get(index);
        final TextView itemView = new TextView(context);
        itemView.setText(menuItem.getTitle());
        itemView.setTextColor(Color.WHITE);
        itemView.setTextSize(18);
        itemView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        itemView.setGravity(Gravity.CENTER_VERTICAL);
        itemView.setMinHeight(dp(48));
        itemView.setPadding(dp(14), dp(8), dp(14), dp(8));
        itemView.setClickable(true);
        itemView.setFocusable(true);
        itemView.setFocusableInTouchMode(true);
        itemView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        itemView.setOnClickListener(view -> activateItem(index));
        itemView.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                selectFromAccessibility(index);
            }
        });
        itemView.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public boolean performAccessibilityAction(
                    final View view,
                    final int action,
                    final Bundle args
            ) {
                if (action == R.id.rokid_accessibility_select) {
                    selectFromAccessibility(index);
                    return true;
                }
                if (action == AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS
                        || action == AccessibilityNodeInfo.ACTION_FOCUS) {
                    selectFromAccessibility(index);
                }
                return super.performAccessibilityAction(view, action, args);
            }

            @Override
            public void onInitializeAccessibilityNodeInfo(
                    final View view,
                    final AccessibilityNodeInfo info
            ) {
                super.onInitializeAccessibilityNodeInfo(view, info);
                info.setClickable(true);
                info.setFocusable(true);
                info.setSelected(index == selectedIndex);
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(
                        R.id.rokid_accessibility_select,
                        view.getContext().getString(R.string.rokid_accessibility_select)));
            }
        });
        return itemView;
    }

    private void selectFromAccessibility(final int index) {
        if (index < 0 || index >= itemViews.size() || selectedIndex == index) {
            return;
        }

        selectedIndex = index;
        updateSelection();
    }

    private void activateItem(final int index) {
        if (index < 0 || index >= menuItems.size()) {
            return;
        }

        final MenuItem menuItem = menuItems.get(index);
        dismiss(false);
        itemClickListener.onMenuItemClick(menuItem);
        dismissListener.onDismiss(null);
    }

    private void updateSelection() {
        for (int index = 0; index < itemViews.size(); index++) {
            final TextView itemView = itemViews.get(index);
            final boolean selected = index == selectedIndex;
            itemView.setSelected(selected);
            itemView.setTextColor(Color.WHITE);
            itemView.setContentDescription(menuItems.get(index).getTitle());
            itemView.setBackground(itemBackground(selected));
        }
    }

    private GradientDrawable panelBackground() {
        final GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.BLACK);
        drawable.setStroke(dp(2), Color.WHITE);
        drawable.setCornerRadius(dp(3));
        return drawable;
    }

    private GradientDrawable itemBackground(final boolean selected) {
        final GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.BLACK);
        drawable.setStroke(dp(selected ? 2 : 1),
                selected ? Color.WHITE : Color.rgb(120, 120, 120));
        drawable.setCornerRadius(dp(3));
        return drawable;
    }

    private int dp(final int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
