package com.example.castlesapp;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Full-screen result screen shown after every print attempt.
 *
 * Displayed for AUTO_DISMISS_MS then finishes automatically, returning
 * the user to the PIN pad.  The user can also tap anywhere to dismiss early.
 *
 * Launched by MainActivity.showResult():
 *
 *   Intent i = new Intent(this, PrintResultActivity.class);
 *   i.putExtra(EXTRA_SUCCESS, true);
 *   i.putExtra(EXTRA_MESSAGE, "Afgedrukt!");
 *   startActivity(i);
 */
public class PrintResultActivity extends Activity {

    public  static final String EXTRA_SUCCESS = "success";
    public  static final String EXTRA_MESSAGE = "message";
    private static final int    AUTO_DISMISS_MS = 3000;

    private final Handler  handler  = new Handler(Looper.getMainLooper());
    private       Runnable dismissR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on during the result display
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_print_result);

        boolean success = getIntent().getBooleanExtra(EXTRA_SUCCESS, false);
        String  message = getIntent().getStringExtra(EXTRA_MESSAGE);
        if (message == null) message = success
                ? getString(R.string.print_ok)
                : getString(R.string.error_print);

        ImageView icon    = findViewById(R.id.ivResultIcon);
        TextView  tvMsg   = findViewById(R.id.tvResultMessage);
        View      root    = findViewById(R.id.resultRoot);

        // Colours
        int bgColor   = getColor(success ? R.color.result_bg_success : R.color.result_bg_failure);
        int iconRes   = success ? R.drawable.ic_result_check : R.drawable.ic_result_cross;
        root.setBackgroundColor(bgColor);
        icon.setImageResource(iconRes);
        tvMsg.setText(message);

        // Pop-in animation on the icon
        icon.setScaleX(0f);
        icon.setScaleY(0f);
        icon.animate()
                .scaleX(1f).scaleY(1f)
                .setDuration(400)
                .setInterpolator(new OvershootInterpolator(2f))
                .start();

        // Auto-dismiss
        dismissR = this::finish;
        handler.postDelayed(dismissR, AUTO_DISMISS_MS);

        // Tap anywhere to dismiss early
        root.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dismissR != null) handler.removeCallbacks(dismissR);
    }
}
