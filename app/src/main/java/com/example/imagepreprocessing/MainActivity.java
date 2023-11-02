package com.example.imagepreprocessing;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int WRITE_PERMISSION_REQUEST_CODE = 1;

    Button button;

    String tag;
    int counter;

    SwitchMaterial aSwitch;





    public static String normalizeText(String input) {
        // Your text normalization code here
        String normalizedText = input.replaceAll("\\n+", "\n");
        normalizedText = normalizedText.replaceAll("\\s+", " ").trim();
        normalizedText = normalizedText.replaceAll("\\(\\S+\\)", "");
        normalizedText = normalizedText.replaceAll("https?://\\S+", "");

        return normalizedText;
    }

    public void exportData() throws IOException {
        // creates a directory for exported files
        File fpath = new File(Environment.getExternalStoragePublicDirectory("/Image_Processing_Exports").toString());
        if (!fpath.exists()){
            if (fpath.mkdir()){
                Toast.makeText(this,"Export directory created",Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(this,"Failed to create directory",Toast.LENGTH_SHORT).show();
            }
        }

        File xlfile = new File(fpath+"/demo.xls");
        if (!xlfile.exists()){
            // create new xls file to export data

            Workbook workbook = new HSSFWorkbook();
            Sheet sheet = workbook.createSheet();
            Row row= sheet.createRow(0);
            Cell cell= row.createCell(0);
            cell.setCellValue("message");
            Cell cell2 = row.createCell(1);
            cell2.setCellValue("spam/ham");

            FileOutputStream fos = new FileOutputStream(fpath+"/demo.xls");
            workbook.write(fos);
            fos.close();
        }



    }
    private void requestPermission() throws IOException {
        // if android is > 11
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);

            // check if already permitted
            if (!Environment.isExternalStorageManager()){
                Toast.makeText(MainActivity.this,"Please Grant permission",Toast.LENGTH_SHORT).show();
                startActivity(intent);
            }else {
                exportData();
            }

        } else {
            // Check if the permission is already granted
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // You already have permission; perform your storage operations here
                exportData();

            } else {
                // Request permission from the user
                Toast.makeText(MainActivity.this,"Please Grant permission",Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        aSwitch = findViewById(R.id.category_switch);
        button = findViewById(R.id.button);
        try {
            requestPermission();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        // Registers a photo picker activity launcher in multi-select mode.
        // In this example, the app lets the user select up to 5 media files.

        ActivityResultLauncher<PickVisualMediaRequest> pickMultipleMedia =
                registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(), uris -> {
                    // Callback is invoked after the user selects media items or closes the
                    // photo picker.
                    if (!uris.isEmpty()) {
                        Log.d("PhotoPickers", "Number of items selected: " + uris.size());

                        if (!aSwitch.isChecked()){
                            tag="ham";
                        }else {
                            tag="spam";
                        }

                        try {
                            FileInputStream fis = new FileInputStream(Environment.getExternalStoragePublicDirectory("/Image_Processing_Exports")+"/demo.xls");
                            Workbook workbook1 = new HSSFWorkbook(fis);
                            Sheet sheet1= workbook1.getSheet("Sheet0");
                            counter = sheet1.getLastRowNum();
                            fis.close();

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }


                        for (Uri imageUri:uris){

                            Log.d("PhotoPickers",imageUri+"");
                            InputImage image = null;
                            try {
                                image = InputImage.fromFilePath(MainActivity.this,imageUri);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
//
                            TextRecognizer recognizer = TextRecognition.getClient(new DevanagariTextRecognizerOptions.Builder().build());
                            Task<Text> result = recognizer.process(image).addOnSuccessListener(new OnSuccessListener<Text>() {
                                @Override
                                public void onSuccess(Text text) {
                                    if (text.getTextBlocks().isEmpty()){
                                        return;
                                    }
                                    Log.i("PhotoPickers", "onSuccess: "+text.getText());
                                    // onsuccess operations



                                    counter+=1;


                                    FileInputStream fileInputStream;
                                    FileOutputStream fos;
                                    try {
                                        fileInputStream = new FileInputStream(Environment.getExternalStoragePublicDirectory("/Image_Processing_Exports")+"/demo.xls");
                                        Workbook workbook = new HSSFWorkbook(fileInputStream);
                                        fileInputStream.close();
                                        Sheet sheet1 = workbook.getSheet("Sheet0");

                                        Row row1= sheet1.createRow(counter);
                                        Cell cell= row1.createCell(0);
                                        cell.setCellValue(normalizeText(text.getText()));
                                        Cell cell2 = row1.createCell(1);
                                        cell2.setCellValue(tag);

                                        fos = new FileOutputStream(Environment.getExternalStoragePublicDirectory("/Image_Processing_Exports")+"/demo.xls");
                                        workbook.write(fos);
                                        fos.close();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }


                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d("PhotoPickers","failure: "+e);
                                }
                            });

                        }
                        Toast.makeText(MainActivity.this,"Excel file updated",Toast.LENGTH_SHORT).show();



                    } else {
                        Log.d("PhotoPickers", "No media selected");
                    }

                });


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // launch the photo picker and let the user choose images
                // and videos. If you want the user to select a specific type of media file,
                // use the overloaded versions of launch(), as shown in the section about how
                // to select a single media item.


                pickMultipleMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
                                        // this warning can be ignored

            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now perform your storage operations
            } else {
                // Permission denied; handle this scenario, show a message, or gracefully degrade functionality
            }
        }
    }



//    public Bitmap convertToBlackAndWhite(Bitmap orignalBitmap){
//        Bitmap blckAndWhiteBitmap = Bitmap.createBitmap(orignalBitmap.getWidth(),orignalBitmap.getHeight(),Bitmap.Config.RGB_565);
//        Canvas canvas = new Canvas(blckAndWhiteBitmap);
//        ColorMatrix colorMatrix = new ColorMatrix();
//        colorMatrix.setSaturation(0);
//        Paint paint = new Paint();
//        paint.setColorFilter( new ColorMatrixColorFilter(colorMatrix));
//        canvas.drawBitmap(orignalBitmap,0,0,paint);
//        return blckAndWhiteBitmap;
//    }
//    private void saveBlackAndWhiteImage(Bitmap blackAndWhiteBitmap) {
//        // Check if external storage is available
//        String state = Environment.getExternalStorageState();
//        if (Environment.MEDIA_MOUNTED.equals(state)) {
//            // Define the directory where you want to save the image
//            File directory = new File(Environment.getExternalStorageDirectory(), "MyAppImages");
//
//            // Create the directory if it doesn't exist
//            if (!directory.exists()) {
//                directory.mkdirs();
//            }
//
//            // Define the filename for the saved image
//            String fileName = "black_and_white_image.jpg";
//
//            // Create a file object with the directory and filename
//            File imageFile = new File(directory, fileName);
//            if (imageFile.exists()) {
//                int fileCounter = 1;
//                String newFileName;
//
//                // Keep incrementing a counter until a unique filename is found
//                do {
//                    newFileName = "black_and_white_image_" + fileCounter + ".jpg";
//                    imageFile = new File(directory, newFileName);
//                    fileCounter++;
//                } while (imageFile.exists());
//            }
//
//            // Create an output stream to write the image data to the file
//            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
//                // Compress and save the black and white bitmap as a JPEG image
//                blackAndWhiteBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
//
//                // Notify the MediaScanner about the new image
//                MediaScannerConnection.scanFile(this, new String[]{imageFile.getAbsolutePath()}, null, null);
//
//                // Display a toast message to indicate successful save
//                Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show();
//            } catch (IOException e) {
//                e.printStackTrace();
//                // Handle the exception, e.g., show an error message
//                Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
//            }
//        } else {
//            // External storage is not available, handle accordingly
//            Toast.makeText(this, "External storage not available", Toast.LENGTH_SHORT).show();
//        }
//    }
}