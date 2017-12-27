package com.mysoftsource.rxandroidlogger.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.mysoftsource.rxandroidlogger.TBPLogHelper;

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
                TBPLogHelper.getInstance().storeDropBox()
                        .subscribe(path -> Toast.makeText(getApplicationContext(), "Path saved: " + path, Toast.LENGTH_LONG).show(),
                                throwable -> Toast.makeText(getApplicationContext(), "Store logcat is failed " + throwable.getMessage(), Toast.LENGTH_LONG).show());
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
