package com.example.imagepreprocessing;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int WRITE_PERMISSION_REQUEST_CODE = 1;
    private static final int PICK_MULTIPLE_IMAGES = 2;
    Button button;
    EditText textView;
//    ImageView imageView;
    static  String path;


    private void requestPermission() {
        // if android is > 11
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);

            // check if already permitted
            if (!Environment.isExternalStorageManager()){
                startActivity(intent);
            }

        } else {
            // Check if the permission is already granted
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // You already have permission; perform your storage operations here
                Log.d("PhotoPickers", "onCreate: permission granted");
            } else {
                // Request permission from the user
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        imageView = findViewById(R.id.imageView);
        button = findViewById(R.id.button);
        textView = findViewById(R.id.textView);

       requestPermission();




        // Registers a photo picker activity launcher in multi-select mode.
        // In this example, the app lets the user select up to 5 media files.
//        Log.d("PhotoPickers", "Photo picker available "+ ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable());
        ActivityResultLauncher<PickVisualMediaRequest> pickMultipleMedia =
                registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(50), uris -> {
                    // Callback is invoked after the user selects media items or closes the
                    // photo picker.
                    if (!uris.isEmpty()) {
                        Log.d("PhotoPickers", "Number of items selected: " + uris.size());
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

                                    textView.setText(textView.getText() + "\n\n------------\n\n"+text.getText());
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.d("error","failure: "+e);
                                }
                            });




                        }
                    } else {
                        Log.d("PhotoPickers", "No media selected");
                    }

                });


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

//                pickMultipleImages();
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

    private void pickMultipleImages() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_MULTIPLE_IMAGES);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("PhotoPickers", "onResume: ");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_MULTIPLE_IMAGES && resultCode == RESULT_OK) {
            if (data != null) {
                // Get a list of selected image URIs
                ArrayList<Uri> selectedImageUris = new ArrayList<>();
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        selectedImageUris.add(imageUri);
                    }
                } else if (data.getData() != null) {
                    // Handle single image selection here
                    Uri imageUri = data.getData();
                    selectedImageUris.add(imageUri);
                }

                // Process the selected image URIs, for example, display or store them
                for (Uri uri : selectedImageUris) {
                    // Do something with the selected image URI
                }
            }
        }
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



    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String filePath = cursor.getString(column_index);
        cursor.close();
        return filePath;
    }
    private void saveImagePathToSharedPreferences(String imagePath) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("imagePath", imagePath);
        editor.apply();
    }
    public Bitmap convertToBlackAndWhite(Bitmap orignalBitmap){
        Bitmap blckAndWhiteBitmap = Bitmap.createBitmap(orignalBitmap.getWidth(),orignalBitmap.getHeight(),Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(blckAndWhiteBitmap);
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        Paint paint = new Paint();
        paint.setColorFilter( new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(orignalBitmap,0,0,paint);
        return blckAndWhiteBitmap;
    }
    private void saveBlackAndWhiteImage(Bitmap blackAndWhiteBitmap) {
        // Check if external storage is available
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Define the directory where you want to save the image
            File directory = new File(Environment.getExternalStorageDirectory(), "MyAppImages");

            // Create the directory if it doesn't exist
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Define the filename for the saved image
            String fileName = "black_and_white_image.jpg";

            // Create a file object with the directory and filename
            File imageFile = new File(directory, fileName);
            if (imageFile.exists()) {
                int fileCounter = 1;
                String newFileName;

                // Keep incrementing a counter until a unique filename is found
                do {
                    newFileName = "black_and_white_image_" + fileCounter + ".jpg";
                    imageFile = new File(directory, newFileName);
                    fileCounter++;
                } while (imageFile.exists());
            }

            // Create an output stream to write the image data to the file
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                // Compress and save the black and white bitmap as a JPEG image
                blackAndWhiteBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

                // Notify the MediaScanner about the new image
                MediaScannerConnection.scanFile(this, new String[]{imageFile.getAbsolutePath()}, null, null);

                // Display a toast message to indicate successful save
                Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                // Handle the exception, e.g., show an error message
                Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
            }
        } else {
            // External storage is not available, handle accordingly
            Toast.makeText(this, "External storage not available", Toast.LENGTH_SHORT).show();
        }
    }
}