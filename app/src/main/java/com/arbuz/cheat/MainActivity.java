
package com.arbuz.cheat;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.SharedPreferences;
import android.content.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.*;
import android.text.InputType;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String SUPABASE_URL =
            "https://itktpasdhilihrqckioz.supabase.co";

    private static final String SUPABASE_KEY =
            "sb_publishable_RMX3aGldGlEyzrL5l4XCCA_L6OWoNzC";

    private final int BLACK = Color.rgb(8, 8, 8);
    private final int PANEL = Color.rgb(20, 20, 20);
    private final int RED = Color.rgb(255, 20, 35);
    private final int WHITE = Color.WHITE;

    private LinearLayout layout;
    private SharedPreferences prefs;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private String currentScreen = "auth";
    private String accessToken = "";
    private String username = "";

    private EditText loginInput;
    private EditText passwordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(BLACK);
        getWindow().setNavigationBarColor(BLACK);

        prefs = getSharedPreferences("arbuz_settings", MODE_PRIVATE);

        accessToken = prefs.getString("access_token", "");
        username = prefs.getString("username", "");

        if (!accessToken.isEmpty()) {
            showMenu();
        } else {
            showAuth();
        }
    }

    private int dp(float value) {
        return (int) (value *
                getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(18));
        d.setStroke(dp(2), stroke);
        return d;
    }

    private TextView makeTitle(String text, int size) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(RED);
        t.setTextSize(size);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(12), 0, dp(12));
        return t;
    }

    private TextView makeText(String text, int size) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(WHITE);
        t.setTextSize(size);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(10), dp(10), dp(10), dp(10));
        return t;
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(RED);
        b.setTextSize(16);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setBackground(shape(PANEL, RED));

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(62));
        p.setMargins(0, dp(9), 0, dp(9));
        b.setLayoutParams(p);
        return b;
    }

    private EditText makeInput(String hint, boolean password) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setTextColor(WHITE);
        e.setHintTextColor(Color.GRAY);
        e.setHint(hint);
        e.setTextSize(16);
        e.setPadding(dp(16), dp(10), dp(16), dp(10));
        e.setBackground(shape(PANEL, RED));

        if (password) {
            e.setInputType(
                    InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            e.setInputType(
                    InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        }

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(58));
        p.setMargins(0, dp(8), 0, dp(8));
        e.setLayoutParams(p);
        return e;
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

        AlphaAnimation a = new AlphaAnimation(0f, 1f);
        a.setDuration(250);
        layout.startAnimation(a);
    }

    private void addSpace(int height) {
        View v = new View(this);
        layout.addView(v,
                new LinearLayout.LayoutParams(1, dp(height)));
    }

    private void addHeader() {
        addSpace(25);
        layout.addView(makeTitle("🍉", 55));
        layout.addView(makeTitle("ARBUZ CHEAT", 31));
        layout.addView(makeTitle("V4 MAX", 25));
        layout.addView(makeText("ANDROID EDITION", 12));
        addSpace(20);
    }

    private void showAuth() {
        prepareScreen("auth");
        addHeader();

        layout.addView(makeTitle("ВХОД В АККАУНТ", 22));

        loginInput = makeInput("Логин", false);
        passwordInput = makeInput("Пароль", true);

        layout.addView(loginInput);
        layout.addView(passwordInput);

        Button login = makeButton("ВОЙТИ");
        login.setOnClickListener(v -> authenticate(false));
        layout.addView(login);

        Button register = makeButton("СОЗДАТЬ АККАУНТ");
        register.setOnClickListener(v -> authenticate(true));
        layout.addView(register);

        layout.addView(makeText(
                "Используй логин и пароль. Email не требуется.",
                12));
    }

    private void authenticate(boolean registration) {
        String user = loginInput.getText().toString()
                .trim().toLowerCase();
        String pass = passwordInput.getText().toString();

        if (!user.matches("[a-z0-9_]{3,24}")) {
            toast("Логин: 3–24 символа, латинские буквы, цифры или _");
            return;
        }

        if (pass.length() < 6) {
            toast("Пароль должен содержать минимум 6 символов");
            return;
        }

        String email = user + "@users.arbuz.invalid";

        setBusy(true);

        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", pass);

                String endpoint;

                if (registration) {
                    endpoint = "/auth/v1/signup";
                    JSONObject data = new JSONObject();
                    data.put("username", user);
                    body.put("data", data);
                } else {
                    endpoint = "/auth/v1/token?grant_type=password";
                }

                JSONObject result = request(
                        endpoint, "POST", body, null);

                String token = result.optString("access_token", "");

                if (token.isEmpty()) {
                    if (registration &&
                            result.has("id")) {
                        runOnUiThread(() -> {
                            setBusy(false);
                            toast("Аккаунт создан. Теперь попробуй войти.");
                        });
                    } else {
                        String message = result.optString(
                                "msg",
                                result.optString(
                                        "message",
                                        "Не удалось выполнить вход"));
                        runOnUiThread(() -> {
                            setBusy(false);
                            toast(message);
                        });
                    }
                    return;
                }

                prefs.edit()
                        .putString("access_token", token)
                        .putString("username", user)
                        .apply();

                accessToken = token;
                username = user;

                runOnUiThread(() -> {
                    setBusy(false);
                    showMenu();
                    toast("Добро пожаловать!");
                });

            } catch (Exception ex) {
                runOnUiThread(() -> {
                    setBusy(false);
                    toast("Ошибка соединения: " + ex.getMessage());
                });
            }
        });
    }

    private JSONObject request(
            String path,
            String method,
            JSONObject body,
            String token) throws Exception {

        URL url = new URL(SUPABASE_URL + path);
        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(method);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty(
                "apikey", SUPABASE_KEY);
        connection.setRequestProperty(
                "Content-Type", "application/json");

        if (token != null && !token.isEmpty()) {
            connection.setRequestProperty(
                    "Authorization", "Bearer " + token);
        }

        if (body != null) {
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(body.toString().getBytes(
                        StandardCharsets.UTF_8));
            }
        }

        int code = connection.getResponseCode();

        InputStream stream = code >= 200 && code < 300
                ? connection.getInputStream()
                : connection.getErrorStream();

        StringBuilder response = new StringBuilder();

        if (stream != null) {
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
        }

        connection.disconnect();

        JSONObject json = new JSONObject(response.toString());

        if (code < 200 || code >= 300) {
            throw new Exception(json.optString(
                    "msg",
                    json.optString("message", "HTTP " + code)));
        }

        return json;
    }

    private void setBusy(boolean busy) {
        runOnUiThread(() -> {
            if (loginInput != null) loginInput.setEnabled(!busy);
            if (passwordInput != null) passwordInput.setEnabled(!busy);
        });
    }

    private void toast(String message) {
        Toast.makeText(this, message,
                Toast.LENGTH_LONG).show();
    }

    private void showMenu() {
        prepareScreen("menu");
        addHeader();

        layout.addView(makeTitle("ГЛАВНОЕ МЕНЮ", 23));

        Button profile = makeButton("👤  ПРОФИЛЬ");
        profile.setOnClickListener(v -> showProfile());

        Button settings = makeButton("⚙  НАСТРОЙКИ");
        settings.setOnClickListener(v -> showSettings());

        Button about = makeButton("ⓘ  ИНФОРМАЦИЯ");
        about.setOnClickListener(v -> showAbout());

        Button logout = makeButton("ВЫЙТИ ИЗ АККАУНТА");
        logout.setOnClickListener(v -> logout());

        layout.addView(profile);
        layout.addView(settings);
        layout.addView(about);
        layout.addView(logout);
    }

    private void showProfile() {
        prepareScreen("profile");
        addSpace(35);

        layout.addView(makeTitle("ПРОФИЛЬ", 28));
        layout.addView(makeTitle("🍉", 60));
        layout.addView(makeText("ARBUZ CHEAT V4 MAX", 21));
        layout.addView(makeText("Логин: " + username, 17));
        layout.addView(makeText("Статус: авторизован", 15));

        Button back = makeButton("← НАЗАД");
        back.setOnClickListener(v -> showMenu());
        layout.addView(back);
    }

    private void showSettings() {
        prepareScreen("settings");
        addSpace(30);

        layout.addView(makeTitle("НАСТРОЙКИ", 28));

        Switch sound = makeSwitch("ЗВУК", "sound");
        Switch vibration = makeSwitch("ВИБРАЦИЯ", "vibration");

        layout.addView(sound);
        layout.addView(vibration);

        Button back = makeButton("← НАЗАД");
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
                        new int[] {android.R.attr.state_checked},
                        new int[] {}
                },
                new int[] {RED, Color.LTGRAY}));

        toggle.setTrackTintList(new ColorStateList(
                new int[][] {
                        new int[] {android.R.attr.state_checked},
                        new int[] {}
                },
                new int[] {
                        Color.rgb(130, 20, 30),
                        Color.DKGRAY
                }));

        toggle.setOnCheckedChangeListener(
                (button, checked) ->
                        prefs.edit().putBoolean(key, checked).apply());

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(75));
        p.setMargins(0, dp(8), 0, dp(8));

        toggle.setLayoutParams(p);
        toggle.setBackground(shape(PANEL, Color.DKGRAY));
        return toggle;
    }

    private void showAbout() {
        prepareScreen("about");
        addSpace(35);

        layout.addView(makeTitle("ИНФОРМАЦИЯ", 27));
        layout.addView(makeTitle("🍉", 55));
        layout.addView(makeText("ARBUZ CHEAT V4 MAX", 22));
        layout.addView(makeText("Платформа: Android", 16));
        layout.addView(makeText("Разработчик: ARBUZ", 16));

        Button back = makeButton("← НАЗАД");
        back.setOnClickListener(v -> showMenu());
        layout.addView(back);
    }

    private void logout() {
        accessToken = "";
        username = "";

        prefs.edit()
                .remove("access_token")
                .remove("username")
                .apply();

        showAuth();
    }

    @Override
    public void onBackPressed() {
        if (currentScreen.equals("auth")) {
            super.onBackPressed();
        } else if (currentScreen.equals("menu")) {
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
