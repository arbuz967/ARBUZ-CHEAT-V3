
package com.arbuz.cheat;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;
import android.content.SharedPreferences;

public class MainActivity extends Activity {

    LinearLayout layout;
    SharedPreferences prefs;

    int bg = Color.rgb(15, 15, 15);
    int red = Color.RED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("arbuz_settings", MODE_PRIVATE);
        showHome();
    }

    private TextView makeTitle(String text, int size) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(red);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        return b;
    }

    private void prepareScreen() {
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(30, 30, 30, 30);
        layout.setBackgroundColor(bg);
    }

    private void showHome() {
        prepareScreen();

        TextView title = makeTitle("ARBUZ CHEAT", 32);
        TextView subtitle = makeTitle("ANDROID EDITION", 14);
        subtitle.setTextColor(Color.WHITE);

        Button open = makeButton("ОТКРЫТЬ МЕНЮ");
        open.setOnClickListener(v -> {
            Toast.makeText(this, "КНОПКА РАБОТАЕТ",
                Toast.LENGTH_SHORT).show();
            showMenu();
        });

        layout.addView(title);
        layout.addView(subtitle);
        layout.addView(open);
        setContentView(layout);
    }

    private void showMenu() {
        prepareScreen();

        layout.addView(makeTitle("ARBUZ CHEAT MENU", 26));

        Button settings = makeButton("НАСТРОЙКИ");
        settings.setOnClickListener(v -> showSettings());

        Button profile = makeButton("ПРОФИЛЬ");
        profile.setOnClickListener(v -> showProfile());

        Button back = makeButton("НАЗАД");
        back.setOnClickListener(v -> showHome());

        layout.addView(settings);
        layout.addView(profile);
        layout.addView(back);

        setContentView(layout);
    }

    private void showSettings() {
        prepareScreen();

        layout.addView(makeTitle("НАСТРОЙКИ", 28));

        Switch sound = new Switch(this);
        sound.setText("ЗВУК");
        sound.setTextColor(Color.WHITE);
        sound.setChecked(prefs.getBoolean("sound", true));
        sound.setOnCheckedChangeListener((button, checked) ->
            prefs.edit().putBoolean("sound", checked).apply());

        Switch vibration = new Switch(this);
        vibration.setText("ВИБРАЦИЯ");
        vibration.setTextColor(Color.WHITE);
        vibration.setChecked(prefs.getBoolean("vibration", true));
        vibration.setOnCheckedChangeListener((button, checked) ->
            prefs.edit().putBoolean("vibration", checked).apply());

        Button back = makeButton("НАЗАД");
        back.setOnClickListener(v -> showMenu());

        layout.addView(sound);
        layout.addView(vibration);
        layout.addView(back);

        setContentView(layout);
    }

    private void showProfile() {
        prepareScreen();

        layout.addView(makeTitle("ПРОФИЛЬ", 28));

        TextView info = new TextView(this);
        info.setText("ARBUZ CHEAT V3\nANDROID EDITION\nВерсия: 1.0");
        info.setTextColor(Color.WHITE);
        info.setTextSize(17);
        info.setGravity(Gravity.CENTER);

        Button back = makeButton("НАЗАД");
        back.setOnClickListener(v -> showMenu());

        layout.addView(info);
        layout.addView(back);

        setContentView(layout);
    }

    @Override
    public void onBackPressed() {
        showMenu();
    }
}
