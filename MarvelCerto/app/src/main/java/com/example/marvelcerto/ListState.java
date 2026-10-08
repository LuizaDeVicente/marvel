package com.example.marvelcerto;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

final class ListState {
    static void show(Activity screen, String message, boolean loading, Runnable retry) {
        View container = screen.findViewById(R.id.listState);
        container.setVisibility(message == null ? View.GONE : View.VISIBLE);
        container.setImportantForAccessibility(message == null ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                : View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        ((TextView) screen.findViewById(R.id.listMessage)).setText(message);
        screen.findViewById(R.id.listMessage).setContentDescription(message);
        screen.findViewById(R.id.listProgress).setVisibility(loading ? View.VISIBLE : View.GONE);
        View button = screen.findViewById(R.id.listRetry);
        button.setVisibility(retry == null ? View.GONE : View.VISIBLE);
        button.setOnClickListener(retry == null ? null : v -> retry.run());
    }
}
