package com.meetme.cameracolors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * Show the camera preview, along with UI components for taking a picture,
 * switching cameras, and (maybe) recording video
 *
 * @author bherbert
 *
 */
public class MainActivity extends FragmentActivity implements CameraFragment.CameraFragmentListener {
    public static final String TAG = "ReactionActivity";
    public static final String EXTRA_PROFILE_PHOTO = "com.meetme.android.fuzzy.EXTRA_PROFILE_PHOTO";

    public static final String PHOTO_URL = "com.meetme.reaction.PHOTO_URL";

    public CameraFragment cameraFragment;

    Button btnCreate;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!hasCamera(getPackageManager())) {
            Toast.makeText(this, "No camera hardware!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_camera);

        cameraFragment = (CameraFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_camera);

        btnCreate = (Button) findViewById(R.id.btn_create);

        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CreateActivity.class));
                finish();
            }
        });
    }

    @SuppressLint("InlinedApi")
    public boolean hasCamera(PackageManager pm) {
        // check for any camera if our SDK version allows; otherwise, we need to
        // explicitly check
        // for both back and front cameras
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        }

        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) || pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    @Override
    public void onCameraFailed() {
        Log.e(TAG, "Unable to obtain camera; it may be in use.");
        // TODO: Crashlytics?

        Toast.makeText(this, "camera inaccessible", Toast.LENGTH_LONG).show();

        startActivity(new Intent(MainActivity.this, CreateActivity.class));
        return;
    }


    public static Intent createIntent(Context context, String photoUrl) {
        // TODO save off url and whatnot
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(PHOTO_URL, photoUrl);
        return intent;
    }
}