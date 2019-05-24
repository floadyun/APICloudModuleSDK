package com.qll.arcface;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.qll.arcface.activity.RegisterAndRecognizeActivity;
import com.qll.arcface.common.Constants;
import com.qll.arcface.faceserver.FaceServer;
import com.qll.arcface.util.ImageUtil;
import com.uzmap.pkg.uzcore.UZWebView;
import com.uzmap.pkg.uzcore.uzmodule.UZModule;
import com.uzmap.pkg.uzcore.uzmodule.UZModuleContext;
import org.json.JSONException;
import org.json.JSONObject;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * @copyright : 深圳市喜投金融服务有限公司
 * Created by yixf on 2019/5/22
 * @description:
 */
public class ArcfaceApi extends UZModule {
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE
    };
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;

    public ArcfaceApi(UZWebView webView) {
        super(webView);

        //本地人脸库初始化
        FaceServer.getInstance().init(context());
    }
    @Override
    protected void onClean() {
        super.onClean();
        FaceServer.getInstance().unInit();
    }
    /**
     * 激活
     * @param moduleContext
     */
    public void jsmethod_active(final UZModuleContext moduleContext){
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions((Activity) context(), NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
            return;
        }
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                FaceEngine faceEngine = new FaceEngine();
                int activeCode = faceEngine.active(context(), Constants.APP_ID, Constants.SDK_KEY);
                emitter.onNext(activeCode);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }
                    @Override
                    public void onNext(Integer activeCode) {
                        JSONObject ret = new JSONObject();
                        if (activeCode == ErrorInfo.MOK) {//激活成功
                            try {
                                ret.put("status", true);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {//已激活
                            try {
                                ret.put("code", ErrorInfo.MERR_ASF_ALREADY_ACTIVATED);
                                ret.put("msg",context().getResources().getString(R.string.already_activated));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
//                            showToast(context().getResources().getString(R.string.already_activated));
                        } else {//激活失败
                            try {
                                ret.put("code", ErrorInfo.MERR_ASF_ALREADY_ACTIVATED);
                                ret.put("msg",context().getResources().getString(R.string.active_failed));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
//                            showToast(context().getResources().getString(R.string.active_failed, activeCode));
                        }
                        moduleContext.success(ret, true);
                    }
                    @Override
                    public void onError(Throwable e) {

                    }
                    @Override
                    public void onComplete() {

                    }
                });
    }
    /**
     * 注册
     * @param moduleContext
     */
    public void jsmethod_register(final UZModuleContext moduleContext){
        startActivity(new Intent(context(), RegisterAndRecognizeActivity.class));
    }
    /**
     * 打开注册界面
     * @param moduleContext
     */
    public void jsmethod_openRegister(final UZModuleContext moduleContext){

    }
    /**
     * 关闭自定义注册界面
     * @param moduleContext
     */
    public void jsmethod_closeRegister(final UZModuleContext moduleContext){

    }
    /**
     * 在本地添加注册用户
     * @param moduleContext
     */
    public void jsmethod_addUser(final UZModuleContext moduleContext){
        String id = moduleContext.optString("id");
        String name = moduleContext.optString("name");
        String gender = moduleContext.optString("gender");
        String ext = moduleContext.optString("ext");
        String faceFeature = moduleContext.optString("faceFeature");
        String faceImage = moduleContext.optString("faceImage");
        SimpleTarget simpleTarget = new SimpleTarget<Bitmap>(){
            @Override
            public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                byte[] nv21 = ImageUtil.bitmapToNv21(bitmap, bitmap.getWidth(), bitmap.getHeight());
                boolean success = FaceServer.getInstance().register(context(), nv21, bitmap.getWidth(), bitmap.getHeight(),
                        System.currentTimeMillis()+"");
            }
        };
        Glide.with(context()).asBitmap().load(faceImage).into(simpleTarget);
    }
    /**
     * 获取注册用户数
     * @param moduleContext
     */
    public void jsmethod_countUser(final UZModuleContext moduleContext){
        JSONObject ret = new JSONObject();
        try {
            ret.put("status", true);
            ret.put("count",FaceServer.getInstance().getFaceNumber(context()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        moduleContext.success(ret, true);
    }

    /**
     * 获取本地注册用户
     * @param moduleContext
     */
    public void jsmethod_getUser(final UZModuleContext moduleContext){
//        FaceServer.getInstance().
        JSONObject ret = new JSONObject();
        try {
            ret.put("status", true);
            ret.put("count",FaceServer.getInstance().getFaceNumber(context()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        moduleContext.success(ret, true);
    }
    /**
     * 清空本地注册用户
     * @param moduleContext
     */
    public void jsmethod_clearAllUser(final UZModuleContext moduleContext){
        FaceServer.getInstance().clearAllFaces(context());
        JSONObject ret = new JSONObject();
        try {
            ret.put("status", true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        moduleContext.success(ret, true);
    }
    /**
     * 删除指定用户
     * @param moduleContext
     */
    public void jsmethod_removeUser(final UZModuleContext moduleContext){
        FaceServer.getInstance().clearAllFaces(context());
        JSONObject ret = new JSONObject();
        try {
            ret.put("status", true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        moduleContext.success(ret, true);
    }

    /**
     * 获取所有用户
     * @param moduleContext
     */
    public void jsmethod_listUser(final UZModuleContext moduleContext){

    }

    /**
     * 人脸对比
     * @param moduleContext
     */
    public void jsmethod_compare(final UZModuleContext moduleContext){
//        FaceServer.getInstance().
    }

    /**
     * 打开人脸对比界面
     * @param moduleContext
     */
    public void jsmethod_openCompare(final UZModuleContext moduleContext){

    }
    /**
     * 关闭人脸对比界面
     * @param moduleContext
     */
    public void jsmethod_closeCompare(final UZModuleContext moduleContext){

    }
    /**
     * 人脸搜索
     * @param moduleContext
     */
    public void jsmethod_search(final UZModuleContext moduleContext){

    }
    /**
     * 自定义人脸搜索界面
     * @param moduleContext
     */
    public void jsmethod_openSearch(final UZModuleContext moduleContext){

    }
    /**
     * 关闭搜索界面
     * @param moduleContext
     */
    public void jsmethod_closeSearch(final UZModuleContext moduleContext){

    }
    /**
     * 检查权限
     * @param neededPermissions
     * @return
     */
    private boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= ContextCompat.checkSelfPermission(context(), neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }
    /**
     * 显示提示信息
     * @param msg
     */
    private void showToast(String msg){
        Toast.makeText(context(),msg,Toast.LENGTH_LONG).show();
    }
}
