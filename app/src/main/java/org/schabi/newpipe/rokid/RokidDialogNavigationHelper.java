package org.schabi.newpipe.rokid;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public final class RokidDialogNavigationHelper {
    private RokidDialogNavigationHelper() {
    }

    public static void attach(
            @NonNull final Activity activity,
            @NonNull final Dialog dialog
    ) {
        if (!RokidMode.enabled()) {
            return;
        }

        dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }
            if (RokidKeyboardController.forActivity(activity).handleKeyEvent(event)) {
                return true;
            }
            if (event.getRepeatCount() > 0
                    && RokidKeyMapper.isDirectionalKey(event.getKeyCode())) {
                return true;
            }

            final Window window = dialog.getWindow();
            if (window == null) {
                return false;
            }
            final View root = window.getDecorView();
            return root != null && RokidFocusNavigator.handle(activity, root, event);
        });
    }

    public static void attach(
            @NonNull final Context context,
            @NonNull final Dialog dialog
    ) {
        final Activity activity = findActivity(context);
        if (activity != null) {
            attach(activity, dialog);
        }
    }

    @NonNull
    public static AlertDialog show(
            @NonNull final Context context,
            @NonNull final AlertDialog.Builder builder
    ) {
        final AlertDialog dialog = builder.create();
        attach(context, dialog);
        dialog.show();
        return dialog;
    }

    private static Activity findActivity(@NonNull final Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }
}
