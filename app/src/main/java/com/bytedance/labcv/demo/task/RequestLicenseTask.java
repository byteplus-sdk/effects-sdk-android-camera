package com.bytedance.labcv.demo.task;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.AsyncTask;

import com.bytedance.labcv.core.effect.EffectResourceHelper;
import com.bytedance.labcv.core.license.EffectLicenseHelper;
import com.bytedance.labcv.core.license.EffectLicenseProvider;
import com.bytedance.labcv.effectsdk.RenderManager;

import java.lang.ref.WeakReference;

/**
 * Created on 2019-07-20 13:05
 */
public class RequestLicenseTask extends AsyncTask<String, Void, Boolean> {
    public interface ILicenseViewCallback {
        Context getContext();
        void onStartTask();
        void onEndTask(boolean result);
    }

    private WeakReference<ILicenseViewCallback> mCallback;

    public RequestLicenseTask(ILicenseViewCallback callback) {
        mCallback = new WeakReference<>(callback);
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        ILicenseViewCallback callback = mCallback.get();
        if (callback == null) return false;

        try {
            EffectResourceHelper resourceHelper = new EffectResourceHelper(callback.getContext());
            EffectLicenseHelper licenseHelper = EffectLicenseHelper.getInstance(callback.getContext());
            ActivityManager am = (ActivityManager) callback.getContext().getSystemService(callback.getContext().ACTIVITY_SERVICE);
            ConfigurationInfo ci = am.getDeviceConfigurationInfo();
            int renderapi = (ci.reqGlEsVersion >= 0x30000)?1:0;

            String filePath = licenseHelper.getLicensePath();

            RenderManager renderManager = new RenderManager();
            int ret = renderManager.init(callback.getContext(), resourceHelper.getModelPath(), filePath, true,
                    licenseHelper.getLicenseMode() == EffectLicenseProvider.LICENSE_MODE_ENUM.ONLINE_LICENSE, renderapi);
            if (ret != 0 && ret != -11 && ret != 1 && licenseHelper.getLicenseMode() == EffectLicenseProvider.LICENSE_MODE_ENUM.ONLINE_LICENSE) {
                filePath = licenseHelper.updateLicensePath();
            }

            return licenseHelper.getLastErrorCode() == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onPreExecute() {
        ILicenseViewCallback callback = mCallback.get();
        if (callback == null) return;
        callback.onStartTask();
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        ILicenseViewCallback callback = mCallback.get();
        if (callback == null) return;
        callback.onEndTask(result);
    }
}
