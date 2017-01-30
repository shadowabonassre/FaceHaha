package com.faceahaha.huang.facehaha;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;


public class Guide extends Activity implements ViewPager.OnPageChangeListener{

    private ViewPager vp;
    private ViewPagerAdapter vpAdapter;
    private List<View> views;
    private ImageView[] dots;
    private int[] ids = {R.id.iv1,R.id.iv2,R.id.iv3,R.id.iv4,R.id.iv5};
    private Button start_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide);
        initViews();
        initDots();

        start_btn = (Button)views.get(4).findViewById(R.id.start_btn);
        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Guide.this,MainActivity.class);
                startActivity(i);
                finish();
            }
        });

    }

    private void initViews()
    {
        LayoutInflater inflater = LayoutInflater.from(this);

        views = new ArrayList<View>();
        views.add(inflater.inflate(R.layout.guide_one,null));
        views.add(inflater.inflate(R.layout.guide_two,null));
        views.add(inflater.inflate(R.layout.guide_three,null));
        views.add(inflater.inflate(R.layout.guide_four,null));
        views.add(inflater.inflate(R.layout.guide_five,null));

        vpAdapter = new ViewPagerAdapter(views,this);
        vp = (ViewPager)findViewById(R.id.viewpager);
        vp.setAdapter(vpAdapter);

        vp.setOnPageChangeListener(this);

    }

    private void initDots()
    {
        dots = new ImageView[views.size()];
        for (int i = 0; i < views.size();i++)
        {
            dots[i] = (ImageView)findViewById(ids[i]);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int arg0) {
        for (int i = 0 ; i < ids.length; i++)
        {
            if (arg0 == i)
            {
                dots[i].setImageResource(R.drawable.blackdot);

            }
        else
            {
                dots[i].setImageResource(R.drawable.whitedot);

            }
        }

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}

