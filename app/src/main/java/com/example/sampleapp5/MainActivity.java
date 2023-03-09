package com.example.sampleapp5;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private TextView textView2;
    private SeekBar seekBar;
    private ImageView imageView;
    private Bitmap bmp;
    private Bitmap bmp2;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_CODE = 100;
    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent resultData  = result.getData();
                    if (resultData  != null) {
                        openImage(resultData);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkPermission()) {
            Toast.makeText(this, "許可されています", Toast.LENGTH_SHORT).show();
        } else {
            requestPermission();
        }

        textView = findViewById(R.id.text_view);
        Button button = findViewById(R.id.button);
        Button saveButton = findViewById(R.id.button2);
        textView2 = findViewById(R.id.text_view2);
        seekBar = findViewById(R.id.seekbar);
        imageView = findViewById(R.id.image_view);

        // 初期値を表示
        textView2.setText(seekBar.getProgress() + "");

        //「Get Image」を押した場合
        button.setOnClickListener( v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");

            resultLauncher.launch(intent);
        });

        //「Save Image」を押した場合
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImage();
            }
        });

        // 「SeekBar」が変化した場合
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int i = seekBar.getProgress();
                textView2.setText(i + "");

                int w = bmp.getWidth();
                int h = bmp.getHeight();
                for(int y=0;y < h;y++){
                    for(int x=0;x < w;x++){
                        int c = bmp.getPixel(x, y);
                        //シフト演算してr,g,bごとに0x??の形にそろえる
                        int r = (c & 0x00ff0000) >> 16;
                        int g = (c & 0x0000ff00) >> 8;
                        int b = c & 0x000000ff;
                        final int add = i;
                        r += add;
                        if (r > 0xff){
                            r = 0xff;
                        } else if (r < 0x00){
                            r = 0x00;
                        }
                        g += add;
                        if (g > 0xff){
                            g = 0xff;
                        } else if (g < 0x00){
                            g = 0x00;
                        }
                        b += add;
                        if (b > 0xff){
                            b = 0xff;
                        } else if (b < 0x00){
                            b = 0x00;
                        }
                        c = 0xff000000 | (r << 16) | (g << 8) | b;
                        bmp2.setPixel(x,y,c);
                    }
                }
                imageView.setImageBitmap(bmp2);
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {}
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void openImage(Intent resultData){
        ParcelFileDescriptor pfDescriptor = null;
        try{
            Uri uri = resultData.getData();
            // Uriを表示
            textView.setText(
                    String.format(Locale.US, "Uri:　%s",uri.toString()));

            pfDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            if(pfDescriptor != null){
                FileDescriptor fileDescriptor = pfDescriptor.getFileDescriptor();
                bmp = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                ExifInterface exif = new ExifInterface(fileDescriptor);
                rotateImage(exif);
                pfDescriptor.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try{
                if(pfDescriptor != null){
                    pfDescriptor.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void rotateImage(ExifInterface exif){
        try {
            // Exif メタデータを取得
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            // 回転角度を計算
            int rotationAngle = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotationAngle = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotationAngle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotationAngle = 270;
                    break;
                default:
                    break;
            }

            // 画像を回転させる
            if (rotationAngle != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationAngle);
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ImageView に Bitmap を設定
        imageView.setImageBitmap(bmp);
        // 編集用の Bitmap を複製
        bmp2 = bmp.copy(Bitmap.Config.ARGB_8888, true);
    }

    private void saveImage() {
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "SampleImage.jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        ContentResolver resolver = getContentResolver();
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try (OutputStream os = resolver.openOutputStream(Objects.requireNonNull(uri))) {
            if (os != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(uri, values, null, null);
        }
    }

    private boolean checkPermission() {
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        int result;
        int flag = 0;
        for (String permission : permissions) {
            result = ContextCompat.checkSelfPermission(this, permission);
            if (result == PackageManager.PERMISSION_GRANTED) {
                flag ++;
                System.out.println(permission + "は許可されています");
            }
        }
        if(flag == 2){
            return true;
        } else {
            return false;
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_CODE
        );
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE  && resultCode == RESULT_OK) {
            // パーミッションが許可された場合の処理
            Toast.makeText(this, "許可されました", Toast.LENGTH_SHORT).show();
        } else {
            // パーミッションが許可されなかった場合の処理
            Toast.makeText(this, "ストレージへのアクセスが許可されていません", Toast.LENGTH_SHORT).show();
        }
    }

}