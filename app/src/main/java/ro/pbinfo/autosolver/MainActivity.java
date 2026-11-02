package ro.pbinfo.autosolver;

import android.app.Activity;
import android.os.Bundle;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String BACKEND_URL = "https://pbinfoautosolver2.onrender.com/solve";
    private static final int NAVY = Color.rgb(18, 25, 48);
    private static final int ACCENT = Color.rgb(91, 92, 240);
    private static final int PAGE = Color.rgb(244, 246, 251);
    private static final int MUTED = Color.rgb(105, 114, 133);

    private WebView webView;
    private EditText problemId;
    private TextView status, result;
    private Button solveButton;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(Color.rgb(12, 17, 34));
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PAGE);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(20), dp(17), dp(20), dp(17));
        header.setBackgroundColor(NAVY);

        TextView brand = new TextView(this);
        brand.setText("PBINFO  /  AI TOOLS");
        brand.setTextColor(Color.rgb(174, 179, 255));
        brand.setTextSize(11);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(brand);

        TextView title = new TextView(this);
        title.setText("AutoSolver");
        title.setTextColor(Color.WHITE);
        title.setTextSize(27);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(3);
        header.addView(title, titleParams);

        TextView subtitle = new TextView(this);
        subtitle.setText("Find a problem. Generate a C++17 solution.");
        subtitle.setTextColor(Color.rgb(195, 201, 218));
        subtitle.setTextSize(13);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(3);
        header.addView(subtitle, subParams);
        root.addView(header);

        // Scrollable solver panel
        ScrollView panelScroll = new ScrollView(this);
        panelScroll.setFillViewport(false);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout inputCard = card();
        TextView section = heading("01  ·  PROBLEM");
        inputCard.addView(section);

        TextView idLabel = smallLabel("PBINFO PROBLEM ID");
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = dp(13);
        inputCard.addView(idLabel, labelParams);

        problemId = new EditText(this);
        problemId.setSingleLine(true);
        problemId.setTextSize(16);
        problemId.setTextColor(NAVY);
        problemId.setHintTextColor(Color.rgb(153, 160, 176));
        problemId.setHint("e.g. 100");
        problemId.setInputType(2);
        problemId.setPadding(dp(13), 0, dp(13), 0);
        problemId.setBackground(rounded(Color.rgb(247, 248, 252), Color.rgb(224, 228, 238), 12));
        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(-1, dp(52));
        fieldParams.topMargin = dp(7);
        inputCard.addView(problemId, fieldParams);

        Button open = actionButton("Open problem on PBInfo", false);
        LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(-1, dp(48));
        openParams.topMargin = dp(12);
        inputCard.addView(open, openParams);
        open.setOnClickListener(v -> openProblem());

        solveButton = actionButton("Generate C++17 solution", true);
        LinearLayout.LayoutParams solveParams = new LinearLayout.LayoutParams(-1, dp(50));
        solveParams.topMargin = dp(9);
        inputCard.addView(solveButton, solveParams);
        solveButton.setOnClickListener(v -> extractAndSolve());

        status = new TextView(this);
        status.setText("Sign in to PBInfo in the browser below, then open a problem.");
        status.setTextColor(MUTED);
        status.setTextSize(12);
        status.setPadding(dp(2), dp(12), dp(2), dp(2));
        inputCard.addView(status);

        panel.addView(inputCard);

        LinearLayout resultCard = card();
        LinearLayout resultHeader = new LinearLayout(this);
        resultHeader.setOrientation(LinearLayout.HORIZONTAL);
        resultHeader.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView resultHeading = heading("02  ·  AI RESULT");
        resultHeader.addView(resultHeading, new LinearLayout.LayoutParams(0, -2, 1));

        Button copy = actionButton("Copy", false);
        copy.setTextSize(12);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(dp(82), dp(36));
        resultHeader.addView(copy, copyParams);
        copy.setOnClickListener(v -> copyResult());
        resultCard.addView(resultHeader);

        result = new TextView(this);
        result.setText("Your generated solution and explanation will appear here.");
        result.setTextColor(MUTED);
        result.setTextSize(13);
        result.setTextIsSelectable(true);
        result.setPadding(0, dp(14), 0, dp(5));
        resultCard.addView(result);

        LinearLayout.LayoutParams resultCardParams = new LinearLayout.LayoutParams(-1, -2);
        resultCardParams.topMargin = dp(12);
        panel.addView(resultCard, resultCardParams);

        TextView note = new TextView(this);
        note.setText("AI solutions can contain mistakes. Test the code and check the task requirements before using it.");
        note.setTextColor(MUTED);
        note.setTextSize(11);
        note.setPadding(dp(4), dp(10), dp(4), dp(8));
        panel.addView(note);

        panelScroll.addView(panel);
        root.addView(panelScroll, new LinearLayout.LayoutParams(-1, 0, 1.12f));

        // PBInfo browser area
        LinearLayout browserHeader = new LinearLayout(this);
        browserHeader.setOrientation(LinearLayout.HORIZONTAL);
        browserHeader.setGravity(android.view.Gravity.CENTER_VERTICAL);
        browserHeader.setPadding(dp(16), dp(9), dp(16), dp(9));
        browserHeader.setBackgroundColor(Color.WHITE);

        TextView browserTitle = new TextView(this);
        browserTitle.setText("PBINFO BROWSER");
        browserTitle.setTextColor(NAVY);
        browserTitle.setTextSize(11);
        browserTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        browserHeader.addView(browserTitle, new LinearLayout.LayoutParams(0, -2, 1));

        TextView browserHint = new TextView(this);
        browserHint.setText("Login stays saved");
        browserHint.setTextColor(MUTED);
        browserHint.setTextSize(11);
        browserHeader.addView(browserHint);
        root.addView(browserHeader);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
                super.onPageFinished(view, url);
            }
        });
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
        webView.loadUrl("https://www.pbinfo.ro/");
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(15), dp(16), dp(14));
        box.setBackground(rounded(Color.WHITE, Color.rgb(232, 235, 243), 18));
        return box;
    }

    private TextView heading(String value) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(NAVY);
        t.setTextSize(14);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private TextView smallLabel(String value) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(MUTED);
        t.setTextSize(10);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button actionButton(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? Color.WHITE : ACCENT);
        b.setBackground(rounded(primary ? ACCENT : Color.rgb(239, 240, 255),
                primary ? ACCENT : Color.rgb(239, 240, 255), 13));
        b.setPadding(dp(12), 0, dp(12), 0);
        return b;
    }

    private GradientDrawable rounded(int fill, int stroke, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(problemId.getWindowToken(), 0);
            problemId.clearFocus();
        } catch (Exception ignored) {}
    }

    private void openProblem() {
        String id = problemId.getText().toString().trim();
        if (!id.matches("[0-9]+")) {
            problemId.setError("Enter a numeric problem ID");
            return;
        }
        hideKeyboard();
        status.setText("Opening problem " + id + " on PBInfo…");
        result.setText("Open the problem in the browser, then generate a solution.");
        webView.loadUrl("https://www.pbinfo.ro/probleme/" + id);
    }

    private void extractAndSolve() {
        String id = problemId.getText().toString().trim();
        if (!id.matches("[0-9]+")) {
            problemId.setError("Enter a numeric problem ID");
            return;
        }
        hideKeyboard();
        solveButton.setEnabled(false);
        solveButton.setAlpha(0.6f);
        status.setText("Reading the problem page…");
        webView.evaluateJavascript(
            "(function(){return document.body ? document.body.innerText : '';})()",
            value -> {
                String statement = value;
                try {
                    statement = new org.json.JSONTokener(value).nextValue().toString();
                } catch (Exception ignored) {}
                if (statement == null || statement.length() < 80) {
                    status.setText("Problem text not found. Open the problem page and try again.");
                    enableSolve();
                    return;
                }
                sendToBackend(id, statement);
            }
        );
    }

    private void sendToBackend(String id, String statement) {
        runOnUiThread(() -> status.setText("Sending the problem to OpenRouter AI…"));
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                JSONObject payload = new JSONObject();
                payload.put("problem_id", id);
                payload.put("statement", statement);
                conn = (HttpURLConnection) new URL(BACKEND_URL).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(25000);
                conn.setReadTimeout(120000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = conn.getOutputStream()) { out.write(body); }

                int code = conn.getResponseCode();
                InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                StringBuilder response = new StringBuilder();
                if (stream != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) response.append(line);
                    }
                }
                if (code < 200 || code >= 300) throw new Exception("Server error " + code + ": " + response);

                JSONObject json = new JSONObject(response.toString());
                String solution = json.optString("solution", "");
                String explanation = json.optString("explanation", "");
                if (solution.isEmpty()) throw new Exception("The AI server returned an empty solution.");

                final String display = "C++17 SOLUTION\n\n" + solution + "\n\nEXPLANATION\n\n" + explanation;
                runOnUiThread(() -> {
                    result.setText(display);
                    status.setText("Solution generated. Review and test it before using it.");
                    enableSolve();
                });
            } catch (Exception e) {
                final String message = e.getMessage() == null ? "Unknown connection error" : e.getMessage();
                runOnUiThread(() -> {
                    status.setText("Could not generate solution: " + message);
                    enableSolve();
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void enableSolve() {
        solveButton.setEnabled(true);
        solveButton.setAlpha(1f);
    }

    private void copyResult() {
        String value = result.getText().toString();
        if (value.isEmpty() || value.startsWith("Your generated")) {
            Toast.makeText(this, "No solution to copy yet", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("PBInfo solution", value));
        Toast.makeText(this, "Result copied", Toast.LENGTH_SHORT).show();
    }

    @Override protected void onPause() {
        if (webView != null) CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
