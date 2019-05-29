package com.st.faceplusplus.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.plattysoft.leonids.ParticleSystem;
import com.st.faceplusplus.R;
import com.st.faceplusplus.base.BaseActivity;
import com.st.faceplusplus.api.C;
import com.st.faceplusplus.api.Network;
import com.st.faceplusplus.utils.JLog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import es.dmoral.toasty.Toasty;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE_CAMERA_ADDFACE= 102;
    private static final int REQUEST_CODE_CAMERA_RECOGNIZE_FACE = 103;
    private static final int REQUEST_CODE_PHOTOS_ADDFACE = 104;
    private static final int REQUEST_CODE_PHOTOS_RECOGNIZE_FACE = 105;
    private Bitmap           faceBitmap;
    private String           targetFaceToken;
    private String           cameraPhotoPath;

    ImageView ivFaceTarget;
    ImageView ivFaceRecg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        findViewById(R.id.bt_add_face).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getImageByCamera(REQUEST_CODE_CAMERA_ADDFACE);
            }
        });
        findViewById(R.id.bt_recognize_face).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getImageByCamera(REQUEST_CODE_CAMERA_RECOGNIZE_FACE);
            }
        });
        findViewById(R.id.bt_clear_face).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearImages();
            }
        });
    }

    private void clearImages() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPer();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode){
                case REQUEST_CODE_CAMERA_ADDFACE:
                    String bitmapString1 = getBitmapStringAndShowImage(getCameraUri(),ivFaceTarget);
                    detectFace(bitmapString1);
                    break;
                case REQUEST_CODE_CAMERA_RECOGNIZE_FACE:
                    String bitmapString2 = getBitmapStringAndShowImage(getCameraUri(),ivFaceRecg);
                    detectFaceAndCompareFace(bitmapString2);
                    break;
                case REQUEST_CODE_PHOTOS_ADDFACE:
                    //getBitmapStringAndShowImage(data.getData(),ivFaceTarget);
                default:
            }
        }
    }

    private Uri getCameraUri(){
        File f = new File(cameraPhotoPath);
        return Uri.fromFile(f);
    }


    private String getBitmapStringAndShowImage(Uri uri, ImageView iv) {
        try {
            faceBitmap = scaleBitmapFormUri(uri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            faceBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            baos.flush();
            baos.close();
            String img64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            Log.d(TAG, "getBitmapStringAndShowImage: img64" + img64.length());
            Glide.with(this).load(baos.toByteArray()).into(iv);
            return img64;
        } catch (IOException e) {
            JLog.e(e);
        } finally {
            dismissLoading();
        }
        return null;
    }

    /**
     * 获得脸型的token
     * @param img64
     */
    private void detectFace(String img64) {
        Network.getFaceApi().detect(C.KEY, C.SECRET, 1, img64)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                new Consumer<DetectResp>() {
                    @Override
                    public void accept(DetectResp detectResp) throws Exception {
                        List<DetectResp.FacesBean> faces = detectResp.getFaces();
                        if (faces == null || faces.isEmpty()) {
                            Toasty.info(MainActivity.this,"抱歉，没找到脸").show();
                            return;
                        }
                        DetectResp.FacesBean face = faces.get(0);
                        targetFaceToken = face.getFace_token();
                        Toasty.info(MainActivity.this,"找到脸了：" + targetFaceToken).show();
                    }
                }
            );
    }

    // 获取第二张脸型token，并上传比较
    private void detectFaceAndCompareFace(String bitmapString) {
        Network.getFaceApi().detect(C.KEY, C.SECRET, 1, bitmapString)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map(
                new Function<DetectResp, String>() {
                    @Override
                    public String apply(DetectResp detectResp) throws Exception {
                        List<DetectResp.FacesBean> faces = detectResp.getFaces();
                        if (faces == null || faces.isEmpty()) {
                            Toasty.info(MainActivity.this, "没找到脸").show();
                            return null;
                        }
                        DetectResp.FacesBean face = faces.get(0);
                        String faceToken = face.getFace_token();
                        Log.d(TAG, "getBody: targetFaceToken = " + targetFaceToken);
                        Toasty.info(MainActivity.this, "找到脸：" + faceToken).show();
                        return faceToken;
                    }
                }
            )
            .observeOn(Schedulers.io())
            .subscribe(new Consumer<String>() {
                @Override
                public void accept(String faceToken) throws Exception {
                    Network.getFaceApi().compare(C.KEY, C.SECRET, targetFaceToken, faceToken)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            new Consumer<CompareResp>() {
                                @Override
                                public void accept(CompareResp compareResp) throws Exception {
                                    if (compareResp.getConfidence() > 70) {
                                        showFairy();
                                    } else {
                                        Toasty.info(MainActivity.this, "不是本人 分数:" + compareResp.getConfidence()).show();
                                    }
                                }
                            }
                        );
                }
            });
    }

    private void showFairy() {
        Toast.makeText(this, "确认是本人！！！", Toast.LENGTH_LONG).show();
        new ParticleSystem(this, 150, R.drawable.ic_heart, 5000)
                .setSpeedRange(0.1f, 0.15f)
                .setScaleRange(0.5f, 1f)
                .setRotationSpeed(180)
                .setFadeOut(4000)
                .oneShot(ivFaceRecg, 150);
    }

    private void getImage() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PHOTOS_ADDFACE);
    }

    private void getImageByCamera(int requestCode) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
                cameraPhotoPath = photoFile.getAbsolutePath();
            } catch (IOException ex) {
                JLog.e(ex);
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(getApplicationContext(),
                        getPackageName() + ".fileprovider",
                        photoFile);
//                向响应意图的所有应用授权使用空间，避免报错
                List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(intent, requestCode);
            }
        }
    }

    // 拍照文件起名
    public File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        return image;
    }

    private Bitmap scaleBitmapFormUri(Uri uri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (height > width) {
            double s = height / 400D;
            width /= s;
            height = 400;
        } else {
            double s = width / 400D;
            height /= s;
            width = 400;
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, false);
    }

    private void checkPer() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                ) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE
                            , Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    10086);
        }
    }

    private void initView() {
        ivFaceTarget = findViewById(R.id.iv_face_target);
        ivFaceRecg = findViewById(R.id.iv_face_recg);
    }

}
