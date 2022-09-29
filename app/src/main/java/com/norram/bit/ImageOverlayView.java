package com.norram.bit;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

public class ImageOverlayView extends ConstraintLayout {

    public ImageOverlayView(@NonNull Context context) {
        super(context, null, 0);
        View.inflate(context, R.layout.view_image_overlay, this);

        Button postButton = findViewById(R.id.postButton);
        Button browseButton = findViewById(R.id.browseButton);

        postButton.setOnClickListener(v -> {
            CommonData data = CommonData.getInstance();
            Uri uri = Uri.parse(data.chosenPermalink);
            Intent exIntent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(exIntent);
        });
        browseButton.setOnClickListener(v -> {
            CommonData data = CommonData.getInstance();
            Uri uri = Uri.parse(data.chosenUrl);
            Intent exIntent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(exIntent);
        });
    }
}