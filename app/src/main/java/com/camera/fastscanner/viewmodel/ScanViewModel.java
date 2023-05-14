package com.camera.fastscanner.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.camera.fastscanner.objects.CodeAnalyser;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.barcode.common.Barcode;

import java.util.List;

public class ScanViewModel extends AndroidViewModel {

    private final CodeAnalyser codeAnalyser;
    private final MutableLiveData<Boolean> modelDownloaded;
    private final MutableLiveData<String> scanResult;

    public ScanViewModel(@NonNull Application application) {
        super(application);

        modelDownloaded = new MutableLiveData<>();
        modelDownloaded.setValue(true);

        scanResult = new MutableLiveData<>();

        codeAnalyser = new CodeAnalyser(
                barcodes -> {
                    if (barcodes.size() > 0) {
                        scanBarcode(barcodes);
                    }
                },
                (e) -> {
                    if (e instanceof MlKitException) {
                        // Barcode not downloaded.
                        modelDownloaded.setValue(false);
                    }
                }
        );
    }

    public CodeAnalyser getCodeAnalyser() {
        return codeAnalyser;
    }

    public MutableLiveData<String> getScanResult() {
        return scanResult;
    }

    public LiveData<Boolean> getModelDownloaded() {
        return modelDownloaded;
    }

    public void scanBarcode(List<Barcode> barcodes) {
        for (Barcode barcode : barcodes) {

            scanResult.setValue(barcode.getDisplayValue());
            break;
        }
        String result = scanResult.getValue();
        scanResult.setValue(result);
    }
}
