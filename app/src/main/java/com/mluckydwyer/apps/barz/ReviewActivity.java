package com.mluckydwyer.apps.barz;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ReviewActivity extends AppCompatActivity {

    public static boolean isGifReady = false;
    private static PropertyChangeSupport pcs;
    private Intent share;
    private ImageView imageView;

    public static void setIsGifReady(boolean isGifReadyState) {
        isGifReady = isGifReadyState;
        pcs.firePropertyChange("gifReady", !isGifReadyState, isGifReadyState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        pcs = new PropertyChangeSupport(this);
        pcs.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("gifReady"))
                    gifReady();
            }
        });

        FloatingActionButton shareButton = (FloatingActionButton) findViewById(R.id.sharebutton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(share, "Share GIF!"));
            }
        });

        Thread thread = new Thread(new Runnable() {
            public void run() {
                while (!isGifReady) {
                }
                gifReady();
            }
        });
        //thread.start();
    }

    public void gifReady() {
        String sharePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/popout.gif";
        Uri uri = Uri.parse(sharePath);
        share = new Intent(Intent.ACTION_SEND);
        share.setType("image/gif");
        share.putExtra(Intent.EXTRA_STREAM, uri);

        imageView = (ImageView) findViewById(R.id.imageviewgif);
        Glide.with(this).load(sharePath).into(imageView);

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress);
        progressBar.setVisibility(View.INVISIBLE);
    }
}