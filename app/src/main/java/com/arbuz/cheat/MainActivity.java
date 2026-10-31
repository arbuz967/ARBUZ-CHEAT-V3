
package com.arbuz.cheat;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.EditText;
import android.widget.Toast;
import android.provider.Settings;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private LinearLayout layout;
    private SharedPreferences prefs;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final int BLACK = Color.rgb(8, 8, 8);
    private final int PANEL = Color.rgb(20, 20, 20);
    private final int RED = Color.rgb(255, 20, 35);
    private final int WHITE = Color.WHITE;

    private String currentScreen = "key";

    private static final String SUPABASE_URL =
            "https://itktpasdhilihrqckioz.supabase.co";

    private static final String SUPABASE_ANON_KEY =
            "sb_publishable_RMX3aGldGlEyzrL5l4XCCA_L6OWoNzC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(BLACK);
        getWindow().setNavigationBarColor(BLACK);

        prefs = getSharedPreferences(
                "arbuz_settings", MODE_PRIVATE);

        String savedKey =
                prefs.getString("license_key", "");

        if (savedKey.isEmpty()) {
            showKeyScreen();
        } else {
            checkSavedKey(savedKey);
        }
    }

    private String getInstallationId() {
        String id = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        return id == null ? "" : id;
    }

    private JSONObject requestLicense(
            String function, String key) throws Exception {

        HttpURLConnection conn = null;

        try {
            URL url = new URL(
                    SUPABASE_URL + "/functions/v1/" + function
            );

            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);

            conn.setRequestProperty(
                    "Content-Type", "application/json");

            conn.setRequestProperty(
                    "apikey", SUPABASE_ANON_KEY);

            JSONObject body = new JSONObject();

            body.put("key", key);
            body.put("installation_id", getInstallationId());

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes("UTF-8"));
            }

            int status = conn.getResponseCode();

            InputStream stream =
                    status >= 200 && status < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            if (stream == null) {
                throw new Exception("Empty response");
            }

            Scanner scanner = new Scanner(
                    stream, "UTF-8").useDelimiter("\\A");

            String response =
                    scanner.hasNext() ? scanner.next() : "";

            scanner.close();

            if (status < 200 || status >= 300) {
                throw new Exception("HTTP " + status);
            }

            return new JSONObject(response);

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void checkSavedKey(String key) {

        prepareScreen("checking");
        addHeader();

        layout.addView(
                makeTitle("ПРОВЕРКА ЛИЦЕНЗИИ", 21));

        layout.addView(
                makeText("Пожалуйста, подожди...", 15));

        executor.execute(() -> {

            boolean valid = false;

            try {
                JSONObject result =
                        requestLicense("check-key", key);

                valid = result.optBoolean("ok", false);

            } catch (Exception ignored) {
            }

            final boolean success = valid;

            runOnUiThread(() -> {

                if (isFinishing()) return;

                if (success) {
                    showHome();

                } else {
                    prefs.edit()
                            .remove("license_key")
                            .apply();

                    showKeyScreen();

                    Toast.makeText(
                            this,
                            "Не удалось подтвердить ключ. Проверь интернет и ключ.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
        });
    }

    private void showKeyScreen() {

        prepareScreen("key");
        addHeader();

        layout.addView(
                makeTitle("АКТИВАЦИЯ", 25));

        layout.addView(
                makeText("Введи лицензионный ключ", 16));

        EditText keyInput = new EditText(this);

        keyInput.setSingleLine(true);
        keyInput.setHint("Лицензионный ключ");
        keyInput.setHintTextColor(Color.GRAY);
        keyInput.setTextColor(WHITE);

        keyInput.setPadding(
                dp(15), dp(12), dp(15), dp(12));

        keyInput.setBackground(
                shape(PANEL, RED));

        layout.addView(
                keyInput,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(60)
                )
        );

        addSpace(15);

        Button activate =
                makeButton("АКТИВИРОВАТЬ");

        layout.addView(activate);

        activate.setOnClickListener(v -> {

            String key =
                    keyInput.getText().toString().trim();

            if (key.isEmpty()) {
                Toast.makeText(
                        this, "Введи ключ",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            activate.setEnabled(false);
            activate.setText("ПРОВЕРКА...");

            executor.execute(() -> {

                boolean valid = false;

                try {
                    JSONObject result =
                            requestLicense("verify-key", key);

                    valid = result.optBoolean("ok", false);

                } catch (Exception ignored) {
                }

                final boolean success = valid;

                runOnUiThread(() -> {

                    if (isFinishing()) return;

                    activate.setEnabled(true);
                    activate.setText("АКТИВИРОВАТЬ");

                    if (success) {

                        prefs.edit()
                                .putString("license_key", key)
                                .apply();

                        Toast.makeText(
                                this,
                                "Ключ активирован",
                                Toast.LENGTH_SHORT
                        ).show();

                        showHome();

                    } else {

                        Toast.makeText(
                                this,
                                "Ключ недействителен или нет соединения",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
            });
        });
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(
            int color, int stroke) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(dp(18));
        drawable.setStroke(dp(2), stroke);

        return drawable;
    }

    private TextView makeTitle(
            String text, int size) {

        TextView title = new TextView(this);

        title.setText(text);
        title.setTextColor(RED);
        title.setTextSize(size);

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setGravity(Gravity.CENTER);

        title.setPadding(
                0, dp(12), 0, dp(12));

        return title;
    }

    private TextView makeText(
            String text, int size) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextColor(WHITE);
        view.setTextSize(size);
        view.setGravity(Gravity.CENTER);

        view.setPadding(
                dp(10), dp(10), dp(10), dp(10));

        return view;
    }

    private Button makeButton(String text) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextColor(RED);
        button.setTextSize(16);

        button.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        button.setAllCaps(false);
        button.setBackground(shape(PANEL, RED));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(62)
                );

        params.setMargins(
                0, dp(9), 0, dp(9));

        button.setLayoutParams(params);

        return button;
    }

    private void prepareScreen(String screen) {

        currentScreen = screen;

        ScrollView scroll = new ScrollView(this);

        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BLACK);

        layout = new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL);

        layout.setGravity(
                Gravity.CENTER_HORIZONTAL);

        layout.setPadding(
                dp(24), dp(25), dp(24), dp(25));

        layout.setBackgroundColor(BLACK);

        scroll.addView(layout);

        setContentView(scroll);
    }

    private void addSpace(int height) {

        View space = new View(this);

        layout.addView(
                space,
                new LinearLayout.LayoutParams(
                        1, dp(height)
                )
        );
    }

    private void addHeader() {

        addSpace(35);

        layout.addView(
                makeTitle("🍉", 55));

        layout.addView(
                makeTitle("ARBUZ CHEAT", 31));

        layout.addView(
                makeTitle("V3", 27));

        layout.addView(
                makeText("ANDROID EDITION", 12));

        addSpace(35);
    }

    private void showHome() {

        prepareScreen("home");
        addHeader();

        Button open =
                makeButton("ОТКРЫТЬ МЕНЮ");

        open.setOnClickListener(v -> showMenu());

        layout.addView(open);
    }

    private void showMenu() {

        prepareScreen("menu");
        addHeader();

        layout.addView(
                makeTitle("ГЛАВНОЕ МЕНЮ", 23));

        Button settings =
                makeButton("НАСТРОЙКИ");

        settings.setOnClickListener(
                v -> showSettings());

        Button profile =
                makeButton("ПРОФИЛЬ");

        profile.setOnClickListener(
                v -> showProfile());

        Button about =
                makeButton("ИНФОРМАЦИЯ");

        about.setOnClickListener(
                v -> showAbout());

        Button back =
                makeButton("НАЗАД");

        back.setOnClickListener(
                v -> showHome());

        layout.addView(settings);
        layout.addView(profile);
        layout.addView(about);
        layout.addView(back);
    }

    private void showSettings() {

        prepareScreen("settings");

        addSpace(30);

        layout.addView(
                makeTitle("НАСТРОЙКИ", 28));

        Button back =
                makeButton("НАЗАД");

        back.setOnClickListener(
                v -> showMenu());

        layout.addView(back);
    }

    private void showProfile() {

        prepareScreen("profile");

        addSpace(40);

        layout.addView(
                makeTitle("ПРОФИЛЬ", 28));

        layout.addView(
                makeTitle("ARBUZ CHEAT V3", 21));

        layout.addView(
                makeText("ANDROID EDITION", 14));

        Button back =
                makeButton("НАЗАД");

        back.setOnClickListener(
                v -> showMenu());

        layout.addView(back);
    }

    private void showAbout() {

        prepareScreen("about");

        addSpace(35);

        layout.addView(
                makeTitle("ИНФОРМАЦИЯ", 27));

        layout.addView(
                makeText("ARBUZ CHEAT V3", 22));

        layout.addView(
                makeText("Платформа: Android", 16));

        layout.addView(
                makeText("Версия: 1.0", 16));

        Button back =
                makeButton("НАЗАД");

        back.setOnClickListener(
                v -> showMenu());

        layout.addView(back);
    }

    @Override
    public void onBackPressed() {

        if (currentScreen.equals("key")
                || currentScreen.equals("checking")) {
            return;
        }

        if (currentScreen.equals("home")) {
            super.onBackPressed();

        } else if (currentScreen.equals("menu")) {
            showHome();

        } else {
            showMenu();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
