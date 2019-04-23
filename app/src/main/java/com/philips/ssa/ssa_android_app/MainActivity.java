package com.philips.ssa.ssa_android_app;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void StartLogin_click(View view) {
        Intent nextActivityView;
        nextActivityView = new Intent(this, LoginActivity.class);
        startActivity(nextActivityView);
        finish();
    }

}
