package altabel.pervacio.test.testcamerazoom;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import altabel.pervacio.test.camerazoomtester.constant.ZoomNotSupportedReason;
import altabel.pervacio.test.camerazoomtester.listener.TestResultListener;
import altabel.pervacio.test.camerazoomtester.CameraZoomViewTest;

public class MainActivity extends AppCompatActivity implements TestResultListener, View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int PERMISSION_REQUEST_CODE = 32 << 4;

    private Button mZoomInBtn;
    private Button mZoomOutBtn;

    private TextView mCameraStatusTv;
    private CameraZoomViewTest mCameraPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraStatusTv = (TextView) findViewById(R.id.camera_status);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mZoomInBtn = (Button) findViewById(R.id.btnZoomIn);
        mZoomInBtn.setOnClickListener(this);
        mZoomOutBtn = (Button) findViewById(R.id.btnZoomOut);
        mZoomOutBtn.setOnClickListener(this);

        mCameraPreview = (CameraZoomViewTest) findViewById(R.id.custom_preview);
        mCameraPreview.setTestResultListener(this);
    }

    @Override
    public void onZoomSupported() {
        setCameraStatusText(getString(R.string.camera_zoom_supported));
    }

    @Override
    public void onZoomNotSupported(ZoomNotSupportedReason reason) {
        switch (reason) {
            case NO_CAMERA:
                setCameraStatusText(getString(R.string.camera_not_supported));
                break;
            case NO_PERMISSION:
                setCameraStatusText(getString(R.string.need_camera_permission));
                requestPermissionWithRationale();
                break;
            case NOT_SUPPORTED:
                setCameraStatusText(getString(R.string.camera_zoom_not_supported));
                break;
            case NO_ACCESS:
                setCameraStatusText(getString(R.string.cant_access_camera));
                break;
        }
    }

    private void setCameraStatusText(final String status) {
        mCameraStatusTv.setText(status);
        mCameraStatusTv.setVisibility(View.VISIBLE);
        Log.i(TAG, status);
    }

    //Check if permission granted
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //notify library that permission had been granted
                mCameraPreview.initCamera();
            } else {
                showNoCameraPermissionSnackbar();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Make an attempt to request camera when getting back to this activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            //notify library that permission had been granted
            mCameraPreview.initCamera();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    // request permsission for camera
    public void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.CAMERA
                },
                PERMISSION_REQUEST_CODE);
    }

    public void requestPermissionWithRationale() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Snackbar.make(findViewById(android.R.id.content), R.string.need_camera_permission, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.snackbar_action_grant, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            requestCameraPermission();
                        }
                    })
                    .show();
        } else {
            requestCameraPermission();
        }
    }

    public void showNoCameraPermissionSnackbar() {
        Snackbar.make(findViewById(android.R.id.content), R.string.permission_not_granted, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_action_settings, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openApplicationSettings();
                        Toast.makeText(getApplicationContext(), R.string.open_permission, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    public void openApplicationSettings() {
        Intent appSettingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(appSettingsIntent, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnZoomIn:
                mCameraPreview.zoomIn();
                break;
            case R.id.btnZoomOut:
                mCameraPreview.zoomOut();
                break;
        }
    }
}
