package de.wico.fatburner.activity;

import android.graphics.BlurMaskFilter;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sccomponents.gauges.ScGauge;
import com.sccomponents.gauges.ScLinearGauge;
import com.sccomponents.gauges.ScNotches;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import de.wico.fatburner.R;
import de.wico.fatburner.logging.AndroidLogger;

public class MainActivity extends AppCompatActivity {

    private static final AndroidLogger LOG = AndroidLogger.get(MainActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final LayoutInflater layoutInflater = LayoutInflater.from(this);


        final ScLinearGauge gauge = (ScLinearGauge) this.findViewById(R.id.fatProgressBar);
        initProgress(gauge);

        final Button helpButton = (Button) findViewById(R.id.helpButton);
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View helpDialogView = layoutInflater.inflate(R.layout.dialog_help, null);
                TextView msg = (TextView) helpDialogView.findViewById(R.id.helpText);
                Spanned sp = Html.fromHtml(getString(R.string.help_dialog_content));
                msg.setText(sp);
                final AlertDialog helpDialog = new AlertDialog.Builder(MainActivity.this).setView(helpDialogView).show();
                helpDialogView.findViewById(R.id.closeHelpButton).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        helpDialog.hide();
                    }
                });
                Button licensesButton = (Button) helpDialog.findViewById(R.id.licensesButon);
                licensesButton.setBackgroundColor(Color.TRANSPARENT);
                licensesButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final View licensesDialogView = layoutInflater.inflate(R.layout.dialog_licenses, null);
                        TextView msg = (TextView) licensesDialogView.findViewById(R.id.licensesText);
                        Spanned sp = Html.fromHtml(getString(R.string.licenses_dialog_content));
                        msg.setText(sp);
                        final AlertDialog licensesDialog = new AlertDialog.Builder(MainActivity.this).setView(licensesDialogView).show();
                        licensesDialog.findViewById(R.id.closeLicenesButton).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                licensesDialog.hide();
                            }
                        });
                    }
                });
            }
        });
        final Button burnFatButton = (Button) findViewById(R.id.burnFatButton);
        final CapturePreview cameraView = (CapturePreview) findViewById(R.id.cameraView);
        cameraView.setProgressBar(gauge);
        cameraView.setMainActivity(this);
        final TextView progressText = (TextView) findViewById(R.id.progressText);
        final AtomicReference<Thread> threadRef = new AtomicReference<>();
        burnFatButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // PRESSED
                        LOG.debug("Pressed");
                        threadRef.set(createDisplayProgressThread(MainActivity.this, progressText, cameraView));
                        threadRef.get().start();
                        cameraView.enableFlicker();
                        return true; // if you want to handle the touch event
                    case MotionEvent.ACTION_UP:
                        // RELEASED
                        LOG.debug("Released");
                        cameraView.disableFlicker();
                        threadRef.get().interrupt();
                        return true; // if you want to handle the touch event
                }
                return true;
            }
        });
    }

    private void initProgress(ScLinearGauge gauge) {
        gauge.setHighValue(100);
    }

    private Thread createDisplayProgressThread(final MainActivity mainActivity, final TextView progressText, final CapturePreview cameraView) {
        return new Thread() {
            @Override
            public void run() {
                final Random random = new Random();
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressText.setVisibility(View.VISIBLE);
                            progressText.setText("Preparing burn mode...");
                        }
                    });
                    sleep(1000L);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressText.setText("Adjusting burn patterns...");
                        }
                    });
                    sleep(3000L);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cameraView.activateBurnMode();
                            progressText.setText("Burning fat...");
                        }
                    });
                    sleep(5000L);
                    cameraView.disableFlicker();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int reduction = random.nextInt(20) + 5;
                            progressText.setText("Burning successful.\nFat reduced by " + reduction + "%.");
                        }
                    });
                    sleep(4000L);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressText.setVisibility(View.INVISIBLE);
                        }
                    });
                } catch (InterruptedException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                sleep(2000L);
                            } catch (InterruptedException e1) {
                                // logwut
                            }
                            progressText.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            }
        };
    }
}
