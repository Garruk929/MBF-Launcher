package com.mbf.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Independently implemented minimalist Android home launcher, tuned for Pixel 9a. */
public final class MainActivity extends Activity {
    private static WeakReference<MainActivity> visible = new WeakReference<>(null);
    private static final String PREFS = "mbf_settings";
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final ArrayList<AppEntry> allApps = new ArrayList<>();
    private final HashMap<Character, View> letterRows = new HashMap<>();
    private SharedPreferences prefs;
    private FrameLayout root;
    private LinearLayout page;
    private LinearLayout cards;
    private LinearLayout appRows;
    private LinearLayout favoritesRows;
    private ScrollView appScroll;
    private AlphabetRail rail;
    private TextView clock;
    private TextView date;
    private EditText search;
    private boolean dark;
    private boolean modern;
    private int fg, muted, accent, panel;
    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (clock != null) clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            if (date != null) date.setText(new SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(new Date()));
            clockHandler.postDelayed(this, 30_000);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        dark = prefs.getBoolean("dark", true);
        modern = prefs.getBoolean("modern", false);
        loadApplications();
        buildHome();
    }

    @Override protected void onResume() {
        super.onResume();
        visible = new WeakReference<>(this);
        loadApplications();
        drawAppList();
        refreshCards();
        clockHandler.removeCallbacks(clockTick);
        clockTick.run();
    }

    @Override protected void onPause() {
        visible.clear();
        clockHandler.removeCallbacks(clockTick);
        super.onPause();
    }

    @Override protected void onDestroy() {
        clockHandler.removeCallbacks(clockTick);
        super.onDestroy();
    }

    static void notifyCardsChanged() {
        MainActivity activity = visible.get();
        if (activity != null) activity.runOnUiThread(activity::refreshCards);
    }

    private int dp(float n) { return (int) (n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable rounded(int color, float radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(radius));
        return bg;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private TextView label(String value, float size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(color);
        v.setTextSize(size);
        if (bold) v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private void buildHome() {
        dark = prefs.getBoolean("dark", true);
        modern = prefs.getBoolean("modern", false);
        fg = dark ? 0xFFF5F2FF : 0xFF171624;
        muted = dark ? 0xFFAFACBF : 0xFF6F697B;
        accent = dark ? 0xFFBDA7FF : 0xFF6046A2;
        panel = dark ? 0xD5262535 : 0xE7FFFFFF;
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        root = new FrameLayout(this);
        GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                dark ? new int[]{0xFF1B1B2C, 0xFF0E1017, 0xFF252033} : new int[]{0xFFEEE7FF, 0xFFF8F8FC, 0xFFE9E1F8});
        root.setBackground(gradient);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        setContentView(root);
        root.requestApplyInsets();

        page = vertical();
        page.setPadding(dp(23), dp(modern ? 29 : 18), dp(46), dp(10));
        root.addView(page, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(top, new LinearLayout.LayoutParams(-1, dp(42)));
        TextView brand = label("MBF  /  HOME", 11, muted, true);
        brand.setLetterSpacing(.22f);
        top.addView(brand, new LinearLayout.LayoutParams(0, -1, 1));
        TextView settings = label("⚙", 27, accent, false);
        settings.setGravity(Gravity.CENTER);
        settings.setContentDescription("Impostazioni del launcher");
        top.addView(settings, new LinearLayout.LayoutParams(dp(48), dp(44)));
        settings.setOnClickListener(v -> showSettings());

        clock = label("", modern ? 66 : 50, fg, true);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(modern ? 92 : 70));
        cp.topMargin = dp(modern ? 24 : 8);
        page.addView(clock, cp);
        date = label("", 14, muted, false);
        page.addView(date, new LinearLayout.LayoutParams(-1, dp(28)));
        clockHandler.removeCallbacks(clockTick);
        clockTick.run();

        cards = vertical();
        LinearLayout.LayoutParams cardsParams = new LinearLayout.LayoutParams(-1, -2);
        cardsParams.topMargin = dp(modern ? 23 : 14);
        page.addView(cards, cardsParams);

        TextView fTitle = label("PREFERITI", 11, muted, true);
        fTitle.setLetterSpacing(.15f);
        LinearLayout.LayoutParams ft = new LinearLayout.LayoutParams(-1, dp(28));
        ft.topMargin = dp(modern ? 20 : 10);
        page.addView(fTitle, ft);
        favoritesRows = vertical();
        page.addView(favoritesRows, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout searchFrame = new LinearLayout(this);
        searchFrame.setGravity(Gravity.CENTER_VERTICAL);
        searchFrame.setBackground(rounded(panel, 15));
        LinearLayout.LayoutParams sfp = new LinearLayout.LayoutParams(-1, dp(47));
        sfp.topMargin = dp(15);
        sfp.bottomMargin = dp(9);
        page.addView(searchFrame, sfp);
        TextView mag = label("⌕", 29, accent, false);
        mag.setGravity(Gravity.CENTER);
        searchFrame.addView(mag, new LinearLayout.LayoutParams(dp(43), -1));
        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Cerca app…");
        search.setTextColor(fg);
        search.setHintTextColor(muted);
        search.setTextSize(15);
        search.setBackgroundColor(Color.TRANSPARENT);
        search.setPadding(0, 0, dp(8), 0);
        searchFrame.addView(search, new LinearLayout.LayoutParams(0, -1, 1));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { drawAppList(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        search.clearFocus();
        root.setFocusableInTouchMode(true);
        root.requestFocus();

        appScroll = new ScrollView(this);
        appScroll.setFillViewport(false);
        appScroll.setVerticalScrollBarEnabled(false);
        appRows = vertical();
        appScroll.addView(appRows);
        page.addView(appScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        rail = new AlphabetRail(this, muted, accent, this::scrollToLetter);
        FrameLayout.LayoutParams railParams = new FrameLayout.LayoutParams(dp(34), -1, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        railParams.topMargin = dp(132);
        railParams.bottomMargin = dp(24);
        rail.setPadding(dp(2), dp(7), dp(3), dp(7));
        root.addView(rail, railParams);
        drawAppList();
        refreshCards();
    }

    private void loadApplications() {
        PackageManager pm = getPackageManager();
        Intent launcher = new Intent(Intent.ACTION_MAIN);
        launcher.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> result = pm.queryIntentActivities(launcher, 0);
        Map<String, AppEntry> unique = new HashMap<>();
        for (ResolveInfo info : result) {
            if (info.activityInfo == null) continue;
            ComponentName component = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
            if (component.getPackageName().equals(getPackageName())) continue;
            try {
                String name = String.valueOf(info.loadLabel(pm));
                Drawable icon = info.loadIcon(pm);
                unique.put(component.flattenToString(), new AppEntry(name, component, icon));
            } catch (Exception ignored) { /* one broken third-party app must not break Home */ }
        }
        allApps.clear();
        allApps.addAll(unique.values());
        Collections.sort(allApps, Comparator.comparing(a -> a.name.toLowerCase(Locale.getDefault())));
    }

    private Set<String> favoriteIds() {
        return new HashSet<>(prefs.getStringSet("favorites", Collections.emptySet()));
    }

    private void toggleFavorite(AppEntry entry) {
        Set<String> updated = favoriteIds();
        String key = entry.component.flattenToString();
        boolean added = updated.add(key);
        if (!added) updated.remove(key);
        prefs.edit().putStringSet("favorites", updated).apply();
        Toast.makeText(this, added ? "Aggiunta ai preferiti" : "Rimossa dai preferiti", Toast.LENGTH_SHORT).show();
        drawAppList();
    }

    private void drawAppList() {
        if (appRows == null || favoritesRows == null) return;
        appRows.removeAllViews();
        favoritesRows.removeAllViews();
        letterRows.clear();
        Set<String> fav = favoriteIds();
        int shownFavorites = 0;
        for (AppEntry app : allApps) {
            if (fav.contains(app.component.flattenToString()) && shownFavorites < 5) {
                favoritesRows.addView(appRow(app, true));
                shownFavorites++;
            }
        }
        if (shownFavorites == 0) {
            TextView hint = label("Tieni premuta un'app per aggiungerla qui", 12, muted, false);
            favoritesRows.addView(hint, new LinearLayout.LayoutParams(-1, dp(35)));
        }
        String query = search == null ? "" : search.getText().toString().trim().toLowerCase(Locale.getDefault());
        for (AppEntry app : allApps) {
            if (!query.isEmpty() && !app.name.toLowerCase(Locale.getDefault()).contains(query)) continue;
            View row = appRow(app, false);
            appRows.addView(row);
            char letter = Character.toUpperCase(app.name.charAt(0));
            if (!letterRows.containsKey(letter)) letterRows.put(letter, row);
        }
        if (appRows.getChildCount() == 0) {
            TextView empty = label("Nessuna app trovata", 14, muted, false);
            appRows.addView(empty, new LinearLayout.LayoutParams(-1, dp(55)));
        }
    }

    private View appRow(AppEntry app, boolean favorite) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int size = modern ? 54 : 47;
        row.setMinimumHeight(dp(size));
        row.setPadding(dp(favorite ? 5 : 1), dp(2), dp(3), dp(2));
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(app.icon);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(26), dp(26));
        ip.rightMargin = dp(16);
        row.addView(icon, ip);
        TextView name = label(app.name, modern ? 17 : 15, fg, favorite);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(name, new LinearLayout.LayoutParams(0, dp(size), 1));
        row.setContentDescription(app.name + (favorite ? ", preferita" : "") + ". Tieni premuto per preferiti");
        row.setOnClickListener(v -> {
            try {
                Intent launch = new Intent(Intent.ACTION_MAIN);
                launch.addCategory(Intent.CATEGORY_LAUNCHER);
                launch.setComponent(app.component);
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                startActivity(launch);
            } catch (Exception e) { Toast.makeText(this, "Impossibile aprire " + app.name, Toast.LENGTH_SHORT).show(); }
        });
        row.setOnLongClickListener(v -> { toggleFavorite(app); return true; });
        return row;
    }

    private void scrollToLetter(char letter) {
        if (appScroll == null) return;
        View target = letterRows.get(letter);
        if (target == null) {
            for (char c = (char) (letter + 1); c <= 'Z'; c++) {
                target = letterRows.get(c);
                if (target != null) break;
            }
        }
        if (target != null) {
            final View row = target;
            appScroll.post(() -> appScroll.scrollTo(0, row.getTop()));
        }
    }

    private void refreshCards() {
        if (cards == null) return;
        cards.removeAllViews();
        StatusBarNotification[] active = MBFNotificationService.snapshot();
        if (active.length == 0) return;
        ArrayList<StatusBarNotification> sorted = new ArrayList<>();
        for (StatusBarNotification item : active) {
            Notification n = item.getNotification();
            if (n == null || (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) continue;
            sorted.add(item);
        }
        sorted.sort((a,b) -> Long.compare(b.getPostTime(), a.getPostTime()));
        int count = 0;
        for (StatusBarNotification item : sorted) {
            if (count++ >= 2) break;
            cards.addView(notificationCard(item));
        }
    }

    private View notificationCard(StatusBarNotification item) {
        Notification n = item.getNotification();
        LinearLayout outer = vertical();
        outer.setBackground(rounded(panel, modern ? 18 : 13));
        outer.setPadding(dp(14), dp(9), dp(13), dp(10));
        LinearLayout.LayoutParams margin = new LinearLayout.LayoutParams(-1, -2);
        margin.bottomMargin = dp(6);
        outer.setLayoutParams(margin);
        String appName = item.getPackageName();
        try { appName = String.valueOf(getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(appName, 0))); }
        catch (PackageManager.NameNotFoundException ignored) {}
        String title = value(n.extras.getCharSequence(Notification.EXTRA_TITLE));
        String message = value(n.extras.getCharSequence(Notification.EXTRA_TEXT));
        TextView source = label(appName.toUpperCase(Locale.getDefault()), 10, accent, true);
        outer.addView(source, new LinearLayout.LayoutParams(-1, dp(20)));
        if (!title.isEmpty()) outer.addView(label(title, 14, fg, true));
        if (!message.isEmpty()) {
            TextView content = label(message, 12, muted, false);
            content.setMaxLines(2);
            content.setEllipsize(android.text.TextUtils.TruncateAt.END);
            outer.addView(content);
        }
        if (n.contentIntent != null) outer.setOnClickListener(v -> {
            try { n.contentIntent.send(); }
            catch (PendingIntent.CanceledException e) { Toast.makeText(this, "Notifica non più disponibile", Toast.LENGTH_SHORT).show(); }
        });
        Notification.Action reply = findReply(n);
        if (reply != null) {
            TextView replyButton = label("↳  Rispondi", 13, accent, true);
            replyButton.setPadding(0, dp(8), 0, dp(1));
            replyButton.setOnClickListener(v -> showReply(reply));
            outer.addView(replyButton);
        }
        return outer;
    }

    private static String value(CharSequence text) { return text == null ? "" : text.toString(); }

    private Notification.Action findReply(Notification n) {
        if (n.actions == null) return null;
        for (Notification.Action action : n.actions) {
            if (action == null || action.actionIntent == null) continue;
            RemoteInput[] inputs = action.getRemoteInputs();
            if (inputs != null && inputs.length > 0) return action;
        }
        return null;
    }

    private void showReply(Notification.Action action) {
        final EditText input = new EditText(this);
        input.setSingleLine(false);
        input.setMinLines(1);
        input.setHint("Scrivi una risposta…");
        FrameLayout holder = new FrameLayout(this);
        holder.setPadding(dp(20), dp(4), dp(20), dp(2));
        holder.addView(input);
        new AlertDialog.Builder(this).setTitle("Risposta rapida").setView(holder)
                .setNegativeButton("Annulla", null)
                .setPositiveButton("Invia", (dialog, which) -> {
                    String replyText = input.getText().toString().trim();
                    if (replyText.isEmpty()) return;
                    try {
                        RemoteInput[] inputs = action.getRemoteInputs();
                        if (inputs == null || inputs.length == 0) return;
                        Bundle results = new Bundle();
                        for (RemoteInput remote : inputs) {
                            if (remote.getAllowFreeFormInput()) results.putCharSequence(remote.getResultKey(), replyText);
                        }
                        if (results.isEmpty()) {
                            Toast.makeText(this, "Questa notifica non consente testo libero", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Intent fillIn = new Intent();
                        RemoteInput.addResultsToIntent(inputs, fillIn, results);
                        action.actionIntent.send(this, 0, fillIn);
                        Toast.makeText(this, "Risposta inviata", Toast.LENGTH_SHORT).show();
                    } catch (Exception ex) { Toast.makeText(this, "Risposta non riuscita", Toast.LENGTH_SHORT).show(); }
                }).show();
    }

    private void showSettings() {
        String[] items = {"Layout: " + (modern ? "Modern" : "Classic"),
                "Tema: " + (dark ? "Scuro" : "Chiaro"),
                "Abilita / gestisci notifiche", "Imposta come schermata Home"};
        new AlertDialog.Builder(this).setTitle("MBF Launcher · Personalizza")
                .setItems(items, (dialog, selection) -> {
                    if (selection == 0) {
                        prefs.edit().putBoolean("modern", !modern).apply();
                        buildHome();
                    } else if (selection == 1) {
                        prefs.edit().putBoolean("dark", !dark).apply();
                        buildHome();
                    } else if (selection == 2) {
                        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    } else {
                        try { startActivity(new Intent(Settings.ACTION_HOME_SETTINGS)); }
                        catch (Exception e) { Toast.makeText(this, "Apri Impostazioni → App predefinite → App Home", Toast.LENGTH_LONG).show(); }
                    }
                }).show();
    }

    @Override public void onBackPressed() {
        if (search != null && search.length() > 0) {
            search.setText("");
            search.clearFocus();
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(), 0);
        } else {
            super.onBackPressed();
        }
    }

    private static final class AppEntry {
        final String name;
        final ComponentName component;
        final Drawable icon;
        AppEntry(String name, ComponentName component, Drawable icon) {
            this.name = name.isEmpty() ? component.getPackageName() : name;
            this.component = component;
            this.icon = icon;
        }
    }
}
