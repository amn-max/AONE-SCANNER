package com.aonescan.scanner.CostumClass;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.google.android.material.imageview.ShapeableImageView;

public class ImageMagnifier extends ShapeableImageView {
    private final int sizeOfMagnifier = 200;
    private PointF zoomPos;
    private boolean zooming = false;
    private Matrix matrix;
    private Paint paint;
    private Bitmap bitmap;
    private BitmapShader shader;

    public ImageMagnifier(Context context) {
        super(context);
        init();
    }

    public ImageMagnifier(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ImageMagnifier(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        zoomPos = new PointF(0, 0);
        matrix = new Matrix();
        paint = new Paint();

    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        Log.d("Tocuh", "TOuch");
        zoomPos.x = event.getX();
        zoomPos.y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                zooming = true;
                this.invalidate();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                zooming = false;
                this.invalidate();
                break;

            default:
                break;
        }

        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!zooming) {
            buildDrawingCache();
        } else {

            bitmap = getDrawingCache();
            shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

            paint = new Paint();
            paint.setShader(shader);
            matrix.reset();
            matrix.postScale(2f, 2f, zoomPos.x, zoomPos.y);
            paint.getShader().setLocalMatrix(matrix);
            canvas.drawCircle(zoomPos.x, zoomPos.y, sizeOfMagnifier, paint);
        }
    }
}
