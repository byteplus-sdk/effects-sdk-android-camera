package com.bytedance.labcv.demo;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.opengl.GLSurfaceView.RENDERMODE_WHEN_DIRTY;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.util.Size;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bef.effectsdk.OpenGLUtils;
import com.bytedance.labcv.core.effect.EffectManager;
import com.bytedance.labcv.core.effect.EffectResourceHelper;
import com.bytedance.labcv.core.license.EffectLicenseHelper;
import com.bytedance.labcv.core.util.ImageUtil;
import com.bytedance.labcv.core.util.LogUtils;
import com.bytedance.labcv.demo.databinding.FragmentCameraBinding;
import com.bytedance.labcv.effectsdk.BytedEffectConstants;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class CameraFragment extends Fragment
        implements GLSurfaceView.Renderer,
        SurfaceTexture.OnFrameAvailableListener,
        EffectManager.OnEffectListener{

    private FragmentCameraBinding binding;
    private GLSurfaceView mSurfaceView;
    private int mTextureId;
    private SurfaceTexture mSurfaceTexture;
    private int mTextureWidth;
    private int mTextureHeight;
    private int mCameraRotation;
    private Context context;
    private CameraUtil cameraUtil;

    private EffectManager mEffectManager;
    private ImageUtil mImageUtil;
    private Boolean effectsOn;

    private ImageView mDebugWindow;

    // permission request with activityResultLauncher (for Android X)
    private ActivityResultLauncher<String> activityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // camera permission is granted. Open camera now
                    cameraUtil.openCamera();
                    binding.openCamButton.setVisibility(View.GONE); // hide the button after camera is on
                } else {
                    // camera permission is not granted. Do nothing here
                }
            });

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // init
        context = this.getContext();
        // initialize a camera util
        cameraUtil = new CameraUtil();
        mImageUtil = new ImageUtil();
        effectsOn = false;
        cameraUtil.setCameraContext(context);
        binding = FragmentCameraBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDebugWindow = binding.debugWindow;
        // set up GL Surface View for rendering camera preview
        initGLSurfaceView();

        // open camera upon button click
        binding.openCamButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                // request for camera permission and open camera
                activityResultLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        // toggle effects upon button click
        binding.toggleEffectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                effectsOn = !effectsOn;
            }
        });
    }

    @Override
    public void onResume() {

        super.onResume();
        cameraUtil.startBackgroundThread();
    }


    @Override
    public void onPause() {

        cameraUtil.stopBackgroundThread();

        super.onPause();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ------------------------------- Util functions ------------------------------------------
    private void initGLSurfaceView(){
        mSurfaceView = binding.glview;
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        ConfigurationInfo ci = am.getDeviceConfigurationInfo();
        if(ci.reqGlEsVersion >= 0x30000)
        {
            mSurfaceView.setEGLContextClientVersion(3);
        }
        else
        {
            mSurfaceView.setEGLContextClientVersion(2);
        }

        // set GL Surface View listener
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(RENDERMODE_WHEN_DIRTY);

    }


    // ------------------------------- Listeners ------------------------------------------

    /**
     * Listeners for GLSurfaceView
     * */

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        System.out.println("GLSurfaceView is created");

        // get surface texture to render
        mTextureId = OpenGLUtils.getExternalOESTextureID();
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mSurfaceTexture.setOnFrameAvailableListener(this);


        // init Effects SDK Manager
        mEffectManager = new EffectManager(context, new EffectResourceHelper(context), EffectLicenseHelper.getInstance(context));
        mEffectManager.setOnEffectListener(this);
        int ret = mEffectManager.init();
        if (ret != BytedEffectConstants.BytedResultCode.BEF_RESULT_SUC){
            LogUtils.e("mEffectManager.init() fail!! error code ="+ret);
        }


        // set up camera preview as rendering source
        cameraUtil.setSurfaceTexture(mSurfaceTexture);

        // set up front camera
        cameraUtil.setupFrontCamera(mSurfaceView);
        // turn on camera immediately
        cameraUtil.initCameraStateCallback();
        // get camera rotation angle
        mCameraRotation = cameraUtil.getCameraRotation();
        // try open camera
        // cameraUtil.openCamera();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int i, int i1) {

    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        // {zh} 清空缓冲区颜色 {en} Clear buffer color
        //Clear buffer color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Update the texture image to the most recent frame from the image stream
        mSurfaceTexture.updateTexImage();

        // get current timestamp
        long timestamp = System.currentTimeMillis();

        // initialize output texture as the same as the camera preview texture
        int dstTexture = mTextureId;

        // get camera preview texture width and height, based on rotation
        mTextureWidth = cameraUtil.getPreviewSize().getWidth();
        mTextureHeight = cameraUtil.getPreviewSize().getHeight();

        // check rotation
        if (mCameraRotation % 180 == 90){
            mTextureWidth =  cameraUtil.getPreviewSize().getHeight();
            mTextureHeight = cameraUtil.getPreviewSize().getWidth();
        }

        // Convert the input texture to a 2D texture (0 rotation, front camera mirror view)
//        ImageUtil.Transition transition = new ImageUtil.Transition()
//                .rotate(mCameraRotation).flip(false, true).reverse();
        ImageUtil.Transition transition = new ImageUtil.Transition()
                .rotate(mCameraRotation).flip(false, true);


        // Note: the width and height here need to be original from the camera preview (regardless of rotation)
        int texture2D = mImageUtil.transferTextureToTexture(mTextureId,
                BytedEffectConstants.TextureFormat.Texture_Oes, BytedEffectConstants.TextureFormat.Texure2D,
                cameraUtil.getPreviewSize().getWidth(), cameraUtil.getPreviewSize().getHeight(), transition);


        // get camera orientation for Effects
        BytedEffectConstants.Rotation rotation = BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_0;
        switch(mCameraRotation){
            case 0:
                rotation = BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_0;
                break;
            case 90:
                rotation = BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_90;
                break;
            case 180:
                rotation = BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_180;
                break;
            case 270:
                rotation = BytedEffectConstants.Rotation.CLOCKWISE_ROTATE_270;
                break;
        }

        // Prepare frame buffer texture object
        dstTexture = mImageUtil.prepareTexture(mTextureWidth, mTextureHeight);

        // {zh} 调试代码，用于显示输入图        {en} Debugging code for displaying input diagrams
//        Bitmap b = mImageUtil.transferTextureToBitmap( texture2D, BytedEffectConstants.TextureFormat.Texure2D, mTextureWidth, mTextureHeight);
//        this.getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                mDebugWindow.setImageBitmap(b);
//            }
//        });


        // set camera position for EffectManager
        mEffectManager.setCameraPosition(true);
        // Conduct special effects operation, output texture is 2D texture with upright face
        boolean ret = mEffectManager.process(texture2D, dstTexture, mTextureWidth, mTextureHeight, rotation, timestamp);

        if(!ret){
            // revert back to original texture
            dstTexture = mTextureId;
        }

        if(effectsOn){
            // set beauty effects
            mEffectManager.setComposeNodes(new String[]{"lip/lite/fuguhong"});
            mEffectManager.updateComposerNodeIntensity("lip/lite/fuguhong", "Internal_Makeup_Lips", 0.8f);

            // set sticker
            mEffectManager.setSticker("stickers/ar/glasses_0");

            // apply a fliter
            mEffectManager.setFilter("Filter_02_14");
            mEffectManager.updateFilterIntensity(0.8f);
        } else {
            dstTexture = texture2D;
        }

        // Draw on screen
        // mDrawer.draw(mTextureId, true);
        if (!GLES20.glIsTexture(dstTexture)){
            LogUtils.e("output texture not a valid texture");
            return;
        }


        // Note: rotation is already handled above, and we can treat it as 0 here for drawing
        ImageUtil.Transition drawTransition = new ImageUtil.Transition()
                                                .crop( ImageView.ScaleType.CENTER_CROP,
                                                    0,
                                                    mTextureWidth, mTextureHeight,
                                                    mSurfaceView.getWidth(), mSurfaceView.getHeight());

        mImageUtil.drawFrameOnScreen( dstTexture,
                BytedEffectConstants.TextureFormat.Texure2D,
                mSurfaceView.getWidth(), mSurfaceView.getHeight(),
                drawTransition.getMatrix());


    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mSurfaceView.requestRender();
    }

    @Override
    public void onEffectInitialized() {

    }

}