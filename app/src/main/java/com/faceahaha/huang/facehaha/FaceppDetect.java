package com.faceahaha.huang.facehaha;

import android.graphics.Bitmap;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class FaceppDetect {

    //------------回调接口，返回两种结果----------

    public interface CallBack {
        void success(JSONObject result);

        void error(FaceppParseException exception);

    }

    //----------识别方法，实现于子线程----------

    public static void detect(final Bitmap bm, final CallBack callBack) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {

                    //-----------发送网络请求-----------
                    HttpRequests requests = new HttpRequests(Constant.KEY, Constant.SECRET, true, true);
                    Bitmap bmSmall = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight());
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bmSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] arrays = stream.toByteArray();
                    PostParameters params = new PostParameters();
                    params.setImg(arrays);
                    //----------获得返回的JSON对象--------
                    JSONObject jsonObject = requests.detectionDetect(params);

                    //----------打出log便于调试----------
                    Log.e("TAG", jsonObject.toString());

                    if (callBack != null) {
                        callBack.success(jsonObject);

                    }

                } catch (FaceppParseException e)

                {
                    e.printStackTrace();

                    if (callBack != null) {
                        callBack.error(e);
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
//                    Toast.makeText(, "", Toast.LENGTH_SHORT).show();
                }


            }
        }).start();
    }
}
