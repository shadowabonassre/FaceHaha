package com.faceahaha.huang.facehaha;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.Arrays;

public class SelectAct extends Activity{

    private static final String TAG = MainActivity.class.getSimpleName();
    private WheelView myChoose;

    private static final String[] NAMES = new String[]{"开始","玥玥脸","水神脸","梦妮脸", "一斌脸", "建川脸", "丑比脸", "罐头脸", "则神脸", "高航脸", "雨轩脸","剑飞脸","悦悦脸","馆长脸"};
    private FloatingActionButton fab_check;
    private String mySelectname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.selectact);

        //------设置Activity类型的Dialog不会因为点击其他区域而消失---------
        setFinishOnTouchOutside(false);

        myChoose = (WheelView)findViewById(R.id.id_choose);
        fab_check = (FloatingActionButton)findViewById(R.id.id_fab_check);

        //----------自定义滚动选择的回调----------

        myChoose.setOffset(1);
        myChoose.setItems(Arrays.asList(NAMES));
        myChoose.setOnWheelViewListener(new WheelView.OnWheelViewListener() {
            @Override
            public void onSelected(int selectedIndex, String item)
            {
                Log.d(TAG, "selectedIndex: " + selectedIndex + ", item: " + item);
                mySelectname = item;
            }

        });

        //----------设置确认键---------

        fab_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(SelectAct.this,MainActivity.class);
                intent.putExtra("name",mySelectname);
                setResult(RESULT_OK,intent);
                finish();
            }
        });

    }

}
