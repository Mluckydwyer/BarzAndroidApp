package com.mluckydwyer.apps.barz;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;

public class ReviewActivity extends AppCompatActivity {

    private Intent share;
    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        String sharePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/myimages/popout.gif";
        Uri uri = Uri.parse(sharePath);
        share = new Intent(Intent.ACTION_SEND);
        share.setType("g");
        share.putExtra(Intent.EXTRA_STREAM, uri);

        Button shareButton = (Button) findViewById(R.id.button2);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(share, "Share Video File"));
            }
        });
    }
}