package com.faceahaha.huang.facehaha;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private FloatingActionButton fab_detect;
    private FloatingActionButton fab_save;
    private FloatingActionButton fab_pick;
    private FloatingActionButton fab_share;
    private FloatingActionButton fab_select;
    private FloatingActionButton fab_camera;
    private String Item;
    private String mCurrentPhotoStr;
    private Bitmap mPhotoImg;
    private ImageView mphoto;
    private Paint mPaint;
    private Bitmap mPhotoImg_changed;
    private boolean isDrawed = true;
    private View mWaitting;
    private int count = 0;
    private String CurrentImageUri;
    private String mFilePath;
    private Bitmap mFacebitmap;
    private Rect mSrcRect, mDestRect;
    private int mScreenWidth, mScreenHeight;

    private Intent intent2;

    public static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 0x144;
    public static final int MY_PERMISSIONS_REQUEST_ACCESS_WRITE_LOCATION = 0x145;
    public static final int PICK_CODE = 0x117;
    public static final int REQUEST_CODE = 0x110;
    public static final int SNAP_CODE = 0x255;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_ACCESS_WRITE_LOCATION);
            ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.CAMERA);
        }

        requestContactPermission();
        initViews();

        initEvents();

        //----------储存
        mFilePath = Environment.getExternalStorageDirectory().getPath();
        mFilePath = mFilePath + "/DCIM/" + "temp.png";
        getScreenSize();

        //-----------------------实现调用系统分享功能---------------------

        fab_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                SaveImg();
                Uri imageUri = Uri.fromFile(new File(CurrentImageUri));
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                shareIntent.setType("image/*");
                startActivity(Intent.createChooser(shareIntent, "分享到"));

            }
        });

        //------------------保存图片到本地的文件夹的方法----------------------

        fab_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SaveImg();
            }
        });


    }

    private void initViews() {
        //-----------------初始化控件----------------

        //-----------初始化绘制线框的画笔 ------------
        mPaint = new Paint();
        mWaitting = findViewById(R.id.id_waitting);
        mphoto = (ImageView) findViewById(R.id.id_photo);

        fab_camera = (FloatingActionButton) findViewById(R.id.id_fab_camera);
        fab_detect = (FloatingActionButton) findViewById(R.id.id_fab_detect);
        fab_save = (FloatingActionButton) findViewById(R.id.id_fab_save);
        fab_pick = (FloatingActionButton) findViewById(R.id.id_fab_pick);
        fab_share = (FloatingActionButton) findViewById(R.id.id_fab_share);
        fab_select = (FloatingActionButton) findViewById(R.id.id_fab_select);

    }

    //-----------声明点击事件-----------

    private void initEvents() {
        fab_camera.setOnClickListener(this);
        fab_select.setOnClickListener(this);
        fab_pick.setOnClickListener(this);
        fab_detect.setOnClickListener(this);
    }

    //-----------处理返回识别结果的消息----------

    private static final int MSG_SUCCESS = 0x111;
    private static final int MSG_ERROR = 0x112;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SUCCESS:
                    mWaitting.setVisibility(View.GONE);
                    JSONObject rs = (JSONObject) msg.obj;

                    //准备好变化了的画布
                    try {
                        prepareRsBitmap(rs);
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "请按右下角选脸按钮，选择你要换的脸~", Toast.LENGTH_SHORT).show();
                    }

                    //将变化了的画布应用到视图上面
                    try {

                        if (isDrawed) {
                            mphoto.setImageBitmap(mPhotoImg_changed);
                            isDrawed = false;
                        } else {
                            mphoto.setImageBitmap(mPhotoImg_changed);
                            isDrawed = true;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                case MSG_ERROR:
                    mWaitting.setVisibility(View.GONE);
                    String errorMsg = (String) msg.obj;

                    if (TextUtils.isEmpty(errorMsg)) {
                        Toast.makeText(getApplicationContext(), "请保持网络通畅才能识别～", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "错误消息：" + errorMsg, Toast.LENGTH_LONG).show();
                    }

                    break;
            }

            super.handleMessage(msg);
        }
    };


    //------横竖屏切换--------

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
    }

    //--------点击事件的逻辑实现--------
    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.id_fab_select:

                Intent intent = new Intent(MainActivity.this, SelectAct.class);
                startActivityForResult(intent, REQUEST_CODE);

                break;

            case R.id.id_fab_pick:

                requestContactPermission();
                count++;
                Intent intent_pick = new Intent(Intent.ACTION_PICK);
                intent_pick.setType("image/*");
                startActivityForResult(intent_pick, PICK_CODE);

                break;

            case R.id.id_fab_camera:
                requestContactPermission();
//              Uri photoUri = Uri.fromFile(new File(mFilePath));
                Uri photoUri = FileProvider.getUriForFile(MainActivity.this, MainActivity.this.getApplicationContext().getPackageName() + ".provider", new File(mFilePath));
                intent2 = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                intent2.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(intent2, SNAP_CODE);
                creatingShourtCuts();

                break;

            case R.id.id_fab_detect:

                mWaitting.setVisibility(View.VISIBLE);
                remoteShortCuts();

                if (count == 0) {
                    mWaitting.setVisibility(View.INVISIBLE);
                    Toast.makeText(getApplicationContext(), "这里没有人脸呐，快去选一张", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {

                    FaceppDetect.detect(mPhotoImg, new FaceppDetect.CallBack() {
                        @Override
                        public void success(JSONObject result) {
                            Message msg = Message.obtain();
                            msg.what = MSG_SUCCESS;
                            msg.obj = result;
                            mHandler.sendMessage(msg);
                        }

                        @Override
                        public void error(FaceppParseException exception) {
                            Message msg = Message.obtain();
                            msg.what = MSG_ERROR;
                            msg.obj = exception.getErrorMessage();
                            mHandler.sendMessage(msg);
                        }
                    });
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "这里没有人脸呐", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        //-----------检查模式----------

        switch (requestCode) {
            case REQUEST_CODE:

                if (resultCode == RESULT_OK) {
                    Item = intent.getStringExtra("name");

                    if (Item == null || Item.equals("开始")) {
                        Toast.makeText(getApplicationContext(), "没有脸,去选一张脸吧~", Toast.LENGTH_SHORT).show();
                        mphoto.setImageBitmap(null);
                    } else {
                        //--------用户自选人脸逻辑------
                        Toast.makeText(getApplicationContext(), Item, Toast.LENGTH_SHORT).show();

                        if (Item.equals("馆长脸")) {
                            mFacebitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.guanzhang_face)).getBitmap();
                        }

                    }

                }
                break;

            //----------选图检查---------

            case PICK_CODE:

                if (intent != null) {
                    requestContactPermission();
                    Uri uri = intent.getData();
                    Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                    cursor.moveToFirst();

                    int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    mCurrentPhotoStr = cursor.getString(idx);
                    cursor.close();
                    resizePhoto();
                    mphoto.setImageBitmap(mPhotoImg);

                } else {
                    Toast.makeText(MainActivity.this, "哎，怎么不选图了呢？要炸!!", Toast.LENGTH_SHORT).show();
                }

                break;

            case SNAP_CODE:

                count++;

                FileInputStream fis = null;
                try {

                    fis = new FileInputStream(mFilePath);
                    mPhotoImg = BitmapFactory.decodeStream(fis);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {

                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }

                }

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;

                BitmapFactory.decodeFile(mFilePath, options);
                double radio = Math.max(options.outWidth * 1.0d / 1024f, options.outHeight * 1.0d / 1024f);
                options.inSampleSize = (int) Math.ceil(radio);
                options.inJustDecodeBounds = false;
                mPhotoImg = BitmapFactory.decodeFile(mFilePath, options);

                mphoto.setImageBitmap(mPhotoImg);

                break;
        }

        super.onActivityResult(requestCode, resultCode, intent);

    }

    //-----------权限申请----------

    private void requestContactPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_ACCESS_WRITE_LOCATION);
            ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_ACCESS_WRITE_LOCATION);
            ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.CAMERA);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
    }

    //----------------获取屏幕长宽------------
    private void getScreenSize() {
        WindowManager wm = this.getWindowManager();
        mScreenWidth = wm.getDefaultDisplay().getWidth();
        mScreenHeight = wm.getDefaultDisplay().getHeight();
    }
    //--------对用户选择的图片进行大小缩放处理---------

    private void resizePhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoStr, options);
        double radio = Math.max(options.outWidth * 1.0d / 1024f, options.outHeight * 1.0d / 1024f);
        options.inSampleSize = (int) Math.ceil(radio);
        options.inJustDecodeBounds = false;
        mPhotoImg = BitmapFactory.decodeFile(mCurrentPhotoStr, options);
    }

    //----------------------创建画布-------------------------

    private void prepareRsBitmap(JSONObject rs) {
        //------------将Drawable里面的.png人脸图片转换为bitmap以便绘制------------
        //-------------获得位图资源-----------

        Bitmap bitmap = Bitmap.createBitmap(mPhotoImg.getWidth(), mPhotoImg.getHeight(), mPhotoImg.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mPhotoImg, 0, 0, null);

        try {
            //-----------------获取人脸数量-----------------
            JSONArray faces = rs.getJSONArray("face");
            int faceCount = faces.length();
            Toast.makeText(getApplicationContext(), "找到  " + faceCount + " 张脸", Toast.LENGTH_SHORT).show();

            for (int i = 0; i < faceCount; i++) {
                //-------------获取人脸位置信息-----------
                JSONObject face = faces.getJSONObject(i);
                JSONObject posObj = face.getJSONObject("position");

                //-------------表情包绘制-----------

                float nose_x = (float) posObj.getJSONObject("nose").getDouble("x") / 100 * bitmap.getWidth();
                float nose_y = (float) posObj.getJSONObject("nose").getDouble("y") / 100 * bitmap.getHeight();

                float left_eye_x = (float) posObj.getJSONObject("eye_left").getDouble("x") / 100 * bitmap.getWidth();
                float left_eye_y = (float) posObj.getJSONObject("eye_left").getDouble("y") / 100 * bitmap.getHeight();

                float right_eye_x = (float) posObj.getJSONObject("eye_right").getDouble("x") / 100 * bitmap.getWidth();
                float right_eye_y = (float) posObj.getJSONObject("eye_right").getDouble("y") / 100 * bitmap.getHeight();

                float mouth_left_x = (float) posObj.getJSONObject("mouth_left").getDouble("x") / 100 * bitmap.getWidth();
                float mouth_left_y = (float) posObj.getJSONObject("mouth_left").getDouble("y") / 100 * bitmap.getHeight();

                float mouth_right_x = (float) posObj.getJSONObject("mouth_right").getDouble("x") / 100 * bitmap.getWidth();
                float mouth_right_y = (float) posObj.getJSONObject("mouth_right").getDouble("y") / 100 * bitmap.getHeight();

                float mMouthWidth = mouth_right_x - mouth_left_x;
                float mChangedX = mouth_left_x - left_eye_x;
                float mChangedY = mouth_left_y - left_eye_y;
                float mTwoEyesX = right_eye_x - left_eye_x;
                double mChangedRoation = Math.atan(mChangedX / mChangedY);

                //-----------------------------------
                float x = (float) posObj.getJSONObject("center").getDouble("x");
                float y = (float) posObj.getJSONObject("center").getDouble("y");
                float w = (float) posObj.getDouble("width");
                float h = (float) posObj.getDouble("height");

                x = x / 100 * bitmap.getWidth();
                y = y / 100 * bitmap.getHeight();
                w = w / 100 * bitmap.getWidth();
                h = h / 100 * bitmap.getHeight();

                //drawing绘制矩形线框--------------------------------
                mPaint.setColor(0xffffffff);
                mPaint.setStrokeWidth(5);

                //--------------绘制表情部分-------------
                //--------------将json对象里面的人脸信息方位赋值给方位容器-----------

                mSrcRect = new Rect(0, 0, (int) mMouthWidth * 100, (int) h / 3 * 100);
                mDestRect = new Rect((int) mouth_left_x, (int) mouth_left_y, (int) mouth_left_x + mFacebitmap.getWidth(), (int) mouth_left_y + mFacebitmap.getHeight());

                //--------实现接受用户选择脸回调逻辑------
                if (bitmap.getWidth() < mphoto.getWidth() && bitmap.getHeight() < mphoto.getHeight()) {
                    float radio = Math.max(bitmap.getWidth() * 1.0f / mphoto.getWidth(), bitmap.getHeight() * 1.0f / mphoto.getHeight());
                    if (Item != null) {
                        if (Item.equals("馆长脸")) {
                            mFacebitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.guanzhang_face)).getBitmap();
                        }
                    }
                    mFacebitmap = Bitmap.createScaledBitmap(mFacebitmap, (int) (mFacebitmap.getWidth() * radio), (int) (mFacebitmap.getHeight() * radio), false);
                }

                //-----------实现换脸算法-------
                mFacebitmap = ResizemFace(mFacebitmap, w * 1.27f, h * 1.85f);
                //---------绘制5大定位点---------
                canvas.drawCircle(nose_x, nose_y, 4, mPaint);
                canvas.drawCircle(left_eye_x, left_eye_y, 4, mPaint);
                canvas.drawCircle(right_eye_x, right_eye_y, 4, mPaint);
                canvas.drawCircle(mouth_right_x, mouth_right_y, 4, mPaint);
                canvas.drawCircle(mouth_left_x, mouth_left_y, 4, mPaint);
                //--------绘制嘴角线---------
                // canvas.drawLine(mouth_right_x,mouth_right_y,mouth_left_x,mouth_left_y,mPaint);
                //---------旋转角度回调------
                int mChangedRoationNum = (int) (-180 * mChangedRoation / Math.PI);
                int mChangedRoationNumAbs = Math.abs(mChangedRoationNum);
                mFacebitmap = adjustPhotoRoation(mFacebitmap, mChangedRoationNum);
                //----------贴图方位计算公式----------
                canvas.drawBitmap(mFacebitmap,
                        x - w / 2 - 0.562f * mTwoEyesX - (2 * mChangedRoationNumAbs), y - h / 2 - (1.73f * mTwoEyesX) /*+ mChangedRoationNum*/, null);
                mPhotoImg_changed = bitmap;
                isDrawed = false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //----------------动态调整贴图大小-------------
    private static Bitmap ResizemFace(Bitmap bitmap, float mWidth, float mHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = (mWidth) / width;
        float scaleHeight = (mHeight) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth + 0.065f, scaleHeight + 0.065f);
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width,
                height, matrix, true);
        return resizedBitmap;
    }

    //--------------动态实现贴图旋转------------

    private Bitmap adjustPhotoRoation(Bitmap bitmap, final int orientationDegree) {
        Matrix matrix = new Matrix();
        //从规则图形的中心开始旋转-------------------
        matrix.setRotate(orientationDegree, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return bitmap;
    }

    //-------------------------------实现图片保存逻辑-------------------------
    //--------原理:将ImageView里面的照片保存----------
    private void SaveImg() {
        mWaitting.setVisibility(View.VISIBLE);
        requestContactPermission();

        String sdStatus = Environment.getExternalStorageState();
        if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) {  // 检测sd是否可用
            Log.v("TestFile", "SD card is not avaiable/writeable right now.");
        }
        // 创建文件夹
        mphoto.setDrawingCacheEnabled(true);
        Bitmap Pic_save = mphoto.getDrawingCache();
        FileOutputStream b = null;
        File file = new File("/sdcard/DCIM/FaceHahaPics");
        file.mkdirs();
        CurrentImageUri = "/sdcard/DCIM/FaceHahaPics/" + System.currentTimeMillis() + ".jpg";

        try {
            b = new FileOutputStream(CurrentImageUri);
            Pic_save.compress(Bitmap.CompressFormat.JPEG, 100, b);// 把数据写入文件
            Toast.makeText(getApplicationContext(), "图片已成功保存到了DCIM目录下！", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {

            e.printStackTrace();

        } finally {
            try {
                mWaitting.setVisibility(View.GONE);
                b.flush();
                b.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mphoto.setDrawingCacheEnabled(false);
    }

    //创建APPshortcuts的方法
    @TargetApi(25)
    private void creatingShourtCuts() {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

        Intent intent1 = new Intent(getApplicationContext(), SelectAct.class);
        intent1.setAction(Intent.ACTION_VIEW);

        Intent intent2 = new Intent(getApplicationContext(), MainActivity.class);
        intent2.setAction(Intent.ACTION_MAIN);

        Intent[] intents = {intent2, intent1};

        ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "id1")
                .setShortLabel("Web site")
                .setLongLabel("Faces")
                .setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_face_black_24dp))
                .setIntents(intents)
                .build();

        shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut));

    }

    @TargetApi(25)
    private void remoteShortCuts() {
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

//        shortcutManager.removeDynamicShortcuts(shortId);
        shortcutManager.removeAllDynamicShortcuts();
    }


}
