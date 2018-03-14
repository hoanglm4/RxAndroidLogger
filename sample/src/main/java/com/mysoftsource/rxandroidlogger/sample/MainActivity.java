package com.mysoftsource.rxandroidlogger.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.mysoftsource.rxandroidlogger.TBPLogHelper;
import com.mysoftsource.rxandroidlogger.TPBLog;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Timber.i("onCreate");

        findViewById(R.id.click_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TBPLogHelper.getInstance().storeAllPreviousLogToDropBox()
                        .doOnSubscribe(() -> Log.d("MainActivity", "doOnSubscribe"))
                        .subscribe(path -> {
                            TPBLog.d( "Path saved: " + path);
                            Toast.makeText(getApplicationContext(), "Path saved: " + path, Toast.LENGTH_LONG).show();
                        }, throwable -> {
                            Toast.makeText(getApplicationContext(), "Store logcat is failed " + throwable.getMessage(), Toast.LENGTH_LONG);
                            TPBLog.e( throwable,"Store logcat is failed ");
                        });
            }
        });

        findViewById(R.id.dummy_log).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < 10000; i++) {
                    Timber.i("dummy " + i);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Timber.i("onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Timber.i("onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Timber.i("onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Timber.i("onDestroy");
    }
}
