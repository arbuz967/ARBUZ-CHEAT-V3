package com.arbuz.cheat;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Switch;
import android.widget.ScrollView;
import android.provider.Settings;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private LinearLayout layout;
    private SharedPreferences prefs;

    private final int BLACK = Color.rgb(8, 8, 8);
    private final int PANEL = Color.rgb(20, 20, 20);
    private final int RED = Color.rgb(255, 20, 35);
    private final int WHITE = Color.WHITE;
    private String currentScreen = "home";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(BLACK);
        getWindow().setNavigationBarColor(BLACK);

        prefs = getSharedPreferences("arbuz_settings", MODE_PRIVATE);
        showHome();
    }

  private void showKeyScreen() {
    prepareScreen("key");
    addHeader();

    layout.addView(makeTitle("АКТИВАЦИЯ", 25));
    layout.addView(makeText("Введи лицензионный ключ", 16));

    EditText keyInput = new EditText(this);
    keyInput.setSingleLine(true);
    keyInput.setHint("Лицензионный ключ");
    keyInput.setHintTextColor(Color.GRAY);
    keyInput.setTextColor(WHITE);
    keyInput.setPadding(dp(15), dp(12), dp(15), dp(12));
    keyInput.setBackground(shape(PANEL, RED));
    layout.addView(keyInput, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(60)));

    addSpace(15);

    Button activate = makeButton("АКТИВИРОВАТЬ");
    layout.addView(activate);

    activate.setOnClickListener(v -> {
        String key = keyInput.getText().toString().trim();

        if (key.isEmpty()) {
            Toast.makeText(this, "Введи ключ", Toast.LENGTH_SHORT).show();
            return;
        }

        activate.setEnabled(false);
        activate.setText("ПРОВЕРКА...");

        Executors.newSingleThreadExecutor().execute(() -> {
            boolean valid = false;

            try {
                String deviceId = Settings.Secure.getString(
                        getContentResolver(),
                        Settings.Secure.ANDROID_ID
                );

                URL url = new URL(
                    "https://itktpasdhilihrqckioz.supabase.co/functions/v1/verify-key"
                );

                HttpURLConnection conn =
                        (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("apikey",
                        "sb_publishable_RMX3aGldGlEyzrL5l4XCCA_L6OWoNzC");

                JSONObject data = new JSONObject();
                data.put("key", key);
                data.put("installation_id", deviceId);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(data.toString().getBytes("UTF-8"));
                }

                int status = conn.getResponseCode();

                java.io.InputStream stream = status >= 200 && status < 300
                        ? conn.getInputStream()
                        : conn.getErrorStream();

                java.util.Scanner scanner =
                        new java.util.Scanner(stream).useDelimiter("\\A");

                String response = scanner.hasNext() ? scanner.next() : "";
                scanner.close();
                conn.disconnect();

                JSONObject result = new JSONObject(response);
                valid = status >= 200 && status < 300
                        && result.optBoolean("ok", false);

            } catch (Exception e) {
                valid = false;
            }

            final boolean success = valid;

            runOnUiThread(() -> {
                if (success) {
                    showHome();
                } else {
                    activate.setEnabled(true);
                    activate.setText("АКТИВИРОВАТЬ");
                    Toast.makeText(this,
                            "Ключ недействителен или ошибка соединения",
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    });
            }
    
    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(18));
        drawable.setStroke(dp(2), stroke);
        return drawable;
    }

    private TextView makeTitle(String text, int size) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextColor(RED);
        title.setTextSize(size);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(12), 0, dp(12));
        return title;
    }

    private TextView makeText(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(WHITE);
        view.setTextSize(size);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(10), dp(10), dp(10), dp(10));
        return view;
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(RED);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setBackground(shape(PANEL, RED));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(62));

        params.setMargins(0, dp(9), 0, dp(9));
        button.setLayoutParams(params);

        return button;
    }

    private void prepareScreen(String screen) {
        currentScreen = screen;

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BLACK);

        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(dp(24), dp(25), dp(24), dp(25));
        layout.setBackgroundColor(BLACK);

        scroll.addView(layout);
        setContentView(scroll);

        AlphaAnimation animation =
                new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(250);
        layout.startAnimation(animation);
    }

    private void addSpace(int height) {
        View space = new View(this);
        layout.addView(space, new LinearLayout.LayoutParams(
                1, dp(height)));
    }

    private void addHeader() {
        addSpace(35);
        layout.addView(makeTitle("🍉", 55));
        layout.addView(makeTitle("ARBUZ CHEAT", 31));
        layout.addView(makeTitle("V3", 27));
        TextView edition = makeText("ANDROID EDITION", 12);
        edition.setTextColor(Color.LTGRAY);
        layout.addView(edition);
        addSpace(35);
    }

    private void showHome() {
        prepareScreen("home");

        addHeader();

        Button open = makeButton("ОТКРЫТЬ МЕНЮ");
        open.setOnClickListener(v -> showMenu());
        layout.addView(open);

        addSpace(70);

        TextView footer = makeText("ARBUZ CHEAT V3", 12);
        footer.setTextColor(Color.GRAY);
        layout.addView(footer);
    }

    private void showMenu() {
        prepareScreen("menu");

        addHeader();
        layout.addView(makeTitle("ГЛАВНОЕ МЕНЮ", 23));

        Button settings = makeButton("⚙  НАСТРОЙКИ");
        settings.setOnClickListener(v -> showSettings());

        Button profile = makeButton("👤  ПРОФИЛЬ");
        profile.setOnClickListener(v -> showProfile());

        Button about = makeButton("ⓘ  ИНФОРМАЦИЯ");
        about.setOnClickListener(v -> showAbout());

        Button back = makeButton("←  НАЗАД");
        back.setOnClickListener(v -> showHome());

        layout.addView(settings);
        layout.addView(profile);
        layout.addView(about);
        layout.addView(back);
    }

    private void showSettings() {
        prepareScreen("settings");

        addSpace(30);
        layout.addView(makeTitle("НАСТРОЙКИ", 28));
        addSpace(25);

        Switch sound = makeSwitch("ЗВУК", "sound");
        Switch vibration = makeSwitch("ВИБРАЦИЯ", "vibration");

        layout.addView(sound);
        layout.addView(vibration);

        addSpace(25);

        Button back = makeButton("←  НАЗАД");
        back.setOnClickListener(v -> showMenu());
        layout.addView(back);
    }

    private Switch makeSwitch(String title, String key) {
        Switch toggle = new Switch(this);
        toggle.setText(title);
        toggle.setTextColor(WHITE);
        toggle.setTextSize(17);
        toggle.setPadding(dp(12), dp(15), dp(12), dp(15));

        toggle.setChecked(prefs.getBoolean(key, true));

        toggle.setThumbTintList(new ColorStateList(
                new int[][] {
                    new int[] { android.R.attr.state_checked },
                    new int[] {}
                },
                new int[] { RED, Color.LTGRAY }
        ));

        toggle.setTrackTintList(new ColorStateList(
                new int[][] {
                    new int[] { android.R.attr.state_checked },
                    new int[] {}
                },
                new int[] {
                    Color.rgb(130, 20, 30),
                    Color.DKGRAY
                }
        ));

        toggle.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean(key, checked).apply());

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(75));

        params.setMargins(0, dp(8), 0, dp(8));
        toggle.setLayoutParams(params);
        toggle.setBackground(shape(PANEL, Color.DKGRAY));

        return toggle;
    }

    private void showProfile() {
        prepareScreen("profile");

        addSpace(40);
        layout.addView(makeTitle("ПРОФИЛЬ", 28));
        addSpace(30);

        layout.addView(makeTitle("🍉", 60));
        layout.addView(makeText("ARBUZ CHEAT V3", 21));
        layout.addView(makeText("ANDROID EDITION", 14));
        layout.addView(makeText("Версия: 1.0", 16));

        addSpace(30);

        Button back = makeButton("←  НАЗАД");
        back.setOnClickListener(v -> showMenu());
        layout.addView(back);
    }

    private void showAbout() {
        prepareScreen("about");

        addSpace(35);
        layout.addView(makeTitle("ИНФОРМАЦИЯ", 27));
        addSpace(20);

        layout.addView(makeTitle("🍉", 55));
        layout.addView(makeText("ARBUZ CHEAT V3", 22));
        layout.addView(makeText("Платформа: Android", 16));
        layout.addView(makeText("Версия: 1.0", 16));
        layout.addView(makeText("Разработчик: ARBUZ", 16));

        addSpace(30);

        Button back = makeButton("←  НАЗАД");
        back.setOnClickListener(v -> showMenu());
        layout.addView(back);
    }

    @Override
    public void onBackPressed() {
        if (currentScreen.equals("home")) {
            super.onBackPressed();
        } else if (currentScreen.equals("menu")) {
            showHome();
        } else {
            showMenu();
        }
    }
}
