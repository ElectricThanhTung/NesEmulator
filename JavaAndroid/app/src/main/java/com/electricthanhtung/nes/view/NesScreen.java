package com.electricthanhtung.nes.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class NesScreen extends View {
    private Bitmap img;
    private Paint paint;
    private boolean fullScreenMode = false;

    public NesScreen(Context context) {
        super(context);
        initPaint();
    }

    public NesScreen(Context context, AttributeSet attrs) {
        super(context, attrs);
        fullScreenMode = attrs.getAttributeBooleanValue(
            "http://schemas.android.com/apk/res-auto",
            "fullScreenMode",
            false
        );
        initPaint();
    }

    private void initPaint() {
        paint = new Paint();
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        paint.setDither(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(img != null) {
            int w = this.getWidth();
            int h = this.getHeight();
            if(w > 0 && h > 0) {
                Rect rect1 = new Rect(0, 0, img.getWidth(), img.getHeight());
                Rect rect2;
                if (fullScreenMode == true)
                    rect2 = new Rect(0, 0, w, h);
                else {
                    int w2 = h * 256 / 220;
                    int offset = (w - w2) / 2;
                    rect2 = new Rect(offset, 0, w2 + offset, h);
                }
                canvas.drawBitmap(img, rect1, rect2, paint);
            }
        }
    }

    public void setImageBitmap(Bitmap bitmap) {
        if(bitmap != null) {
            img = bitmap;
            invalidate();
        }
    }

    public void setSetFullScreenMode(boolean value) {
        fullScreenMode = value;
        invalidate();
    }
}
