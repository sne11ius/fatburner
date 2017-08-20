package de.wico.fatburner.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.EnvironmentalReverb;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

import com.sccomponents.gauges.ScLinearGauge;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import de.wico.fatburner.R;
import de.wico.fatburner.logging.AndroidLogger;

public class CapturePreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final AndroidLogger LOG = AndroidLogger.get(CapturePreview.class);

    private static int mCameraId = findCameraId();

    public Bitmap mBitmap;
    private Camera mCamera;
    boolean on = false;
    private Thread flickerThread = null;

    private Camera.Parameters mParameters;
    private ScLinearGauge progressBar;
    private MainActivity mainActivity;
    private boolean burningActive = false;

    public CapturePreview(Context context, AttributeSet attributes) {
        super(context, attributes);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera = Camera.open(mCameraId);
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            LOG.error("Could not set preview display", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mParameters = mCamera.getParameters();
        List<Camera.Size> previewSizes = mParameters.getSupportedPreviewSizes();
        int actualWidth = 0;
        int actualHeight = 0;
        LOG.debug("Surface size: " + width + "(w), " + height + "(h).");
        for (Camera.Size size : previewSizes) {
            if (size.width <= width && size.width > actualWidth) {
                actualWidth = size.width;
                actualHeight = size.height;
            }
        }
        LOG.debug("Setting size: " + actualWidth + "(w), " + actualHeight + "(h).");
        if (width > getHeight()) {
            LOG.debug("Horizontal");
        } else {
            LOG.debug("Vertical");
            int tmp = actualWidth;
            actualWidth = actualHeight;
            actualHeight = tmp;
        }
        mCamera.stopPreview();

        mCamera.setParameters(mParameters);
        setCameraDisplayOrientation((Activity) getContext(), mCamera);
        mCamera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
    }

    public void enableFlicker() {
        final ScLinearGauge progressBar = CapturePreview.this.progressBar;
        if (flickerThread != null) {
            return;
        }
        flickerThread = new Thread() {
            public void run() {
                final MediaPlayer myMediaPlayer = MediaPlayer.create(CapturePreview.this.getContext(), R.raw.violet_noise);
                try {
                    final Random random = new Random();
                    myMediaPlayer.start();
                    while (true) {
                        if (isInterrupted()) {
                            turnOff();
                            myMediaPlayer.stop();
                            return;
                        }
                        toggleFlashLight();
                        CapturePreview.this.mainActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (burningActive) {
                                    float oldHighValue = progressBar.getHighValue();
                                    progressBar.setHighValue(oldHighValue - random.nextFloat() * 3);
                                }
                            }
                        });
                        sleep(random.nextInt(250));
                        float volume = (0.5F + random.nextFloat()) % 1.F;
                        myMediaPlayer.setVolume(volume, volume);
                    }
                } catch (InterruptedException e) {
                    turnOff();
                    myMediaPlayer.stop();
                    resetProgress();
                } catch (Exception e) {
                    myMediaPlayer.stop();
                    LOG.error("Could not start flicker.", e);
                    resetProgress();
                }
            }
        };
        flickerThread.start();
    }

    public void disableFlicker() {
        turnOff();
        if (flickerThread != null) {
            flickerThread.interrupt();
            flickerThread = null;
        }
        resetProgress();
    }

    private void resetProgress() {
        CapturePreview.this.mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                burningActive = false;
                progressBar.setHighValue(100);
            }
        });
    }

    /** Turn the devices FlashLight on */
    public void turnOn() {
        if (mCamera != null) {
            // Turn on LED
            mParameters = mCamera.getParameters();
            mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(mParameters);

            on = true;
        }
    }

    /** Turn the devices FlashLight off */
    public void turnOff() {
        // Turn off flashlight
        if (mCamera != null) {
            mParameters = mCamera.getParameters();
            if (mParameters.getFlashMode().equals(mParameters.FLASH_MODE_TORCH)) {
                mParameters.setFlashMode(mParameters.FLASH_MODE_OFF);
                mCamera.setParameters(mParameters);
            }
        }
        on = false;
    }

    /** Toggle the flashlight on/off status */
    public void toggleFlashLight() {
        if (!on) { // Off, turn it on
            turnOn();
        } else { // On, turn it off
            turnOff();
        }
    }

    private static int findCameraId() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                LOG.debug("Camera found");
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    private static void setCameraDisplayOrientation(Activity activity, Camera camera) {

        Camera.CameraInfo info = new Camera.CameraInfo();

        Camera.getCameraInfo(mCameraId, info);

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    void setProgressBar(ScLinearGauge progressBar) {
        this.progressBar = progressBar;
    }

    public void activateBurnMode() {
        burningActive = true;
    }

}
