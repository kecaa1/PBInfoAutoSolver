package ro.pbinfo.autosolver;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private WebView webView;
    private EditText problemId;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(12, 12, 12, 12);

        TextView title = new TextView(this);
        title.setText("PBInfo AutoSolver — preview");
        title.setTextSize(20);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        problemId = new EditText(this);
        problemId.setSingleLine(true);
        problemId.setHint("PBInfo problem ID");
        root.addView(problemId, new LinearLayout.LayoutParams(-1, -2));

        Button open = new Button(this);
        open.setText("Open problem on PBInfo");
        root.addView(open);
        open.setOnClickListener(v -> openProblem());

        TextView note = new TextView(this);
        note.setText("Sign in on PBInfo in the page below. This starter app does not yet generate, compile, or submit solutions.");
        note.setTextColor(Color.DKGRAY);
        root.addView(note);

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        webView.loadUrl("https://www.pbinfo.ro/");
    }

    private void openProblem() {
        String id = problemId.getText().toString().trim();
        if (id.matches("[0-9]+")) {
            webView.loadUrl("https://www.pbinfo.ro/probleme/" + id);
        } else {
            problemId.setError("Enter a numeric problem ID");
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
