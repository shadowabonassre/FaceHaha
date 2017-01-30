package com.faceahaha.huang.facehaha;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.util.Arrays;

public class WelcomeAct extends Activity {

    private boolean isFirstIn = false;
    private static final int TIME = 2000;
    private static final int GO_HOME = 1000;
    private static final int GO_GUIDE = 1001;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                case GO_HOME:
                    setGoHome();
                    break;
                case GO_GUIDE:
                    setGoGuide();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcomeact);

        init();
    }

    private void init()
    {
        SharedPreferences preferences = getSharedPreferences("Hello",MODE_PRIVATE);
        isFirstIn = preferences.getBoolean("isFirstIn",true);
        if (!isFirstIn)
        {
            mHandler.sendEmptyMessageDelayed(GO_HOME,TIME);
        }else
        {
            mHandler.sendEmptyMessageDelayed(GO_GUIDE,TIME);

            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isFirstIn",false);
            editor.commit();
        }
    }

    private void setGoHome()
    {
        Intent i = new Intent(WelcomeAct.this,MainActivity.class);
        startActivity(i);
        finish();
    }

    private void setGoGuide()
    {
        Intent i = new Intent(WelcomeAct.this,Guide.class);
        startActivity(i);
        finish();
    }


}
