package ro.pbinfo.autosolver;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView webView;
    private EditText problemId, backendUrl;
    private TextView status, result;
    private Button solveButton;
    private String currentId = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(14, 10, 14, 10);

        TextView title = new TextView(this);
        title.setText("PBInfo AutoSolver");
        title.setTextSize(22);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        problemId = new EditText(this);
        problemId.setSingleLine(true);
        problemId.setHint("PBInfo problem ID");
        root.addView(problemId);

        backendUrl = new EditText(this);
        backendUrl.setSingleLine(true);
        backendUrl.setText("https://YOUR-SERVER.example/solve");
        backendUrl.setHint("Your secure AI backend URL");
        root.addView(backendUrl);

        Button open = new Button(this);
        open.setText("1. Open problem");
        root.addView(open);
        open.setOnClickListener(v -> openProblem());

        solveButton = new Button(this);
        solveButton.setText("2. Read problem and generate solution");
        root.addView(solveButton);
        solveButton.setOnClickListener(v -> extractAndSolve());

        status = new TextView(this);
        status.setText("Sign in to PBInfo below, then open a problem.");
        root.addView(status);

        result = new TextView(this);
        result.setTextIsSelectable(true);
        result.setTextColor(Color.BLACK);
        result.setPadding(4, 8, 4, 8);
        ScrollView resultScroll = new ScrollView(this);
        resultScroll.addView(result);
        root.addView(resultScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(new PageBridge(), "AndroidPage");
        webView.setWebViewClient(new WebViewClient());
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 2));
        setContentView(root);
        webView.loadUrl("https://www.pbinfo.ro/");
    }

    private void openProblem() {
        String id = problemId.getText().toString().trim();
        if (!id.matches("[0-9]+")) {
            problemId.setError("Enter a numeric problem ID");
            return;
        }
        currentId = id;
        status.setText("Loading problem " + id + "…");
        result.setText("");
        webView.loadUrl("https://www.pbinfo.ro/probleme/" + id);
    }

    private void extractAndSolve() {
        String id = problemId.getText().toString().trim();
        String endpoint = backendUrl.getText().toString().trim();
        if (!id.matches("[0-9]+")) { problemId.setError("Enter a numeric problem ID"); return; }
        if (!endpoint.startsWith("https://") || endpoint.contains("YOUR-SERVER")) {
            backendUrl.setError("Set your deployed HTTPS AI backend URL first");
            status.setText("The AI service has not been configured yet.");
            return;
        }
        currentId = id;
        solveButton.setEnabled(false);
        status.setText("Reading the visible problem text from PBInfo…");
        webView.evaluateJavascript(
            "(function(){return document.body ? document.body.innerText : '';})()",
            value -> {
                String text = value;
                try {
                    text = new org.json.JSONTokener(value).nextValue().toString();
                } catch (Exception ignored) {}
                if (text == null || text.length() < 80) {
                    status.setText("Problem text not found. Open the problem page and try again.");
                    solveButton.setEnabled(true);
                    return;
                }
                sendToBackend(endpoint, id, text);
            }
        );
    }

    private void sendToBackend(String endpoint, String id, String statement) {
        status.setText("Sending problem to AI solver…");
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                JSONObject payload = new JSONObject();
                payload.put("problem_id", id);
                payload.put("statement", statement);
                conn = (HttpURLConnection) new URL(endpoint).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(90000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = conn.getOutputStream()) { out.write(body); }
                int code = conn.getResponseCode();
                InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line; while ((line = reader.readLine()) != null) response.append(line);
                }
                if (code < 200 || code >= 300) throw new Exception("Server error " + code + ": " + response);
                JSONObject json = new JSONObject(response.toString());
                String solution = json.optString("solution", "");
                String explanation = json.optString("explanation", "");
                if (solution.isEmpty()) throw new Exception("The AI server did not return a solution.");
                final String display = "C++ SOLUTION\n\n" + solution + "\n\nEXPLANATION\n\n" + explanation
                    + "\n\nReview and test this solution before submitting it to PBInfo.";
                runOnUiThread(() -> {
                    result.setText(display);
                    status.setText("Solution received. Review it before submitting.");
                    solveButton.setEnabled(true);
                });
            } catch (Exception e) {
                final String message = e.getMessage() == null ? "Unknown connection error" : e.getMessage();
                runOnUiThread(() -> {
                    status.setText("Could not generate solution: " + message);
                    solveButton.setEnabled(true);
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private class PageBridge {
        @JavascriptInterface public void pageLoaded(String ignored) {}
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
