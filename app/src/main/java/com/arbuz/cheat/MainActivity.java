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
import android.text.Editable;
import android.text.TextWatcher;
import org.json.JSONObject;
import org.json.JSONArray;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    private String currentScreen = "activation";

    private static final String SUPABASE_URL =
            "https://itktpasdhilihrqckioz.supabase.co";

    private static final String SUPABASE_KEY =
            "sb_publishable_RMX3aGldGlEyzrL5l4XCCA_L6OWoNzC";

    private JSONArray licenseData = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(BLACK);
        getWindow().setNavigationBarColor(BLACK);

        prefs = getSharedPreferences(
                "arbuz_settings", MODE_PRIVATE);

        String savedKey =
                prefs.getString("license_key", "");

        if (!savedKey.isEmpty()) {
            checkSavedLicense(savedKey);
        } else {
            showKeyScreen();
        }
    }

    private String getInstallationId() {
        return Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    private JSONObject postRequest(
            String endpoint, JSONObject data) throws Exception {

        URL url = new URL(
                SUPABASE_URL + "/functions/v1/" + endpoint);

        HttpURLConnection conn =
                (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setDoOutput(true);

        conn.setRequestProperty(
                "Content-Type", "application/json");

        conn.setRequestProperty(
                "apikey", SUPABASE_KEY);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(data.toString().getBytes("UTF-8"));
        }

        int status = conn.getResponseCode();

        java.io.InputStream stream =
                status >= 200 && status < 300
                        ? conn.getInputStream()
                        : conn.getErrorStream();

        String response;

        try (Scanner scanner =
                     new Scanner(stream).useDelimiter("\\A")) {
            response = scanner.hasNext()
                    ? scanner.next() : "";
        }

        conn.disconnect();

        if (status < 200 || status >= 300) {
            throw new Exception("HTTP " + status);
        }

        return new JSONObject(response);
    }

    private JSONObject makeAuthRequest(String key)
            throws Exception {

        JSONObject data = new JSONObject();
        data.put("key", key);
        data.put("installation_id", getInstallationId());
        return data;
    }

    private void checkSavedLicense(String key) {
        executor.execute(() -> {
            boolean valid = false;

            try {
                JSONObject result = postRequest(
                        "check-key", makeAuthRequest(key));

                valid = result.optBoolean("ok", false);
            } catch (Exception ignored) {}

            final boolean success = valid;

            runOnUiThread(() -> {
                if (success) {
                    showMenu();
                } else {
                    prefs.edit()
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
        layout.addView(makeText(
                "Введи лицензионный ключ", 16));

        EditText keyInput = makeInput("Лицензионный ключ");
        layout.addView(keyInput, inputParams());

        Button activate = makeButton("АКТИВИРОВАТЬ");
        layout.addView(activate);

        activate.setOnClickListener(v -> {
            String key = keyInput.getText().toString().trim();

            if (key.isEmpty()) {
                toast("Введи ключ");
                return;
            }

            activate.setEnabled(false);
            activate.setText("ПРОВЕРКА...");

            executor.execute(() -> {
                boolean valid = false;

                try {
                    JSONObject result = postRequest(
                            "verify-key", makeAuthRequest(key));

                    valid = result.optBoolean("ok", false);
                } catch (Exception ignored) {}

                final boolean success = valid;

                runOnUiThread(() -> {
                    activate.setEnabled(true);
                    activate.setText("АКТИВИРОВАТЬ");

                    if (success) {
                        prefs.edit()
                                .putString("license_key", key)
                                .apply();
                        showMenu();
                    } else {
                        toast("Ключ недействителен или ошибка соединения");
                    }
                });
            });
        });
    }

    private void showMenu() {
        prepareScreen("menu");
        addHeader();

        layout.addView(makeTitle("ГЛАВНОЕ МЕНЮ", 23));

        Button launch = makeButton("▶  ПАНЕЛЬ ПРИЛОЖЕНИЯ");
        launch.setOnClickListener(v -> showLaunchPanel());

        Button admin = makeButton("♛  АДМИН-ПАНЕЛЬ");
        admin.setOnClickListener(v -> showAdminLogin());

        Button settings = makeButton("⚙  НАСТРОЙКИ");
        settings.setOnClickListener(v -> showSettings());

        Button profile = makeButton("👤  ПРОФИЛЬ");
        profile.setOnClickListener(v -> showProfile());

        Button about = makeButton("ⓘ  ИНФОРМАЦИЯ");
        about.setOnClickListener(v -> showAbout());

        layout.addView(launch);
        layout.addView(admin);
        layout.addView(settings);
        layout.addView(profile);
        layout.addView(about);
    }

    private void showAdminLogin() {
        prepareScreen("admin_login");
        addHeader();

        layout.addView(makeTitle("АДМИН-ДОСТУП", 25));
        layout.addView(makeText(
                "Введи специальный админ-ключ", 15));

        EditText adminInput = makeInput("Админ-ключ");
        layout.addView(adminInput, inputParams());

        String savedAdmin =
                prefs.getString("admin_key", "");

        if (!savedAdmin.isEmpty()) {
            adminInput.setText(savedAdmin);
        }

        Button enter = makeButton("ПРОВЕРИТЬ ДОСТУП");
        layout.addView(enter);

        enter.setOnClickListener(v -> {
            String key = adminInput.getText().toString().trim();

            if (key.isEmpty()) {
                toast("Введи админ-ключ");
                return;
            }

            enter.setEnabled(false);
            enter.setText("ПРОВЕРКА...");

            executor.execute(() -> {
                boolean valid = false;

                try {
                    JSONObject result = postRequest(
                            "verify-admin", makeAuthRequest(key));

                    valid = result.optBoolean("ok", false)
                            && result.optBoolean("admin", false);
                } catch (Exception ignored) {}

                final boolean success = valid;

                runOnUiThread(() -> {
                    enter.setEnabled(true);
                    enter.setText("ПРОВЕРИТЬ ДОСТУП");

                    if (success) {
                        prefs.edit()
                                .putString("admin_key", key)
                                .apply();
                        showAdminPanel();
                    } else {
                        toast("Админ-доступ не подтверждён");
                    }
                });
            });
        });

        Button back = makeButton("← НАЗАД");
        back.setOnClickListener(v -> showMenu());
        layout.addView(back);
    }

    private void showAdminPanel() {
        prepareScreen("admin");
        addHeader();

        layout.addView(makeTitle("АДМИН-ПАНЕЛЬ", 26));

        Button licenses =
                makeButton("СПИСОК ЛИЦЕНЗИЙ");

        licenses.setOnClickListener(v -> loadLicenses());
        layout.addView(licenses);

        Button logout =
                makeButton("ВЫЙТИ ИЗ АДМИН-ПАНЕЛИ");

        logout.setOnClickListener(v -> {
            prefs.edit()
                    .remove("admin_key")
                    .apply();

            showMenu();
        });

        layout.addView(logout);

        Button back = makeButton("← НАЗАД");
        back.setOnClickListener(v -> showMenu());
        layout.addView(back);
    }

    private void loadLicenses() {
        prepareScreen("licenses");
        addHeader();

        layout.addView(makeTitle("ЛИЦЕНЗИИ", 25));

        TextView status =
                makeText("Загрузка лицензий...", 15);

        layout.addView(status);

        Button refresh =
                makeButton("ОБНОВИТЬ СПИСОК");

        refresh.setOnClickListener(v -> loadLicenses());
        layout.addView(refresh);

        Button back = makeButton("← НАЗАД");
        back.setOnClickListener(v -> showAdminPanel());
        layout.addView(back);

        String key = prefs.getString("admin_key", "");

        executor.execute(() -> {
            try {
                JSONObject result = postRequest(
                        "admin-list-licenses",
                        makeAuthRequest(key));

                if (!result.optBoolean("ok", false)) {
                    throw new Exception("Доступ запрещён");
                }

                JSONArray received =
                        result.optJSONArray("licenses");

                if (received == null) {
                    received = new JSONArray();
                }

                final JSONArray data = received;

                runOnUiThread(() -> {
                    licenseData = data;
                    showLicenseFilters(data);
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        status.setText(
                                "Ошибка загрузки: " + e.getMessage()));
            }
        });
    }

    private void showLicenseFilters(JSONArray data) {
        prepareScreen("licenses");
        addHeader();

        layout.addView(makeTitle("СПИСОК ЛИЦЕНЗИЙ", 24));

        layout.addView(makeText(
                "Всего записей: " + data.length(), 15));

        EditText search = makeInput("Поиск по ID лицензии");
        layout.addView(search, inputParams());

        Spinner planFilter = makeSpinner(new String[]{
                "Все тарифы", "1d", "7d", "30d", "lifetime"
        });

        Spinner statusFilter = makeSpinner(new String[]{
                "Все статусы", "Активные", "Отключённые"
        });

        Spinner sortFilter = makeSpinner(new String[]{
                "Сначала новые", "Сначала старые"
        });

        layout.addView(planFilter);
        layout.addView(statusFilter);
        layout.addView(sortFilter);

        LinearLayout results =
                new LinearLayout(this);

        results.setOrientation(LinearLayout.VERTICAL);
        layout.addView(results);

        Runnable applyFilters = () -> {
            results.removeAllViews();

            ArrayList<JSONObject> filtered =
                    new ArrayList<>();

            String query = search.getText()
                    .toString().trim().toLowerCase();

            String plan = planFilter.getSelectedItem()
                    .toString();

            String state = statusFilter.getSelectedItem()
                    .toString();

            for (int i = 0; i < data.length(); i++) {
                JSONObject item = data.optJSONObject(i);
                if (item == null) continue;

                String id = item.optString("id", "");
                String itemPlan = item.optString("plan", "");
                boolean disabled =
                        item.optBoolean("disabled", false);

                if (!query.isEmpty()
                        && !id.toLowerCase().contains(query)) {
                    continue;
                }

                if (!plan.equals("Все тарифы")
                        && !itemPlan.equalsIgnoreCase(plan)) {
                    continue;
                }

                if (state.equals("Активные") && disabled) {
                    continue;
                }

                if (state.equals("Отключённые") && !disabled) {
                    continue;
                }

                filtered.add(item);
            }

            Collections.sort(filtered,
                    (a, b) -> {
                        String first =
                                a.optString("created_at", "");

                        String second =
                                b.optString("created_at", "");

                        int compare = first.compareTo(second);

                        return sortFilter.getSelectedItemPosition() == 0
                                ? -compare : compare;
                    });

            results.addView(makeText(
                    "Найдено: " + filtered.size(), 15));

            for (JSONObject item : filtered) {
                addLicenseCard(results, item);
            }
        };

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {}

            public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                applyFilters.run();
            }

            public void afterTextChanged(Editable e) {}
        });

        planFilter.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view, int position, long id) {
                        applyFilters.run();
                    }

                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {}
                });

        statusFilter.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view, int position, long id) {
                        applyFilters.run();
                    }

                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {}
                });

        sortFilter.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view, int position, long id) {
                        applyFilters.run();
                    }

                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {}
                });

        Button reset = makeButton("СБРОСИТЬ ФИЛЬТРЫ");
        reset.setOnClickListener(v -> {
            search.setText("");
            planFilter.setSelection(0);
            statusFilter.setSelection(0);
            sortFilter.setSelection(0);
            applyFilters.run();
        });

        layout.addView(reset);

        Button back = makeButton("← НАЗАД");
        back.setOnClickListener(v -> showAdminPanel());
        layout.addView(back);

        applyFilters.run();
    }

    private void addLicenseCard(
            LinearLayout parent, JSONObject item) {

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

        String id = item.optString("id", "—");
        String plan = item.optString("plan", "—");
        boolean disabled = item.optBoolean("disabled", false);

        String state = disabled
                ? "ОТКЛЮЧЕНА" : "АКТИВНА";

        String created = item.optString("created_at", "—");
        String activated = item.optString("activated_at", "—");
        String expires = item.isNull("expires_at")
                ? "Навсегда"
                : item.optString("expires_at", "—");

        String device = item.optString(
                "installation_id", "Не привязано");

        card.addView(makeText("ID: " + id, 14));
        card.addView(makeText("Тариф: " + plan, 14));
        card.addView(makeText("Статус: " + state, 14));
        card.addView(makeText("Создана: " + created, 12));
        card.addView(makeText("Активирована: " + activated, 12));
        card.addView(makeText("Окончание: " + expires, 12));
        card.addView(makeText("Устройство: " + device, 11));

        parent.addView(card);
    }

    private void showProfile() {
        prepareScreen("profile");
        addHeader();

        layout.addView(makeTitle("МОЙ ПРОФИЛЬ", 26));

        TextView info =
                makeText("Загрузка информации...", 15);

        layout.addView(info);

        Button refresh = makeButton("ОБНОВИТЬ ПРОФИЛЬ");
        layout.addView(refresh);

        refresh.setOnClickListener(v -> showProfile());

        Button back = makeButton("← НАЗАД");
        back.setOnClickListener(v -> showMenu());
        layout.addView(back);

        String key = prefs.getString("license_key", "");

        executor.execute(() -> {
            try {
                JSONObject result = postRequest(
                        "check-key", makeAuthRequest(key));

                if (!result.optBoolean("ok", false)) {
                    throw new Exception("Лицензия недействительна");
                }

                String plan = result.optString("plan", "—");
                String expires = result.isNull("expires_at")
                        ? "Навсегда"
                        : result.optString("expires_at", "—");

                runOnUiThread(() -> {
                    info.setText(
                            "СТАТУС: АКТИВНА\n\n" +
                            "ТАРИФ: " + plan + "\n\n" +
                            "ОКОНЧАНИЕ: " + expires + "\n\n" +
                            "ID УСТРОЙСТВА:\n" +
                            getInstallationId());
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        info.setText(
                                "Не удалось загрузить профиль:\n" +
                                e.getMessage()));
            }
        });
    }

    private void showLaunchPanel() {
        prepareScreen("launch");
        addHeader();

        layout.addView(makeTitle("ПАНЕЛЬ ПРИЛОЖЕНИЯ", 23));

        layout.addView(makeText(
                "Настройки приложения", 14));

        layout.addView(makeSwitch("ЗВУК", "sound"));
        layout.addView(makeSwitch("ВИБРАЦИЯ", "vibration"));
        layout.addView(makeSwitch(
                "УВЕДОМЛЕНИЯ", "notifications"));

        Button back = makeButton("← НАЗАД");
        back.setOnClickListener(v -> showMenu());
        layout.addView(back);
    }

    private void showSettings() {
        prepareScreen("settings");
        addHeader();

        layout.addView(makeTitle("НАСТРОЙКИ", 28));

        layout.addView(makeSwitch("ЗВУК", "sound"));
        layout.addView(makeSwitch("ВИБРАЦИЯ", "vibration"));
        layout.addView(makeSwitch(
                "УВЕДОМЛЕНИЯ", "notifications"));

        Button back = makeButton("← НАЗАД");
        back.setOnClickListener(v -> showMenu());
        layout.addView(back);
    }

    private Switch makeSwitch(String title, String key) {
        Switch toggle = new Switch(this);

        toggle.setText(title);
        toggle.setTextColor(WHITE);
        toggle.setTextSize(17);
        toggle.setPadding(
                dp(12), dp(15), dp(12), dp(15));

        toggle.setChecked(prefs.getBoolean(key, true));

        toggle.setThumbTintList(new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{RED, Color.LTGRAY}
        ));

        toggle.setTrackTintList(new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
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

    private void showAbout() {
        prepareScreen("about");
        addHeader();

        layout.addView(makeTitle("ИНФОРМАЦИЯ", 27));
        layout.addView(makeText("🍉 ARBUZ CHEAT", 22));
        layout.addView(makeText("Платформа: Android", 16));
        layout.addView(makeText("Версия: 4.0", 16));
        layout.addView(makeText("Разработчик: ARBUZ", 16));
        layout.addView(makeText("КАНАЛ: @TG_beta_oxide",16));

    
    }

    private EditText makeInput(String hint) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setHintTextColor(Color.GRAY);
        input.setTextColor(WHITE);
        input.setPadding(
                dp(15), dp(12), dp(15), dp(12));
        input.setBackground(shape(PANEL, RED));
        return input;
    }

    private LinearLayout.LayoutParams inputParams() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(60));
    }

    private Spinner makeSpinner(String[] values) {
        Spinner spinner = new Spinner(this);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        values);

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(55));

        params.setMargins(0, dp(5), 0, dp(5));
        spinner.setLayoutParams(params);

        return spinner;
    }

    private void prepareScreen(String screen) {
        currentScreen = screen;

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BLACK);

        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(
                dp(24), dp(25), dp(24), dp(25));
        layout.setBackgroundColor(BLACK);

        scroll.addView(layout);
        setContentView(scroll);
    }

    private void addHeader() {
        addSpace(20);
        layout.addView(makeTitle("🍉", 55));
        layout.addView(makeTitle("ARBUZ CHEAT", 31));
        layout.addView(makeTitle("V4", 27));
        layout.addView(makeText("ANDROID EDITION", 12));
        addSpace(25);
    }

    private void addSpace(int height) {
        View space = new View(this);

        layout.addView(space,
                new LinearLayout.LayoutParams(
                        1, dp(height)));
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
        view.setPadding(
                dp(10), dp(10), dp(10), dp(10));
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

    private void toast(String message) {
        Toast.makeText(
                this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        if (currentScreen.equals("activation")
                || currentScreen.equals("menu")) {
            super.onBackPressed();
        } else if (currentScreen.equals("licenses")) {
            showAdminPanel();
        } else if (currentScreen.equals("admin")) {
            showMenu();
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
