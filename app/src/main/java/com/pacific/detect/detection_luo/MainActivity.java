package com.pacific.detect.detection_luo;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, callback
{
    private final String TAG = "luoyouren";
    private Context mContext = null;
    private Button mBtnPreview;
    private Button mBtnRecord;
    public CameraPreview mCameraPreview;
    public VideoCapture videoCapture = null;

    protected static final int MSG_PREVIEW = 0;
    protected static final int MSG_RECORD = 1;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();

        mBtnPreview = (Button) findViewById(R.id.btn_start_preview);
        mBtnRecord = (Button) findViewById(R.id.btn_start_record);
        mCameraPreview = (CameraPreview)findViewById(R.id.preview);

//        mBtnPreview = new Button(mContext);
//        mBtnPreview.setText("Open");
//        mBtnRecord = new Button(mContext);
//        mBtnPreview.setText("Record");
//
//        mCameraPreview = new CameraPreview(mContext);

        mBtnRecord.setOnClickListener(this);
        mBtnPreview.setOnClickListener(this);

        //回调：设置button text
        mCameraPreview.textCallback = this;

//        ViewGroup viewGroup = (RelativeLayout) findViewById(R.id.MainRelative);
//        viewGroup.addView(mBtnPreview);
//        viewGroup.addView(mBtnRecord);
//        viewGroup.addView(mCameraPreview, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        requestCameraPermission();
    }


    @TargetApi(23)
    private void requestCameraPermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            } else {
            }
        } else {

        }
    }


    @Override
    public void onClick(View v)
    {
        if (v == mBtnPreview) {

            sendMsg(MSG_PREVIEW, "");
        } else if (v == mBtnRecord){

            sendMsg(MSG_RECORD, "");
        }
    }

    private static class MyHandler extends Handler{

        private final String TAG = "luoyouren";
        //对Activity的弱引用
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity){
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if(activity==null){
                super.handleMessage(msg);
                return;
            }
            switch (msg.what) {
                case MSG_PREVIEW:
                    Log.i(TAG, "start preview");
                    activity.mCameraPreview.initPreview();

                    break;


                case MSG_RECORD:
                    Log.i(TAG, "start record");
                    activity.mCameraPreview.initRecord();

                    break;

                default:
                    break;
            }
        }
    }

    private final MyHandler mHandler = new MyHandler(this);

    private void sendMsg(int msgID, Object obj) {
        Message msg = mHandler.obtainMessage();
        msg.what = msgID;
        msg.obj = obj;
        mHandler.sendMessage(msg);
    }


    @Override
    protected void onDestroy()
    {
        mCameraPreview.uninitPreview();

        Log.d(TAG, "onCreate method is onDestroy");
        super.onDestroy();
    }


    @Override
    public void setViewText(int id, String str) {
        // TODO Auto-generated method stub
        switch (id) {
            case CameraPreview.SET_PREVIEW_TEXT:
                mBtnPreview.setText(str);
                break;
            case CameraPreview.SET_RECORD_TEXT:
                mBtnRecord.setText(str);
                break;

            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
    }


    @Override
    protected void onResume() {
        super.onResume();
//        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this, mOpenCVCallBack);

        //添加一下代码即可    （mLoaderCallback为BaseLoaderCallback对象）
        if(OpenCVLoader.initDebug()){
            mOpenCVCallBack.onManagerConnected( LoaderCallbackInterface.SUCCESS);

        }
    }

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    //处理相关工作,加载库
                    // System.loadLibrary("nonfree");
                    Toast toast = Toast.makeText(getApplicationContext(), "load OpenCVManager Success!", Toast.LENGTH_LONG);
                    toast.show();
                    Log.i(TAG, "load OpenCVManager Success!");

                    // luoyouren: 非常注意！！！所有OpenCV对象的操作都要在OpenCVLoader.initDebug()之后，特别要避免View的构造函数里面有OpenCV object创建！
                    videoCapture = new VideoCapture();
                    mCameraPreview.setVideoCapture(videoCapture);

                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };
}
