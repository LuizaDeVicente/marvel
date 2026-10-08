package com.example.marvelcerto;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/** Simple native components shared by the game and the local player profile. */
abstract class GameScreen extends AppCompatActivity {
    protected LinearLayout content;
    protected FrameLayout root;
    protected LinearLayout shell;
    protected void createScreen(boolean footer) {
        root = new FrameLayout(this);
        root.setBackgroundColor(getColor(R.color.ink));
        shell = new LinearLayout(this); shell.setOrientation(LinearLayout.VERTICAL);
        root.addView(shell, new FrameLayout.LayoutParams(-1, -1));
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(12), dp(20), dp(footer ? 96 : 24));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        if (footer) {
            BottomNavigationView nav = new BottomNavigationView(this);
            nav.inflateMenu(R.menu.bottom_nav_menu);
            nav.setBackgroundColor(getColor(R.color.ink_2));
            nav.setItemIconTintList(ColorStateList.valueOf(getColor(R.color.marvel_red)));
            nav.setItemTextColor(ColorStateList.valueOf(getColor(R.color.text_primary)));
            nav.setLabelVisibilityMode(1);
            nav.setSelectedItemId(R.id.nav_profile);
            nav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_profile) return true;
                Class<?> target = id == R.id.nav_home ? MainActivity.class : id == R.id.nav_search ? SearchActivity.class
                        : id == R.id.nav_favorites ? FavoritesActivity.class : CharacterPickerActivity.class;
                startActivity(new Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                finish(); return true;
            });
            shell.addView(nav, new LinearLayout.LayoutParams(-1, -2));
            FloatingActionButton fab = new FloatingActionButton(this);
            fab.setImageResource(R.drawable.ic_quiz);
            fab.setContentDescription("Jogar: Quem é o personagem?");
            fab.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.marvel_red)));
            fab.setImageTintList(ColorStateList.valueOf(getColor(R.color.text_primary)));
            FrameLayout.LayoutParams size = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.BOTTOM | Gravity.END);
            size.setMargins(dp(16), 0, dp(16), dp(96)); root.addView(fab, size);
            fab.setOnClickListener(v -> startActivity(new Intent(this, QuizActivity.class)));
        }
    }
    protected int dp(int size) { return Math.round(size * getResources().getDisplayMetrics().density); }
    protected TextView text(LinearLayout parent, String value, int size, boolean bold) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size);
        view.setTextColor(getColor(bold ? R.color.text_primary : R.color.text_secondary));
        if (bold) view.setTypeface(null, Typeface.BOLD);
        view.setPadding(0, dp(8), 0, dp(8));
        parent.addView(view, new LinearLayout.LayoutParams(-1, -2)); return view;
    }
    protected MaterialButton button(LinearLayout parent, String label, boolean primary, Runnable action) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label); button.setTextSize(14); button.setAllCaps(false); button.setMinHeight(dp(52));
        button.setCornerRadius(dp(12));
        button.setTextColor(getColor(R.color.text_primary));
        button.setBackgroundTintList(ColorStateList.valueOf(getColor(primary ? R.color.marvel_red : R.color.ink_3)));
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(-1, -2); size.topMargin = dp(8);
        parent.addView(button, size); button.setOnClickListener(v -> action.run()); return button;
    }
    protected LinearLayout card(LinearLayout parent) {
        LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL);
        view.setBackgroundResource(R.drawable.bg_profile_card); view.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(-1, -2); size.topMargin = dp(12);
        parent.addView(view, size); return view;
    }
}
