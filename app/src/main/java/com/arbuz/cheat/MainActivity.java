package com.arbuz.cheat;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;

public class MainActivity extends Activity {

    LinearLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private void showHome() {
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(30, 30, 30, 30);
        layout.setBackgroundColor(Color.rgb(15, 15, 15));

        TextView title = new TextView(this);
        title.setText("ARBUZ CHEAT");
        title.setTextColor(Color.RED);
        title.setTextSize(32);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("ANDROID EDITION");
        subtitle.setTextColor(Color.WHITE);
        subtitle.setTextSize(14);

        Button button = new Button(this);
        button.setText("ОТКРЫТЬ МЕНЮ");
        button.setOnClickListener(v -> {
    android.widget.Toast.makeText(this, "КНОПКА РАБОТАЕТ", android.widget.Toast.LENGTH_SHORT).show();
    showMenu();
});

        layout.addView(title);
        layout.addView(subtitle);
        layout.addView(button);

        setContentView(layout);
    }

    private void showMenu() {
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);
        layout.setBackgroundColor(Color.rgb(15, 15, 15));

        TextView title = new TextView(this);
        title.setText("ARBUZ CHEAT MENU");
        title.setTextColor(Color.RED);
        title.setTextSize(26);

        Button settings = new Button(this);
        settings.setText("НАСТРОЙКИ");

        Button profile = new Button(this);
        profile.setText("ПРОФИЛЬ");

        Button back = new Button(this);
        back.setText("НАЗАД");
        back.setOnClickListener(v -> showHome());

        layout.addView(title);
        layout.addView(settings);
        layout.addView(profile);
        layout.addView(back);

        setContentView(layout);
    }
}
