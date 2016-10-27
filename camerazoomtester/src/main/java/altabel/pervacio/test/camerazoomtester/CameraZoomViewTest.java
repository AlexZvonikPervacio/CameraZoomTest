package altabel.pervacio.test.camerazoomtester;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.hardware.Camera;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import altabel.pervacio.test.camerazoomtester.R;
import altabel.pervacio.test.camerazoomtester.constant.CameraType;
import altabel.pervacio.test.camerazoomtester.constant.ZoomNotSupportedReason;
import altabel.pervacio.test.camerazoomtester.listener.TestResultListener;


public class CameraZoomViewTest extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = CameraZoomViewTest.class.getSimpleName();

    /**
     * @hide
     */
    @IntDef({CameraType.BACK_CAMERA, CameraType.FRONT_CAMERA})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Cam {
    }

    private static final int INCREMENT_ZOOM_STEP = 2;
    private static final int DECREMENT_ZOOM_STEP = -2;
    private static final int ZOOM_GESTURE_MULTIPLIER = 100;

    private Camera.Size mPreviewSize;

    private int mCameraId;

    private TestResultListener mTestResultListener;

    private Camera mCamera;
    private SurfaceHolder previewHolder;

    private ScaleGestureDetector mScaleGestureDetector;
    private final ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener
            = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            changeZoom(Math.log(scaleGestureDetector.getScaleFactor()) * ZOOM_GESTURE_MULTIPLIER);
            return true;
        }
    };

    public CameraZoomViewTest(Context context) {
        super(context);
        initView(null);
        initCamera();
    }

    public CameraZoomViewTest(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(attrs);
        initCamera();
    }

    public CameraZoomViewTest(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(attrs);
    }

    private void initView(AttributeSet attrs) {
        TypedArray attributes = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.CameraType, 0, 0);
        try {
            setCameraId(attributes.getInt(R.styleable.CameraType_camera_type, Camera.CameraInfo.CAMERA_FACING_FRONT));
            initCamera();
        } finally {
            attributes.recycle();
        }
    }

    public void zoomIn() {
        changeZoom(INCREMENT_ZOOM_STEP);
    }

    public void zoomOut() {
        changeZoom(DECREMENT_ZOOM_STEP);
    }

    public void setTestResultListener(@Nullable final TestResultListener testResultListener) {
        mTestResultListener = testResultListener;
        initCamera();
    }

    public void setCameraId(int cameraId) {
        mCameraId = cameraId;
    }

    public void setCameraType(@Cam final int cameraType) {
        setCameraId(cameraType);
        initCamera();
    }

    public void initCamera() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            logMessage("CAMERA permission isn't granted");
            notifyTestFailure(ZoomNotSupportedReason.NO_PERMISSION);
        } else if (!checkCameraHardware(getContext())) {
            logMessage("This device doesn't have mCamera");
            notifyTestFailure(ZoomNotSupportedReason.NO_CAMERA);
        } else {
            mScaleGestureDetector = new ScaleGestureDetector(getContext(), mScaleGestureListener);
            previewHolder = this.getHolder();
            previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            previewHolder.addCallback(this);
            openCamera(mCameraId);
            requestLayout();
            mCamera.startPreview();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mScaleGestureDetector != null) {
            mScaleGestureDetector.onTouchEvent(event);
        }
        return true;
    }

    private boolean checkCameraHardware(@NonNull final Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    private void changeZoom(double extraZoom) {
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            if (params.isZoomSupported()) {
                int maxZoom = params.getMaxZoom();
                int currentZoom = params.getZoom();
                if (currentZoom + (int) extraZoom < maxZoom) {
                    params.setZoom(currentZoom + (int) extraZoom);
                } else if (currentZoom + (int) extraZoom < 0) {
                    params.setZoom(0);
                } else {
                    params.setZoom(maxZoom);
                }
                try {
                    mCamera.setParameters(params);
                } catch (RuntimeException e) {
                    Log.d(TAG, "unable set mCamera params: " + e.getLocalizedMessage());
                }
                if (currentZoom != mCamera.getParameters().getZoom()) {
                    notifyTestSucceed();
                }
            } else {
                logMessage("Zoom is not supported");
                notifyTestFailure(ZoomNotSupportedReason.NOT_SUPPORTED);
            }
        } else {
            logMessage("Can't access mCamera on this device");
            notifyTestFailure(ZoomNotSupportedReason.NO_ACCESS);
        }
    }

    private void openCamera(final int cameraId) {
        releaseCameraAndPreview();
        try {
            mCamera = Camera.open(getCameraType(cameraId));
            mCamera.setPreviewDisplay(previewHolder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releaseCameraAndPreview() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        openCamera(mCameraId);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if (mCamera != null) {
            mCamera.stopPreview();
            setCameraDisplayOrientation(getCameraType(mCameraId));
            setCameraPreviewSize(mPreviewSize);
            try {
                mCamera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }
    }

    private int getDisplayOrientation() {
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        return windowManager.getDefaultDisplay().getRotation();
    }


    private void setCameraDisplayOrientation(final int cameraId) {
        int degrees = 0;
        switch (getDisplayOrientation()) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = 0;

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        //back mCamera
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            result = ((360 - degrees) + info.orientation);
        } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {  //face mCamera
            result = ((360 - degrees) - info.orientation);
            result += 360;
        }
        result = result % 360;
        mCamera.setDisplayOrientation(result);
    }

    private void setCameraPreviewSize(final Camera.Size size) {
        if(mCamera != null && size != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(size.width, size.height);
            Log.i(TAG, "camera preview size: width: " + size.width + " height: " + size.height);
            mCamera.setParameters(parameters);
        }
    }

    private void notifyTestFailure(@NonNull final ZoomNotSupportedReason reason) {
        if (mTestResultListener != null) {
            mTestResultListener.onZoomNotSupported(reason);
        }
    }

    private void notifyTestSucceed() {
        if (mTestResultListener != null) {
            mTestResultListener.onZoomSupported();
        }
    }

    private int getCameraType(final int cameraId) {
        if (cameraId != Camera.CameraInfo.CAMERA_FACING_BACK && cameraId != Camera.CameraInfo.CAMERA_FACING_FRONT) {
            //return back camera as default
            return Camera.CameraInfo.CAMERA_FACING_BACK;
        } else {
            return cameraId;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        if (!getCameraPreviewSizes().isEmpty()) {
            mPreviewSize = getOptimalPreviewSize(getCameraPreviewSizes(), width, height);

            float ratio = mPreviewSize.height >= mPreviewSize.width
                    ? (float) mPreviewSize.height / (float) mPreviewSize.width
                    : (float) mPreviewSize.width / (float) mPreviewSize.height;


            float camHeight = (int) (width * ratio);
            float newHeightRatio;

            if (camHeight < height) {
                newHeightRatio = (float) height / (float) mPreviewSize.height;
                setMeasuredDimension((int) (width * newHeightRatio), (int) (newHeightRatio * camHeight));
            } else {
                setMeasuredDimension(width, (int) camHeight);
            }
        } else {
            setMeasuredDimension(width, height);
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double TOLERANCE_ASP = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null) {
            return null;
        }

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.height / size.width;
            if (Math.abs(ratio - targetRatio) > TOLERANCE_ASP) {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private List<Camera.Size> getCameraPreviewSizes() {
        if (mCamera != null) {
            return mCamera.getParameters().getSupportedPreviewSizes();
        } else {
            return new ArrayList<>();
        }
    }

    private void logMessage(final String message) {
        Log.i(TAG, message);
    }
}

