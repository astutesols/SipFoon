package org.linphone.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import org.linphone.R;

/**
 * Class for making Imageview circular
 *
 * @author Rajesh Jadav
 */
//public class RoundedImageView extends ImageView {
//    private final Paint paint;
//    private final Paint paintBorder;
//    private int borderWidth;
//    private int canvasSize;
//
//    public RoundedImageView(final Context context) {
//        this(context, null);
//    }
//
//    public RoundedImageView(Context context, AttributeSet attrs) {
//        this(context, attrs, R.attr.circularImageViewStyle);
//    }
//
//    public RoundedImageView(Context context, AttributeSet attrs, int defStyle) {
//        super(context, attrs, defStyle);
//
//        // init paint
//        paint = new Paint();
//        paint.setAntiAlias(true);
//
//        paintBorder = new Paint();
//        paintBorder.setAntiAlias(true);
//
//        // load the styled attributes and set their properties
//        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.CircularImageView, defStyle, 0);
//
////        if (attributes.getBoolean(R.styleable.CircularImageView_border, true)) {
////            int defaultBorderSize = (int) (1 * getContext().getResources().getDisplayMetrics().density + 0.5f);
////            setBorderWidth(attributes.getDimensionPixelOffset(R.styleable.CircularImageView_border_width, defaultBorderSize));
////            setBorderColor(attributes.getColor(R.styleable.CircularImageView_border_color, getResources().getColor(R.color.blue_background)));
////        }
//
//        //if(attributes.getBoolean(R.styleable.CircularImageView_shadow, false))
//        //addShadow();
//    }
//
//    public void setBorderWidth(int borderWidth) {
//        this.borderWidth = borderWidth;
//        this.requestLayout();
//        this.invalidate();
//    }
//
//    public void setBorderColor(int borderColor) {
//        if (paintBorder != null)
//            paintBorder.setColor(borderColor);
//        this.invalidate();
//    }
//
//    public void addShadow() {
//        setLayerType(LAYER_TYPE_SOFTWARE, paintBorder);
//        paintBorder.setShadowLayer(3.0f, 2.0f, 1.0f, Color.BLACK);
//    }
//
//    @Override
//    public void onDraw(Canvas canvas) {
//        // load the bitmap
//        Bitmap image = drawableToBitmap(getDrawable());
//
//        // init shader
//        if (image != null) {
//
//            canvasSize = canvas.getWidth();
//            if (canvas.getHeight() < canvasSize)
//                canvasSize = canvas.getHeight();
//
//            BitmapShader shader = new BitmapShader(Bitmap.createScaledBitmap(image, canvasSize, canvasSize, false), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
//            paint.setShader(shader);
//
//            // circleCenter is the x or y of the view's center
//            // radius is the radius in pixels of the cirle to be drawn
//            // paint contains the shader that will texture the shape
//            int circleCenter = (canvasSize - (borderWidth * 2)) / 2;
//            //canvas.drawCircle(circleCenter + borderWidth, circleCenter + borderWidth, ((canvasSize - (borderWidth * 2)) / 2) + borderWidth - 4.0f, paintBorder);
//            canvas.drawCircle(circleCenter + borderWidth, circleCenter + borderWidth, ((canvasSize - (borderWidth * 2)) / 2) - 4.0f, paint);
//        }
//    }
//
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int width = measureWidth(widthMeasureSpec);
//        int height = measureHeight(heightMeasureSpec);
//        setMeasuredDimension(width, height);
//    }
//
//    private int measureWidth(int measureSpec) {
//        int result = 0;
//        int specMode = MeasureSpec.getMode(measureSpec);
//        int specSize = MeasureSpec.getSize(measureSpec);
//
//        if (specMode == MeasureSpec.EXACTLY) {
//            // The parent has determined an exact size for the child.
//            result = specSize;
//        } else if (specMode == MeasureSpec.AT_MOST) {
//            // The child can be as large as it wants up to the specified size.
//            result = specSize;
//        } else {
//            // The parent has not imposed any constraint on the child.
//            result = canvasSize;
//        }
//
//        return result;
//    }
//
//    private int measureHeight(int measureSpecHeight) {
//        int result = 0;
//        int specMode = MeasureSpec.getMode(measureSpecHeight);
//        int specSize = MeasureSpec.getSize(measureSpecHeight);
//
//        if (specMode == MeasureSpec.EXACTLY) {
//            // We were told how big to be
//            result = specSize;
//        } else if (specMode == MeasureSpec.AT_MOST) {
//            // The child can be as large as it wants up to the specified size.
//            result = specSize;
//        } else {
//            // Measure the text (beware: ascent is a negative number)
//            result = canvasSize;
//        }
//
//        return (result + 2);
//    }
//

//}


public class RoundedImageView extends ImageView {

    public RoundedImageView(Context context) {
        super(context);
    }

    public RoundedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RoundedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        Drawable drawable = getDrawable();

        if (drawable == null) {
            return;
        }

        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }

        if(drawable instanceof  BitmapDrawable){
            Bitmap b = ((BitmapDrawable) drawable).getBitmap();
            if(b != null){
                Bitmap bitmap = b.copy(Bitmap.Config.ARGB_8888, true);

                int w = getWidth(), h = getHeight();

                Bitmap roundBitmap = getCroppedBitmap(bitmap, w);
                canvas.drawBitmap(roundBitmap, 0, 0, null);
            }

        }
    }

    public static Bitmap getCroppedBitmap(Bitmap bmp, int radius) {
        Bitmap sbmp;

        if (bmp.getWidth() != radius || bmp.getHeight() != radius) {
            float smallest = Math.min(bmp.getWidth(), bmp.getHeight());
            float factor = smallest / radius;
            sbmp = Bitmap.createScaledBitmap(bmp, (int)(bmp.getWidth() / factor), (int)(bmp.getHeight() / factor), false);
        } else {
            sbmp = bmp;
        }

        Bitmap output = Bitmap.createBitmap(radius, radius,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xffa19774;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, radius, radius);

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.parseColor("#BAB399"));
        canvas.drawCircle(radius / 2 + 0.7f,
                radius / 2 + 0.7f, radius / 2 + 0.1f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(sbmp, rect, rect, paint);

        return output;
    }

}