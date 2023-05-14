package com.camera.fastscanner.objects;

import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;

public class CodeAnalyser implements ImageAnalysis.Analyzer {

    private SuccessCallback mCallBack;
    private FailureHandler mExceptionHandler;


    public CodeAnalyser(SuccessCallback scanCallback, FailureHandler exceptionHandler) {

        mCallBack = scanCallback;
        mExceptionHandler = exceptionHandler;
    }


    @Override
    @androidx.camera.core.ExperimentalGetImage
    public void analyze(@NonNull ImageProxy imageProxy) {
        Image barcodeImage = imageProxy.getImage();
        Log.d("CAMERAX", "analyze: " + imageProxy.getImageInfo());
        if (barcodeImage != null) {
            InputImage inputImage = InputImage.fromMediaImage(barcodeImage, imageProxy.getImageInfo().getRotationDegrees());
            //Pass to ML Kit API
            BarcodeScanner barcodeScanner = BarcodeScanning.getClient();
            Task<List<Barcode>> result = barcodeScanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {

                        mCallBack.scannedBarcodes(barcodes);
                    })
                    .addOnFailureListener(exception -> {
                        mExceptionHandler.handleException(exception);

                    })
                    .addOnCompleteListener(task -> {
                        imageProxy.close();
                        barcodeScanner.close();
                    });


        }
    }


    public interface SuccessCallback {
        void scannedBarcodes(List<Barcode> barcodes);
    }

    public interface FailureHandler {
        void handleException(Exception e);
    }

}
