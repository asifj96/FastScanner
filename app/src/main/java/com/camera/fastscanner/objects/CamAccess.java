package com.camera.fastscanner.objects;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.ScaleGestureDetector;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.UseCase;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.camera.fastscanner.Utility.Utils;
import com.camera.fastscanner.callbacks.Availability;
import com.camera.fastscanner.callbacks.CameraFailureCallback;
import com.camera.fastscanner.callbacks.CameraShutdownCallback;
import com.camera.fastscanner.callbacks.SetTouchListenerCallback;
import com.camera.fastscanner.callbacks.UseCaseCreator;
import com.camera.fastscanner.exception.NoCameraException;
import com.camera.fastscanner.exception.ReferenceInvalidException;
import com.google.common.util.concurrent.ListenableFuture;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;

public class CamAccess {
    private @Availability
    int mFlash;
    private final WeakReference<Context> wctx;
    private final boolean mHasCamera;
    private final UseCaseCreator mUseCaseCreator;
    private CameraControl cameraControl;

    public CamAccess(final WeakReference<Context> wctx, final UseCaseCreator useCaseCreator) throws NoCameraException {

        this.wctx = wctx;
        assert wctx.get() != null;

        if (!wctx.get().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            mHasCamera = false;
            throw new NoCameraException("No camera available");
        } else {
            mHasCamera = true;
        }


        if (wctx.get().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            mFlash = Availability.OFF;
        } else {
            mFlash = Availability.UNAVAILABLE;
        }

        mUseCaseCreator = useCaseCreator;


    }


    @SuppressLint("ClickableViewAccessibility")
    public CameraShutdownCallback startCamera(LifecycleOwner lifecycleOwner, CameraFailureCallback cameraFailureCallback, SetTouchListenerCallback setTouchListenerCallback) throws ReferenceInvalidException {
        if (wctx.get() == null) {
            throw new ReferenceInvalidException("Weak reference to context is null.");
        }
        final ListenableFuture<ProcessCameraProvider> cpf = ProcessCameraProvider.getInstance(wctx.get().getApplicationContext());


        cpf.addListener(() -> {

            // Retrieving the Camera Provider
            ProcessCameraProvider cp;
            try {
                cp = cpf.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                cp = null;
            }


            // Create the CameraX use cases with the supplied function
            UseCase[] useCases = mUseCaseCreator.create();
            CameraSelector cs = CameraSelector.DEFAULT_BACK_CAMERA;


            try {
                if (cp == null) {
                    throw new NullPointerException("Camera Provider Unavailable");
                }

                cp.unbindAll();
                Camera camera = cp.bindToLifecycle(lifecycleOwner, cs, useCases[0], useCases[1]);
                cameraControl = camera.getCameraControl();


                ScaleGestureDetector.SimpleOnScaleGestureListener listener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float currentZoomRatio;
                        if (camera.getCameraInfo().getZoomState().getValue() != null) {
                            currentZoomRatio = camera.getCameraInfo().getZoomState().getValue().getZoomRatio();
                        } else {
                            currentZoomRatio = 0F;
                        }
                        float delta = detector.getScaleFactor();

                        cameraControl.setZoomRatio(currentZoomRatio * delta);

                        return true;
                    }
                };
                // You can't return the listener or detector as this is an a‌‍‌‌‍‍‌‌‍‍‌‌‍‍‌‌‍‍‌‍‍‌‌‌‍‍‌‌‍‍‌‌‌‍‌‌‍‍‌‌‍‌‌‌‍‍‌‌‌‍‌‍‍‌‌‍‌‌‌‍‍‌‌‌‌‍‌‍‍‌‌‌‌‍‌‍‍‌‌‌‍‍‌‌‍‍‌‍‍‌‌‌‍‍‌‌‍‌‌‌‍‍‌‌‌‍‌‍‍‌‌‍‌‌‌‍‍‌‌‌‌‍‌‌‍‍‍‌‌‌‌‌‍‍‌‍‌‌‌‍‍‌‌‌‍‌‌‌‍‍‌‌‌‍‌‍‍‌‌‌‌‍‌‌‍‍‌‍‌‌‌‍‍‌‌‍‍‌‌‌‍‍‌‌‍‍‌‌‍‍‌‌‌‍‌‌‍‍‍‌‌‍‌‌‍‍‌‌‌‌‌‌‍‍‌‍‌‍‌‌‍‍‌‍‍‌‌‍‍‌‌‌‍‍‌‌‍‍‌‌‍‍‌‌‍‍‌‍‍‌‌‍‍‌‌‍‍‌‌‌‍‍‌‍‍‍‌‌‍‍‍‌‌‌‌‌‍‍‌‌‌‍‌‌‍‍‌‌‌‌‌‌‍‍‍‌‌‍‌‌‍‍‌‍‍‌‌‍‍‌‌‌‌‍‌‌‍‍‌‌‍‍‌‍‍‌‌‌‍‌‌‍‍‌‌‍‍‌‌‌‍‍‌‌‍‍‌‌‍‍‍‌‌‍‌‌‍‍‌‍‌‍‌‍‍‌‌‌‍‌‌‌‍‍‌‌‌‌‌‌‍‍‌‍‍‌‌‌‍‍‌‍‍‌‌‌‍‍‌‌‍‍‌‌‍‍‌‌‌‌‌‍‍‌‌‌‍‌‌‌‍‍‌‍‌‌‌‌‍‍‌‍‍‍‌‍‍‌‌‌‍‌‌‌‍‍‍‌‌‌‌‌‍‍‌‍‌‍‌‌‍‍‌‍‌‍‌‍‍‌‌‌‍‌‌‍‍‌‌‍‌‍‌‌‍‍‌‌‌‍‌‍‍‌‌‍‌‌‌‌‍‍‌‌‌‍‌‌‍‍‍‌‌‍synchronous method.
                // The method would have probably returned before this async call finished.
                ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(wctx.get().getApplicationContext(), listener);


                // Set touch listener for pre‌‍‌‌‍‍‌‌‍‍‌‌‍‍‌‌‍‍‌‍‍‌‌‌‍‍‌‌‍‍‌‌‌‍‌‌‍‍‌‌‍‌‌‌‍‍‌‌‌‍‌‍‍‌‌‍‌‌‌‍‍‌‌‌‌‍‌‍‍‌‌‌‌‍‌‍‍‌‌‌‍‍‌‌‍‍‌‍‍‌‌‌‍‍‌‌‍‌‌‌‍‍‌‌‌‍‌‍‍‌‌‍‌‌‌‍‍‌‌‌‌‍‌‌‍‍‍‌‌‌‌‌‍‍‌‍‌‌‌‍‍‌‌‌‍‌‌‌‍‍‌‌‌‍‌‍‍‌‌‌‌‍‌‌‍‍‌‍‌‌‌‍‍‌‌‍‍‌‌‌‍‍‌‌‍‍‌‌‍‍‌‌‌‍‌‌‍‍‍‌‌‍‌‌‍‍‌‌‌‌‌‌‍‍‌‍‌‍‌‌‍‍‌‍‍‌‌‍‍‌‌‌‍‍‌‌‍‍‌‌‍‍‌‌‍‍‌‍‍‌‌‍‍‌‌‍‍‌‌‌‍‍‌‍‍‍‌‌‍‍‍‌‌‌‌‌‍‍‌‌‌‍‌‌‍‍‌‌‌‌‌‌‍‍‍‌‌‍‌‌‍‍‌‍‍‌‌‍‍‌‌‌‌‍‌‌‍‍‌‌‍‍‌‍‍‌‌‌‍‌‌‍‍‌‌‍‍‌‌‌‍‍‌‌‍‍‌‌‍‍‍‌‌‍‌‌‍‍‌‍‌‍‌‍‍‌‌‌‍‌‌‌‍‍‌‌‌‌‌‌‍‍‌‍‍‌‌‌‍‍‌‍‍‌‌‌‍‍‌‌‍‍‌‌‍‍‌‌‌‌‌‍‍‌‌‌‍‌‌‌‍‍‌‍‌‌‌‌‍‍‌‍‍‍‌‍‍‌‌‌‍‌‌‌‍‍‍‌‌‌‌‌‍‍‌‍‌‍‌‌‍‍‌‍‌‍‌‍‍‌‌‌‍‌‌‍‍‌‌‍‌‍‌‌‍‍‌‌‌‍‌‍‍‌‌‍‌‌‌‌‍‍‌‌‌‍‌‌‍‍‍‌‌‍view view to accept zoom.
                setTouchListenerCallback.setTouchListener((view, motionEvent) -> {
                    scaleGestureDetector.onTouchEvent(motionEvent);
                    return true;
                });


            } catch (Exception e) {
                cameraFailureCallback.run(e);
                e.printStackTrace();
            }


        }, ContextCompat.getMainExecutor(wctx.get()));

        // EXPM possible leak of context here? ("cpf" was created with wctx.get())
        @SuppressLint("RestrictedApi") CameraShutdownCallback shutdownCallback = () -> {
            ProcessCameraProvider pcp;
            try {
                pcp = cpf.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                return; // the ProcessCameraProvider cannot be obtained, hence we just skip the shutdown process
            }
            pcp.unbindAll();
            pcp.shutdown();
            // Shutdown finished.
        };

        return shutdownCallback;

    }

    public void toggleFlash(@Availability int state) {
        if (state == Availability.UNAVAILABLE) {
            throw new RuntimeException("This must be an error in the code. Please do not set flash state to unavailable. Contact the app developers immediately if you see this message.");
        } else {
            if (cameraControl != null) {
                mFlash = state;
                cameraControl.enableTorch(Utils.availabilityToBoolean(state));
            }
        }
    }

    public @Availability
    int getFlash() {
        return mFlash;
    }


    public boolean hasCamera() {
        return mHasCamera;
    }
}
