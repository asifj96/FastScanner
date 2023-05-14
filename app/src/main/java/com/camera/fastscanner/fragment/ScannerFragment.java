package com.camera.fastscanner.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCase;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.camera.fastscanner.R;
import com.camera.fastscanner.callbacks.CameraFailureCallback;
import com.camera.fastscanner.callbacks.CameraShutdownCallback;
import com.camera.fastscanner.callbacks.SetTouchListenerCallback;
import com.camera.fastscanner.callbacks.UseCaseCreator;
import com.camera.fastscanner.databinding.FragmentScannerBinding;
import com.camera.fastscanner.exception.NoCameraException;
import com.camera.fastscanner.exception.ReferenceInvalidException;
import com.camera.fastscanner.objects.CamAccess;
import com.camera.fastscanner.viewmodel.ScanViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScannerFragment extends Fragment {

    private final String cameraPermission = Manifest.permission.CAMERA;
    private CamAccess camAccessObj;
    private CameraShutdownCallback cameraShutdownCallback;
    private ExecutorService camExecutor;
    private final static String TAG = "ScannerFragment";
    private ScanViewModel vm;
    private static final int CAMERA_REQUEST_CODE = 1000;
    private FragmentScannerBinding binding;

    public ScannerFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentScannerBinding.inflate(getLayoutInflater());

        // Camera Use Cases:
        UseCaseCreator useCaseCreator = () -> {

            // Use Case 1: Preview
            Preview preview = new Preview.Builder().build();
            if (binding != null && binding.previewView != null) {
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());
            }

            // Use case 2: Barcode analysis
            ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();

            imageAnalysis.setAnalyzer(camExecutor, vm.getCodeAnalyser());

            return new UseCase[]{preview, imageAnalysis};
        };

        try {
            camAccessObj = new CamAccess(new WeakReference<>(getContext()), useCaseCreator);
        } catch (NoCameraException e) {
            noCamera();
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialise ViewModel
        vm = new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication()).create(ScanViewModel.class);

        camExecutor = Executors.newSingleThreadExecutor();

        vm.getScanResult().observe(getViewLifecycleOwner(), result -> {
            Snackbar.make(view, result, Snackbar.LENGTH_LONG).show();
            cameraShutdownCallback.shutdown();
            Log.d("CAMERAX", "SHUTDOWN:: ");
            startCamera();
            Log.d("CAMERAX", "START:: ");
        });

        vm.getModelDownloaded().observe(getViewLifecycleOwner(), downloaded -> {
            if (downloaded == Boolean.FALSE) {
                // Not downloaded yet
                Snackbar.make(view, R.string.no_model, Snackbar.LENGTH_LONG).show();
            }
        });

        int permissionStatus = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            permissionStatus = requireContext().checkSelfPermission(cameraPermission);
        }
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {

            startCamera();

        } else if (shouldShowRequestPermissionRationale(cameraPermission)) {
            displayRationale();
        } else {
            requestPm();
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    public void startCamera() {
        CameraFailureCallback cameraFailureCallback = cameraFailureDialog();
        SetTouchListenerCallback touchListenerCallback = listener -> binding.previewView.setOnTouchListener(listener);

        try {
            cameraShutdownCallback = camAccessObj.startCamera(getViewLifecycleOwner(), cameraFailureCallback, touchListenerCallback);

        } catch (ReferenceInvalidException e) {
            Log.e(TAG, "Context invalid.");
        }

    }

    @NonNull
    private CameraFailureCallback cameraFailureDialog() {
        return e -> Log.e(TAG, "Camera unavailable.");
    }


    private void requestPm() {
        // Permission denied. Request the permission.
        requestPermissions(new String[]{cameraPermission}, CAMERA_REQUEST_CODE);
    }

    private void displayRationale() {
        // Display a permission rationale.
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.cam_pm)
                .setMessage(R.string.grant_pm_rationale)
                .setPositiveButton(R.string.grant, (dialog, which) -> requestPm())
                .setNegativeButton(R.string.cancel, (dialog, which) -> permissionDenied())
                .setCancelable(false)
                .create()
                .show();
    }

    private void permissionDenied() {

        Snackbar snack = Snackbar.make(binding.coordinatorLayout,
                        R.string.pm_denial, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.grant, v -> requestPm());

        setMargins(snack);
        snack.setBehavior(new DisableSwipeBehavior());

        snack.show();
    }

    private void permissionDeniedPermanently() {
        Snackbar snack = Snackbar.make(binding.previewView, R.string.pm_denial_permanant, Snackbar.LENGTH_INDEFINITE);
        snack.setAction(R.string.grant, v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri pkg = Uri.fromParts("package", requireContext().getPackageName(), null);
            intent.setData(pkg);
            startActivity(intent);
        });
        setMargins(snack);
        snack.show();
    }

    private void setMargins(Snackbar snack) {
        CoordinatorLayout.LayoutParams params = ((CoordinatorLayout.LayoutParams) snack.getView().getLayoutParams());
        snack.getView().setLayoutParams(params);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();


        binding = null;
        if (cameraShutdownCallback != null) {
            // Shutdown the camera if there is a callback, if not, most likely the camera is not even open anyway.
            cameraShutdownCallback.shutdown();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (camExecutor != null) {
            camExecutor.shutdown();
        }
    }

    private static class DisableSwipeBehavior extends BaseTransientBottomBar.Behavior {
        @Override
        public boolean canSwipeDismissView(@NonNull View view) {
            return false;
        }
    }

    private void noCamera() {
        AlertDialog cameraWarningDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.no_cam)
                .setMessage(R.string.no_cam_msg)
                .create();

        cameraWarningDialog.show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions has been granted.
                startCamera();
            } else if (shouldShowRequestPermissionRationale(cameraPermission)) {
                displayRationale();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (requireContext().checkSelfPermission(cameraPermission) == PackageManager.PERMISSION_DENIED
                        && !shouldShowRequestPermissionRationale(cameraPermission)) {
                    permissionDeniedPermanently(); // Permanent denial
                }
            }
        }
    }
}