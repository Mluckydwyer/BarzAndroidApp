package com.mluckydwyer.apps.barz;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class ReviewActivity extends AppCompatActivity {

    private Intent share;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        String sharePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/popout.gif";
        Uri uri = Uri.parse(sharePath);
        share = new Intent(Intent.ACTION_SEND);
        share.setType("image/gif");
        share.putExtra(Intent.EXTRA_STREAM, uri);

        imageView = (ImageView) findViewById(R.id.imageviewgif);
        Glide.with(this).load(sharePath).into(imageView);

        FloatingActionButton shareButton = (FloatingActionButton) findViewById(R.id.sharebutton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(share, "Share GIF!"));
            }
        });
    }
}