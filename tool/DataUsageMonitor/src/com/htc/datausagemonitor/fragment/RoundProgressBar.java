package com.htc.datausagemonitor.fragment;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.htc.datausagemonitor.R;

/**
 *
 * @author xiaanming
 *
 */
public class RoundProgressBar extends View {
    /**
     *
     */
    private Paint paint;
    private Paint smallpaint;
    private Paint Titlepaint;
    /**
     *
     */
    private int roundColor;

    /**
     *
     */
    private int roundProgressColor;

    /**
     *
     */
    private int textColor;

    /**
     *
     */
    private float textSize;

    /**
     *
     */
    private float roundWidth;

    /**
     *
     */
    private int max;

    /**
     *
     */
    private int progress;
    /**
     *
     */
    private boolean textIsDisplayable;

    /**
     *
     */
    private int style;

    private String GMKB="";
    private String info="";
    private String traffic="";
    public static final int STROKE = 0;
    public static final int FILL = 1;

    public RoundProgressBar(Context context) {
        this(context, null);
    }

    public RoundProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RoundProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        paint = new Paint();
        smallpaint=new Paint();
        Titlepaint=new Paint();
        TypedArray mTypedArray = context.obtainStyledAttributes(attrs,
                R.styleable.RoundProgressBar);

        //
        roundColor = mTypedArray.getColor(R.styleable.RoundProgressBar_roundColor, Color.RED);
        roundProgressColor = mTypedArray.getColor(R.styleable.RoundProgressBar_roundProgressColor, Color.GREEN);
        textColor = mTypedArray.getColor(R.styleable.RoundProgressBar_textColor, Color.GREEN);
        textSize = mTypedArray.getDimension(R.styleable.RoundProgressBar_textSize, 50);
        roundWidth = mTypedArray.getDimension(R.styleable.RoundProgressBar_roundWidth, 5);
        max = mTypedArray.getInteger(R.styleable.RoundProgressBar_max, 100);
        textIsDisplayable = mTypedArray.getBoolean(R.styleable.RoundProgressBar_textIsDisplayable, true);
        style = mTypedArray.getInt(R.styleable.RoundProgressBar_style, 0);

        mTypedArray.recycle();



    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /**
         *
         */
        int centre = getWidth()/2;
        // roundWidth-=50;
        int smalltestsize=50;
        int smallWidth=50;
        int radius = (int) (centre - roundWidth/2);
        paint.setColor(roundColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(roundWidth);
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);

        canvas.drawCircle(centre, centre, radius, paint);


        /**
         *
         */
        paint.setStrokeWidth(0);
        paint.setColor(textColor);
        paint.setTextSize(textSize);
       // paint.setTypeface(Typeface.DEFAULT_BOLD);
        int percent = (int)(((float)progress / (float)max) * 100);
        float textWidth = paint.measureText(traffic);
        smallpaint.setStrokeWidth(0);
        smallpaint.setColor(Color.parseColor("#ffffff"));
        smallpaint.setTextSize(smalltestsize);
       // smallpaint.setTypeface(Typeface.DEFAULT_BOLD);

        Titlepaint.setStrokeWidth(0);
        Titlepaint.setColor(textColor);
        Titlepaint.setTextSize(smalltestsize);
       // Titlepaint.setTypeface(Typeface.DEFAULT_BOLD);

      //  if(textIsDisplayable && percent != 0 && style == STROKE){
        if(textIsDisplayable && style == STROKE){
          //  canvas.drawText( getResources().getString(R.string.night_data_title),centre-smallWidth*2, centre + textSize/2, smallpaint); //显示
            //canvas.drawText(info,centre-smallWidth*2 , centre + textSize, Titlepaint); //显示
            //canvas.drawText(traffic , centre - textWidth / 2, centre + textSize*2, paint); //流量
            //canvas.drawText( GMKB, centre+textWidth/2, centre + textSize*2, Titlepaint); //单位
        }


        /**
         *
         */

        //
        paint.setStrokeWidth(roundWidth); //
        paint.setColor(roundProgressColor);  //

        RectF oval = new RectF(centre - radius, centre - radius, centre
                + radius, centre + radius);

        switch (style) {
            case STROKE:{
                paint.setStyle(Paint.Style.STROKE);
               if(progress<100) {
                    canvas.drawArc(oval, 270, 360 * progress / max, false, paint);
                }
                else{
                    canvas.drawArc(oval, 270, 10, false, paint);  //默认
                }
                break;
            }
            case FILL:{
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                if(progress <100)
                    canvas.drawArc(oval, 270, 360 * progress / max, true, paint);
                else {
                    canvas.drawArc(oval, 270, 10, true, paint);
                }
                break;
            }
        }

    }


    public synchronized int getMax() {
        return max;
    }

    /**
     *
     * @param max
     */
    public synchronized void setMax(int max) {
        if(max < 0){
            throw new IllegalArgumentException("max not less than 0");
        }
        this.max = max;
    }

    /**
     *
     * @return
     */
    public synchronized int getProgress() {
        return progress;
    }

    /**
     *
     *
     * @param progress
     */
    public synchronized void setProgress(int progress,String traffic) {
        if(progress < 0){
            throw new IllegalArgumentException("progress not less than 0");
        }
        if(progress > max){
            progress = max;
        }
        if(progress <= max){
            this.progress = progress;
            postInvalidate();
        }
        this.traffic=traffic;
    }
    public void setProgressOnly(int progress) {
        if(progress < 0){
            throw new IllegalArgumentException("progress not less than 0");
        }
        if(progress > max){
            progress = max;
        }
        if(progress <= max){
            this.progress = progress;
        }
    }

    public int getCricleColor() {
        return roundColor;
    }

    public void setCricleColor(int cricleColor) {
        this.roundColor = cricleColor;
    }

    public int getCricleProgressColor() {
        return roundProgressColor;
    }

    public void setCricleProgressColor(int cricleProgressColor) {
        this.roundProgressColor = cricleProgressColor;
    }

    public int getTextColor() {
        return textColor;
    }
    public void setTraffic(String traffic) {
        this.traffic=traffic;
    }
    public void setidleTextInfo(String info) {
        this.info=info;
    }
    public void setidleTextGMKB(String GMKB) {
        this.GMKB = GMKB;
    }
    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }
    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public float getRoundWidth() {
        return roundWidth;
    }

    public void setRoundWidth(float roundWidth) {
        this.roundWidth = roundWidth;
    }



}

