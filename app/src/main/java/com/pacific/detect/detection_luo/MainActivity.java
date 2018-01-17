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

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
    private final String TAG = "luoyouren";

    private final int SELECT_PHOTO_1 = 1;
    private final int SELECT_PHOTO_2 = 2;
    private final int MAX_MATCHES = 50;
    private ImageView mImage1;
    private TextView mObject1;
    private TextView mObject2;
    private TextView mMatches;
    private TextView mTime;
    private long mStartTime;
    private long mEndTiem;
    private int keypointsObject1, keypointsObject2, keypointMatches;
    Mat src1, src2;
    static int ACTION_MODE = 0;
    private boolean src1Selected = false, src2Selected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImage1 = (ImageView) findViewById(R.id.iv_Image);
        mObject1 = (TextView) findViewById(R.id.tv_Object1);
        mObject2 = (TextView) findViewById(R.id.tv_Object2);
        mMatches = (TextView) findViewById(R.id.tv_Matches);
        keypointsObject1 = keypointsObject2 = keypointMatches = -1;
        mTime = (TextView) findViewById(R.id.tv_Time);

        requestCameraPermission();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_load_first_image) {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, SELECT_PHOTO_1);
            return true;
        } else if (id == R.id.action_load_second_image) {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, SELECT_PHOTO_2);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch (requestCode) {
            case SELECT_PHOTO_1:
                if (resultCode == RESULT_OK) {
                    try {
                        final Uri imageUri = imageReturnedIntent.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        src1 = new Mat(selectedImage.getHeight(), selectedImage.getWidth(), CvType.CV_8UC4);
                        Utils.bitmapToMat(selectedImage, src1);
                        src1Selected = true;
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case SELECT_PHOTO_2:
                if (resultCode == RESULT_OK) {
                    try {
                        final Uri imageUri = imageReturnedIntent.getData();
                        final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        src2 = new Mat(selectedImage.getHeight(), selectedImage.getWidth(), CvType.CV_8UC4);
                        Utils.bitmapToMat(selectedImage, src2);
                        src2Selected = true;
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
        Toast.makeText(MainActivity.this, src1Selected + " " + src2Selected, Toast.LENGTH_SHORT).show();
        if (src1Selected && src2Selected) {
            Log.d("wq.chapter3", "Before Execute");
            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    mStartTime = System.currentTimeMillis();
                }

                @Override
                protected Bitmap doInBackground(Void... params) {

                    return executeTask();//目标检测（计算）
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    super.onPostExecute(bitmap);
                    mEndTiem = System.currentTimeMillis();
                    mImage1.setImageBitmap(bitmap);
                    mObject1.setText("目标1：" + keypointsObject1);
                    mObject2.setText("目标2：" + keypointsObject2);
                    mMatches.setText("关键点匹配：" + keypointMatches);
                    mTime.setText("耗费时间：" + (mEndTiem - mStartTime) + "ms");
                }
            }.execute();
        }
    }

    private Bitmap executeTask() {
        Log.d("wq.chapter3", "Execute");
        FeatureDetector detector;
        MatOfKeyPoint keypoints1, keypoints2;
        DescriptorExtractor descriptorExtractor;
        Mat descriptors1, descriptors2;
        DescriptorMatcher descriptorMatcher;
        MatOfDMatch matches = new MatOfDMatch();
        keypoints1 = new MatOfKeyPoint();
        keypoints2 = new MatOfKeyPoint();
        descriptors1 = new Mat();
        descriptors2 = new Mat();
        Log.d("wq.chapter3", "before switch");
        //特征匹配算法
        detector = FeatureDetector.create(FeatureDetector.ORB);
        descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        Log.d("wq.chapter3", "After switch");
        //检测关键点
        detector.detect(src2, keypoints2);
        detector.detect(src1, keypoints1);

        Log.d("wq.chapter3", CvType.typeToString(src1.type()) + " " + CvType.typeToString(src2.type()));
        Log.d("wq.chapter3", keypoints1.toArray().length + " keypoints");
        Log.d("wq.chapter3", keypoints2.toArray().length + " keypoints");
        Log.d("wq.chapter3", "Detect");

        //添加变量，用于显示关键点数量
        keypointsObject1 = keypoints1.toArray().length;
        keypointsObject2 = keypoints2.toArray().length;
        //计算描述子
        descriptorExtractor.compute(src1, keypoints1, descriptors1);
        descriptorExtractor.compute(src2, keypoints2, descriptors2);

        descriptorMatcher.match(descriptors1, descriptors2, matches);

        Log.d("wq.chapter3", matches.toArray().length + " matches");
        keypointMatches = matches.toArray().length;

        Collections.sort(matches.toList(), new Comparator<DMatch>() {
            @Override
            public int compare(DMatch o1, DMatch o2) {
                if (o1.distance < o2.distance)
                    return -1;
                if (o1.distance > o2.distance)
                    return 1;
                return 0;
            }
        });
        List<DMatch> listOfDMatch = matches.toList();
        if (listOfDMatch.size() > MAX_MATCHES) {
            matches.fromList(listOfDMatch.subList(0, MAX_MATCHES));
        }
        Mat src3 = drawMatches(src1, keypoints1, src2, keypoints2, matches, false);

        Log.d("wq.chapter3", CvType.typeToString(src3.type()));

        Bitmap image1 = Bitmap.createBitmap(src3.cols(), src3.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src3, image1);
        Imgproc.cvtColor(src3, src3, Imgproc.COLOR_BGR2RGB);
        boolean bool = Highgui.imwrite(Environment.getExternalStorageDirectory() + "/Download/Detection/" + ACTION_MODE + ".png", src3);
        Log.d("wq.chapter3", bool + " " + Environment.getExternalStorageDirectory() + "/Download/Detection/" + ACTION_MODE + ".png");
        return image1;
    }

    /**
     * @param img1
     * @param key1
     * @param img2
     * @param key2
     * @param matches
     * @param imageOnly
     * @return 绘制匹配结果-将检索图像和训练图像合并，并在同一幅图像中显示匹配结果。
     */
    static Mat drawMatches(Mat img1, MatOfKeyPoint key1, Mat img2, MatOfKeyPoint key2, MatOfDMatch matches, boolean imageOnly) {
        Mat out = new Mat();
        Mat im1 = new Mat();
        Mat im2 = new Mat();
        Imgproc.cvtColor(img1, im1, Imgproc.COLOR_BGR2RGB);
        Imgproc.cvtColor(img2, im2, Imgproc.COLOR_BGR2RGB);
        if (imageOnly) {
            MatOfDMatch emptyMatch = new MatOfDMatch();
            MatOfKeyPoint emptyKey1 = new MatOfKeyPoint();
            MatOfKeyPoint emptyKey2 = new MatOfKeyPoint();
            Features2d.drawMatches(im1, emptyKey1, im2, emptyKey2, emptyMatch, out);
        } else {
            Features2d.drawMatches(im1, key1, im2, key2, matches, out);
        }
        Bitmap bmp = Bitmap.createBitmap(out.cols(), out.rows(), Bitmap.Config.ARGB_8888);
        Imgproc.cvtColor(out, out, Imgproc.COLOR_BGR2RGB);
        Core.putText(out, "FRAME", new org.opencv.core.Point(im1.width()/2,30),Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0, 255, 255), 3);
        Core.putText(out, "MATCHED", new org.opencv.core.Point(im1.width()+im2.width()/2,30),Core.FONT_HERSHEY_PLAIN, 2, new Scalar(255, 0, 0), 3);
        return out;
    }

    protected static final int MSG_PREVIEW = 0;
    protected static final int MSG_RECORD = 1;




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
//        if (v == mBtnPreview) {
//
//            sendMsg(MSG_PREVIEW, "");
//        } else if (v == mBtnRecord){
//
//            sendMsg(MSG_RECORD, "");
//        }
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

                    break;


                case MSG_RECORD:
                    Log.i(TAG, "start record");

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
        Log.d(TAG, "onCreate method is onDestroy");
        super.onDestroy();
    }



    private boolean isOpenCVLoaded = false;
    @Override
    protected void onResume() {
        super.onResume();
//        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this, mOpenCVCallBack);

        //添加一下代码即可    （mLoaderCallback为BaseLoaderCallback对象）
        if((false == isOpenCVLoaded) && OpenCVLoader.initDebug()){
            mOpenCVCallBack.onManagerConnected( LoaderCallbackInterface.SUCCESS);
            isOpenCVLoaded = true;
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

                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };
}
