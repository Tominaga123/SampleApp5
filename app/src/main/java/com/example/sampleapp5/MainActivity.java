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
import android.graphics.drawable.BitmapDrawable;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private ImageView imageView;
    private Bitmap bmp;
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

        textView = findViewById(R.id.text_view);
        imageView = findViewById(R.id.image_view);

        Button button = findViewById(R.id.button);
        Button saveButton = findViewById(R.id.button2);
        Button brighterButton = findViewById(R.id.button3);

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
                if (checkPermission()) {
                    saveImage();
                } else {
                    requestPermission();
                }
            }
        });

        //「Brighter」を押した場合
        brighterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // もし編集不可なら、編集可能な Bitmap を複製
                if (!bmp.isMutable()) {
                    System.out.println("編集可能にします");
                    bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
                }

                int w = bmp.getWidth();
                int h = bmp.getHeight();
                for(int y=0;y < h;y++){
                    for(int x=0;x < w;x++){
                        int c = bmp.getPixel(x, y);
                        //シフト演算してr,g,bごとに0x??の形にそろえる
                        int r = (c & 0x00ff0000) >> 16;
                        int g = (c & 0x0000ff00) >> 8;
                        int b = c & 0x000000ff;
                        final int add = 0x40;
                        r += add;
                        if (r > 0xff) r = 0xff;
                        g += add;
                        if (g > 0xff) g = 0xff;
                        b += add;
                        if (b > 0xff) b = 0xff;
                        c = 0xff000000 | (r << 16) | (g << 8) | b;
                        bmp.setPixel(x,y,c);
                    }
                }
                imageView.setImageBitmap(bmp);
            }
        });
    }

    void openImage(Intent resultData){
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
                pfDescriptor.close();
                imageView.setImageBitmap(bmp);
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
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_CODE
        );
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE && resultCode == RESULT_OK) {
            // パーミッションが許可された場合の処理
            saveImage();
        } else {
            // パーミッションが許可されなかった場合の処理
            Toast.makeText(this, "ストレージへのアクセスが許可されていません", Toast.LENGTH_SHORT).show();
        }
    }

}

/*	public void toLight() {
		try {
			BufferedImage readBuf=ImageIO.read(new File("C:\\pleiades\\2022-06\\workspace\\朝の人々1.jpg"));
			int w = readBuf.getWidth();
			int h = readBuf.getHeight();
			BufferedImage writeBuf =
			new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

			for(int y=0;y < h;y++){
				for(int x=0;x < w;x++){
					int c = readBuf.getRGB(x, y);
					//シフト演算してr,g,bごとに0x??の形にそろえる
					int r = (c & 0x00ff0000) >> 16;
					int g = (c & 0x0000ff00) >> 8;
					int b = c & 0x000000ff;
					final int add = 0x40;
					r += add;
					if (r > 0xff) r = 0xff;
					g += add;
					if (g > 0xff) g = 0xff;
					b += add;
					if (b > 0xff) b = 0xff;
					c = 0xff000000 | (r << 16) | (g << 8) | b;
					writeBuf.setRGB(x,y,c);
				}
			}

			ImageIO.write(writeBuf,"jpg", new File("C:\\pleiades\\2022-06\\workspace\\朝の人々1_明.jpg"));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}*/