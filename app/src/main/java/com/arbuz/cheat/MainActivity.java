
package com.arbuz.cheat;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private LinearLayout layout;
    private SharedPreferences prefs;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final int BLACK = Color.rgb(8, 8, 8);
    private final int PANEL = Color.rgb(22, 22, 22);
    private final int RED = Color.rgb(255, 20, 35);

    private String currentScreen = "activation";

    private static final String SUPABASE_URL =
            "https://itktpasdhilihrqckioz.supabase.co";

    private static final String SUPABASE_KEY =
            "sb_publishable_RMX3aGldGlEyzrL5l4XCCA_L6OWoNzC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(BLACK);
        getWindow().setNavigationBarColor(BLACK);

        prefs = getSharedPreferences(
                "arbuz_settings", MODE_PRIVATE);

        checkSavedLogin();
    }

    private String getInstallationId() {
        String id = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID);

        return id == null ? "unknown-device" : id;
    }

    private JSONObject postRequest(
            String endpoint, JSONObject data) throws Exception {

        URL url = new URL(
                SUPABASE_URL + "/functions/v1/" + endpoint);

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);

        connection.setRequestProperty(
                "Content-Type", "application/json");

        connection.setRequestProperty(
                "apikey", SUPABASE_KEY);

        try (OutputStream output =
                     connection.getOutputStream()) {

            output.write(data.toString().getBytes("UTF-8"));
        }

        int status = connection.getResponseCode();

        java.io.InputStream stream =
                status >= 200 && status < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

        String response;

        try (Scanner scanner =
                     new Scanner(stream).useDelimiter("\\A")) {

            response = scanner.hasNext()
                    ? scanner.next() : "";
        }

        connection.disconnect();

        if (status < 200 || status >= 300) {
            throw new Exception("HTTP " + status);
        }

        return new JSONObject(response);
    }

    private JSONObject checkKey(
            String endpoint, String key) throws Exception {

        JSONObject data = new JSONObject();

        data.put("key", key);
        data.put("installation_id", getInstallationId());

        return postRequest(endpoint, data);
    }

    private void checkSavedLogin() {

        prepareScreen("checking");
        addHeader();

        layout.addView(
                makeTitle("ПРОВЕРКА ДОСТУПА", 22));

        layout.addView(
                makeText("Подождите...", 15));

        String adminKey =
                prefs.getString("admin_key", "");

        String licenseKey =
                prefs.getString("license_key", "");

        executor.execute(() -> {

            boolean adminOk = false;
            boolean licenseOk = false;

            if (!adminKey.isEmpty()) {
                try {
                    adminOk = checkKey(
                            "verify-admin", adminKey)
                            .optBoolean("admin", false);
                } catch (Exception ignored) { }
            }

            if (!adminOk && !licenseKey.isEmpty()) {
                try {
                    licenseOk = checkKey(
                            "check-key", licenseKey)
                            .optBoolean("ok", false);
                } catch (Exception ignored) { }
            }

            final boolean a = adminOk;
            final boolean l = licenseOk;

            runOnUiThread(() -> {

                if (a || l) {
                    showMenu();
                } else {
                    prefs.edit()
                            .remove("admin_key")
                            .remove("license_key")
                            .apply();

                    showKeyScreen();
                }
            });
        });
    }

    private void showKeyScreen() {

        prepareScreen("activation");
        addHeader();

        layout.addView(makeTitle("АКТИВАЦИЯ", 25));

        EditText input =
                makeEditText("Лицензионный ключ");

        layout.addView(input, matchHeight(60));

        Button activate =
                makeButton("АКТИВИРОВАТЬ");

        layout.addView(activate);

        activate.setOnClickListener(v -> {

            String key =
                    input.getText().toString().trim();

            if (key.isEmpty()) {
                toast("Введи ключ");
                return;
            }

            activate.setEnabled(false);
            activate.setText("ПРОВЕРКА...");

            executor.execute(() -> {

                boolean valid = false;

                try {
                    valid = checkKey(
                            "verify-key", key)
                            .optBoolean("ok", false);
                } catch (Exception ignored) { }

                final boolean success = valid;

                runOnUiThread(() -> {

                    if (success) {

                        prefs.edit()
                                .putString("license_key", key)
                                .remove("admin_key")
                                .apply();

                        showMenu();

                    } else {

                        activate.setEnabled(true);
                        activate.setText("АКТИВИРОВАТЬ");

                        toast(
                                "Ключ недействителен или нет соединения");
                    }
                });
            });
        });
    }

    private void showAdminLogin() {

        prepareScreen("admin_login");
        addHeader();

        layout.addView(
                makeTitle("АДМИН-ДОСТУП", 25));

        EditText input =
                makeEditText("Админ-ключ");

        layout.addView(input, matchHeight(60));

        Button enter =
                makeButton("ПРОВЕРИТЬ ДОСТУП");

        layout.addView(enter);

        enter.setOnClickListener(v -> {

            String key =
                    input.getText().toString().trim();

            if (key.isEmpty()) {
                toast("Введи админ-ключ");
                return;
            }

            enter.setEnabled(false);
            enter.setText("ПРОВЕРКА...");

            executor.execute(() -> {

                boolean valid = false;

                try {
                    valid = checkKey(
                            "verify-admin", key)
                            .optBoolean("admin", false);
                } catch (Exception ignored) { }

                final boolean success = valid;

                runOnUiThread(() -> {

                    enter.setEnabled(true);
                    enter.setText("ПРОВЕРИТЬ ДОСТУП");

                    if (success) {

                        prefs.edit()
                                .putString("admin_key", key)
                                .remove("license_key")
                                .apply();

                        showAdminPanel();

                    } else {
                        toast("Доступ запрещён");
                    }
                });
            });
        });

        Button back = makeButton("НАЗАД");
        back.setOnClickListener(v -> showMenu());

        layout.addView(back);
    }

    private void showAdminPanel() {

        prepareScreen("admin");
        addHeader();

        layout.addView(
                makeTitle("АДМИН-ПАНЕЛЬ", 26));
        Button licenses = makeButton("СПИСОК ЛИЦЕНЗИЙ");
licenses.setOnClickListener(v -> loadLicenses());
layout.addView(licenses);

        layout.addView(
                makeText(
                        "АДМИН-ДОСТУП ПОДТВЕРЖДЁН",
                        15));

        layout.addView(
                makeText(
                        "Серверная проверка выполнена.",
                        14));

        layout.addView(
                makeText(
                        "Список лицензий будет подключён " +
                        "через отдельную серверную функцию.",
                        14));

        Button logout =
                makeButton("ВЫЙТИ ИЗ АДМИН-ПАНЕЛИ");

        logout.setOnClickListener(v -> {

            prefs.edit()
                    .remove("admin_key")
                    .apply();

            showMenu();
        });

        layout.addView(logout);

        Button back = makeButton("НАЗАД");
        back.setOnClickListener(v -> showAdminPanel());

        layout.addView(back);
    }
    
private void loadLicenses() {
    prepareScreen("admin");
    addHeader();

    layout.addView(makeTitle("СПИСОК ЛИЦЕНЗИЙ", 24));

    TextView status = makeText("Загрузка лицензий...", 15);
    layout.addView(status);

    Button refresh = makeButton("ОБНОВИТЬ СПИСОК");
    layout.addView(refresh);

    refresh.setOnClickListener(v -> loadLicenses());

    Button back = makeButton("НАЗАД");
    back.setOnClickListener(v -> showMenu());
    layout.addView(back);

    String key = prefs.getString("admin_key", "");

    executor.execute(() -> {
        try {
            JSONObject request = new JSONObject();
            request.put("key", key);
            request.put("installation_id", getInstallationId());

            JSONObject response =
                    postRequest("admin-list-licenses", request);

            if (!response.optBoolean("ok", false)) {
                throw new Exception("Доступ запрещён");
            }

            org.json.JSONArray licenses =
                    response.getJSONArray("licenses");

            runOnUiThread(() -> {
                layout.removeView(status);

                layout.addView(makeText(
                        "Всего лицензий: " + licenses.length(), 17));

                for (int i = 0; i < licenses.length(); i++) {
                    JSONObject item = licenses.optJSONObject(i);
                    if (item == null) continue;

                    String plan = item.optString("plan", "Неизвестно");
                    String id = item.optString("id", "—");
                    boolean disabled = item.optBoolean("disabled", false);

                    String state = disabled ? "ОТКЛЮЧЕНА" : "АКТИВНА";

                    String expires = item.isNull("expires_at")
                            ? "Навсегда"
                            : item.optString("expires_at");

                    String device = item.optString(
                            "installation_id", "Не привязано");

                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setPadding(dp(12), dp(12), dp(12), dp(12));
                    card.setBackground(shape(PANEL, RED));

                    LinearLayout.LayoutParams params =
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);

                    params.setMargins(0, dp(7), 0, dp(7));
                    card.setLayoutParams(params);

                    card.addView(makeText("Лицензия #" + id, 15));
                    card.addView(makeText("Тариф: " + plan, 14));
                    card.addView(makeText("Статус: " + state, 14));
                    card.addView(makeText("Окончание: " + expires, 13));
                    card.addView(makeText("Устройство: " + device, 12));

                    layout.addView(card);
                }
            });

        } catch (Exception e) {
            runOnUiThread(() -> {
                status.setText(
                        "Ошибка загрузки: " + e.getMessage());
            });
        }
    });
}


    private void showMenu() {

        prepareScreen("menu");
        addHeader();

        layout.addView(
                makeTitle("ГЛАВНОЕ МЕНЮ", 23));

        Button launch =
                makeButton("ЗАПУСК ПАНЕЛИ");

        launch.setOnClickListener(v ->
                showLaunchPanel());

        layout.addView(launch);

        Button admin =
                makeButton("АДМИН-ПАНЕЛЬ");

        admin.setOnClickListener(v ->
                showAdminLogin());

        layout.addView(admin);

        Button settings =
                makeButton("НАСТРОЙКИ");

        settings.setOnClickListener(v ->
                showSettings());

        layout.addView(settings);

        Button profile =
                makeButton("ПРОФИЛЬ");

        profile.setOnClickListener(v ->
                showProfile());

        layout.addView(profile);

        Button logout =
                makeButton("ВЫЙТИ ИЗ АККАУНТА");

        logout.setOnClickListener(v -> {

            prefs.edit()
                    .remove("license_key")
                    .remove("admin_key")
                    .apply();

            showKeyScreen();
        });

        layout.addView(logout);
    }

    private void showLaunchPanel() {

        prepareScreen("launch");
        addHeader();

        layout.addView(
                makeTitle("ПАНЕЛЬ ПРИЛОЖЕНИЯ", 23));

        layout.addView(
                makeSwitch("ЗВУК", "sound"));

        layout.addView(
                makeSwitch("ВИБРАЦИЯ", "vibration"));

        layout.addView(
                makeSwitch("УВЕДОМЛЕНИЯ", "notifications"));

        Button back = makeButton("НАЗАД");
        back.setOnClickListener(v -> showMenu());

        layout.addView(back);
    }

    private void showSettings() {

        prepareScreen("settings");
        addHeader();

        layout.addView(
                makeTitle("НАСТРОЙКИ", 25));

        layout.addView(
                makeSwitch("ЗВУК", "sound"));

        layout.addView(
                makeSwitch("ВИБРАЦИЯ", "vibration"));

        layout.addView(
                makeSwitch("УВЕДОМЛЕНИЯ", "notifications"));

        Button back = makeButton("НАЗАД");
        back.setOnClickListener(v -> showMenu());

        layout.addView(back);
    }

    private void showProfile() {

        prepareScreen("profile");
        addHeader();

        layout.addView(
                makeTitle("ПРОФИЛЬ", 26));

        layout.addView(
                makeTitle("🍉", 55));

        layout.addView(
                makeText("ARBUZ CHEAT V3", 20));

        layout.addView(
                makeText("ANDROID EDITION", 14));

        Button back = makeButton("НАЗАД");
        back.setOnClickListener(v -> showMenu());

        layout.addView(back);
    }

    private Switch makeSwitch(String title, String key) {

        Switch toggle = new Switch(this);

        toggle.setText(title);
        toggle.setTextColor(Color.WHITE);
        toggle.setTextSize(17);

        toggle.setChecked(
                prefs.getBoolean(key, true));

        toggle.setThumbTintList(new ColorStateList(
                new int[][] {
                        new int[] {
                                android.R.attr.state_checked
                        },
                        new int[] {}
                },
                new int[] {
                        RED, Color.LTGRAY
                }));

        toggle.setOnCheckedChangeListener(
                (button, checked) ->
                        prefs.edit()
                                .putBoolean(key, checked)
                                .apply());

        LinearLayout.LayoutParams params =
                matchHeight(65);

        params.setMargins(0, dp(8), 0, dp(8));

        toggle.setLayoutParams(params);
        toggle.setBackground(shape(PANEL, RED));

        return toggle;
    }

    private EditText makeEditText(String hint) {

        EditText input = new EditText(this);

        input.setSingleLine(true);
        input.setHint(hint);
        input.setHintTextColor(Color.GRAY);
        input.setTextColor(Color.WHITE);

        input.setPadding(
                dp(15), dp(12), dp(15), dp(12));

        input.setBackground(shape(PANEL, RED));

        return input;
    }

    private Button makeButton(String text) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextColor(RED);
        button.setTextSize(15);
        button.setTypeface(
                Typeface.DEFAULT, Typeface.BOLD);

        button.setAllCaps(false);
        button.setBackground(shape(PANEL, RED));

        LinearLayout.LayoutParams params =
                matchHeight(60);

        params.setMargins(0, dp(8), 0, dp(8));

        button.setLayoutParams(params);

        return button;
    }

    private TextView makeTitle(String text, int size) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextColor(RED);
        view.setTextSize(size);

        view.setTypeface(
                Typeface.DEFAULT, Typeface.BOLD);

        view.setGravity(Gravity.CENTER);

        view.setPadding(
                0, dp(12), 0, dp(12));

        return view;
    }

    private TextView makeText(String text, int size) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(size);
        view.setGravity(Gravity.CENTER);

        view.setPadding(
                dp(10), dp(10), dp(10), dp(10));

        return view;
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

    private LinearLayout.LayoutParams matchHeight(
            int height) {

        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(height));
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
                dp(24), dp(20), dp(24), dp(25));

        layout.setBackgroundColor(BLACK);

        scroll.addView(layout);
        setContentView(scroll);
    }

    private void addHeader() {

        layout.addView(makeTitle("🍉", 52));
        layout.addView(makeTitle("ARBUZ CHEAT", 29));
        layout.addView(makeTitle("V3", 24));

        layout.addView(
                makeText("ANDROID EDITION", 12));
    }

    private int dp(float value) {

        return (int) (value *
                getResources()
                        .getDisplayMetrics().density
                + 0.5f);
    }

    private void toast(String message) {

        Toast.makeText(
                this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {

        if ("menu".equals(currentScreen) ||
                "activation".equals(currentScreen)) {
            super.onBackPressed();
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
