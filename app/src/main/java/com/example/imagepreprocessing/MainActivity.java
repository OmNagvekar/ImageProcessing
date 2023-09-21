package com.example.imagepreprocessing;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST_CODE = 1;
    Button button;
    TextView textView;
    ImageView imageView;
    static  String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);
        button = findViewById(R.id.button);
        textView = findViewById(R.id.textView);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
//                Intent intent = Intent.createChooser(chooseFile, "Choose an image");
                startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE);


            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            path=getPathFromUri(imageUri);
            saveImagePathToSharedPreferences(path);
            InputImage image;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize=2;
            int minheightwidth =32;
            int imgwidth;
            int imgheight;
            float scale;
            int newwidth, newHeight;
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            imgheight = bitmap.getHeight();
            imgwidth = bitmap.getWidth();
            if(imgheight<minheightwidth||imgwidth<minheightwidth){
                scale = Math.max((float)minheightwidth/imgwidth,(float) minheightwidth/imgheight);
                newwidth = Math.round(imgwidth * scale);
                newHeight = Math.round(imgheight*scale);
                bitmap = Bitmap.createScaledBitmap(bitmap,newwidth,newHeight,true);

            }
            image = InputImage.fromBitmap(convertToBlackAndWhite(bitmap),0);
            imageView.setImageBitmap(convertToBlackAndWhite(bitmap));
            saveBlackAndWhiteImage(convertToBlackAndWhite(bitmap));
            TextRecognizer recognizer = TextRecognition.getClient(new DevanagariTextRecognizerOptions.Builder().build());
            Task<Text> result = recognizer.process(image).addOnSuccessListener(new OnSuccessListener<Text>() {
                @Override
                public void onSuccess(Text text) {
                    if (text.getTextBlocks().isEmpty()){
                        return;
                    }
                    textView.setText(text.getText());
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("error","failure: "+e);
                }
            });
            // Perform operations on the selected image file
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