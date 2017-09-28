package com.htc.datausagemonitor.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.htc.datausagemonitor.R;

/**
 * Created by mj on 2017/8/5.
 */

public class CircleProgressView extends View {
    private int mWidth;
    private int mHeight;

    private Bitmap mBackgroundBitmap;

    private Path mPath;
    private Paint mPathPaint;
    private Paint textpaint1;
    private Paint textpaint2;
    private Paint blankpaint;


    private float mWaveHeight = 20f;
    private float mWaveHalfWidth = 100f;
    private String mWaveColor = "#32CD32"; //没超过时候 否则 #EE0000 红色预警
    private String BGColor="#01FFFFFF"; //背景颜色

    private int mWaveSpeed = 30;

    private Paint mTextPaint;
    private String mCurrentText = "";
    private String mCurrentTextGMKB = "";
    private String mCurrentTextInfo="";
    private String mTextColor = "#66CDAA";
    private String mblankColor="#87ceeb";
    private int mTextSize = 140;

    private int mMaxProgress = 100;
    private int mCurrentProgress = 0;
    private float mCurY;

    private float mDistance = 0;
    private int mRefreshGap = 10;
    //允许上升 下降
    private boolean mAllowProgressInBothDirections = true;

    private static final int INVALIDATE = 0X777;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case INVALIDATE:
                    invalidate();
                    sendEmptyMessageDelayed(INVALIDATE, mRefreshGap);
                    break;
            }
        }
    };


    public CircleProgressView(Context context) {
        this(context, null, 0);
    }

    public CircleProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleProgressView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        Init();
    }
    public void setProgress(int currentProgress) {
        this.mCurrentProgress = currentProgress;

    }
    public void setCurrent(int currentProgress, String currentText) {
        this.mCurrentProgress = currentProgress;
        this.mCurrentText = currentText;
    }
    public void setCurrentText( String currentText) {
        this.mCurrentText = currentText;
    }
    public void setMaxProgress(int maxProgress) {
        this.mMaxProgress = maxProgress;
    }

    public void setTextColor(String mTextColor) {
        this.mTextColor = mTextColor;

    }
    public void setText(String mTextColor, int mTextSize) {
        this.mTextColor = mTextColor;
        this.mTextSize = mTextSize;
    }

    public void setTextGMKBText(String TextGMKBText) {
        this.mCurrentTextGMKB=TextGMKBText;
    }
    public void setTextInfoText(String TextInfoText) {
        this.mCurrentTextInfo=TextInfoText;
    }

    public void setWave(float mWaveHight, float mWaveWidth) {
        this.mWaveHeight = mWaveHight;
        this.mWaveHalfWidth = mWaveWidth *2;
    }

    public void setBlankColor(String blankColor){this.mblankColor=blankColor;}
    public void setWaveColor(String mWaveColor) {
        this.mWaveColor = mWaveColor;
    }
    public void setBGColor(String BGColor) {
        this.BGColor = BGColor;
    }
    public void setWaveSpeed(int mWaveSpeed) {
        this.mWaveSpeed = mWaveSpeed;
    }

    public void allowProgressInBothDirections(boolean allow) {
        this.mAllowProgressInBothDirections = allow;
    }

    private void Init() {

        if (null == getBackground()) {
            throw new IllegalArgumentException(String.format("background is null."));
        } else {
            mBackgroundBitmap = getBitmapFromDrawable(getBackground());
        }

        mPath = new Path();
        mPathPaint = new Paint();
        mPathPaint.setAntiAlias(true);
        mPathPaint.setStyle(Paint.Style.FILL);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        textpaint1 = new Paint();
        textpaint1.setAntiAlias(true);
        textpaint1.setTextAlign(Paint.Align.CENTER);
        textpaint2= new Paint();
        textpaint2.setAntiAlias(true);
        textpaint2.setTextAlign(Paint.Align.CENTER);
        blankpaint= new Paint();
        blankpaint.setAntiAlias(true);
        blankpaint.setTextAlign(Paint.Align.CENTER);

        handler.sendEmptyMessageDelayed(INVALIDATE, 100);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mCurY = mHeight = MeasureSpec.getSize(heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (mBackgroundBitmap != null) {
            canvas.drawBitmap(createImage(), 0, 0, null);
        }
    }

    private Bitmap createImage() {
        mPathPaint.setColor(Color.parseColor(mWaveColor));//mWaveColor
        mTextPaint.setColor(Color.parseColor(mTextColor));
        mTextPaint.setTextSize(mTextSize);
        textpaint1.setColor(Color.parseColor("#ffffff")); //通用流量
        textpaint1.setTextSize(50);
        textpaint2.setColor(Color.parseColor(mTextColor));//变色流量 和M
        textpaint2.setTextSize(50);
        blankpaint.setColor(Color.parseColor(mblankColor)); //中间的线
        blankpaint.setTextSize(50);
        float blankwidth=getResources().getDimension(R.dimen.blankline_width);
        blankpaint.setStrokeWidth(blankwidth);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Bitmap finalBmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        finalBmp.setHasAlpha(true);
        Canvas canvas = new Canvas(finalBmp);
        //canvas.drawColor(Color.parseColor("#ffffff"));

   /*     int min = Math.min(mWidth, mHeight);
        mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap, min, min, false);
        canvas.drawBitmap(mBackgroundBitmap, 0, 0, paint);*/

        //add
        int centre = getWidth()/2;
        int radius = centre;

        int mScreenWidth=getWidth();
        float t=(float)mCurrentProgress;
        float m=t/100;
        // 计算当前油量线和水平中线的距离
        float centerOffset = Math.abs(mScreenWidth / 2 * m
                - mScreenWidth / 4);
        // 计算油量线和与水平中线的角度
        float horiAngle = (float) (Math.asin(centerOffset / (mScreenWidth / 4)) * 180 / Math.PI);
        // 扇形的起始角度和扫过角度
        float startAngle, sweepAngle;
        if (m> 0.5F) {
            startAngle = 360F - horiAngle;
            sweepAngle = 180F + 2 * horiAngle;
        } else {
            startAngle = horiAngle;
            sweepAngle = 180F - 2 * horiAngle;
        }
        //add
        int mtempprogres;
        if(mCurrentProgress==100){
            mtempprogres=3;   //默认
       //     mPath.lineTo(mWidth,0);
        }
        else{
            mtempprogres=mCurrentProgress;
        }

        float CurMidY = mHeight * (mMaxProgress - mtempprogres) / mMaxProgress;
        float myheight=mHeight/2-(mHeight-CurMidY);
        double cufmidx=Math.sqrt(radius*radius-myheight*myheight);
        float x=(float)cufmidx;

        if (mAllowProgressInBothDirections || mCurY > CurMidY) {
            mCurY = mCurY - (mCurY - CurMidY) / 10;
        }
        mPath.reset();
        mPath.moveTo(0, mCurY);

      //  mPath.moveTo(0 - mDistance, mCurY);
       // mPath.moveTo(centre-x, mCurY);
        int waveNum = mWidth / ((int) mWaveHalfWidth * 4) +  8;
        int multiplier = 0;


         /*   for (int i = 0; i < waveNum; i++) {
                // mPath.quadTo(centre, mCurY - mWaveHeight, centre+x, mCurY);
                //  mPath.quadTo(centre+10, mCurY + mWaveHeight, centre+x, mCurY);
                mPath.quadTo(mWaveHalfWidth * (multiplier + 1) - mDistance, mCurY - mWaveHeight, mWaveHalfWidth * (multiplier + 2) - mDistance, mCurY);
                mPath.quadTo(mWaveHalfWidth * (multiplier + 3) - mDistance, mCurY + mWaveHeight, mWaveHalfWidth * (multiplier + 4) - mDistance, mCurY);
                multiplier += 4;
            }*/

       /* mDistance += mWaveHalfWidth / mWaveSpeed;
        mDistance = mDistance % (mWaveHalfWidth * 4);*/

        RectF oval = new RectF(centre - radius, centre - radius, centre
                + radius, centre + radius);
      //  paint.setColor(Color.parseColor("#ff0000"));

     //   canvas.drawArc(oval, startAngle, sweepAngle, false, paint);
        //    canvas.drawArc(oval, 0, 180, false, paint);

        //mPath.arcTo(oval,startAngle,sweepAngle);
        mPath.lineTo(mWidth,mCurY);
        mPath.lineTo(mWidth, mHeight);
        mPath.lineTo(0, mHeight);
        mPath.close();
        //mPathPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
        if(  mCurrentProgress>0) {
            canvas.drawPath(mPath, mPathPaint);
            int min = Math.min(mWidth, mHeight);
            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap, min, min, false);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
            canvas.drawBitmap(mBackgroundBitmap, 0, 0, paint);

        }else if(mCurrentProgress==0)
        {
            int min = Math.min(mWidth, mHeight);
            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap, min, min, false);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
            canvas.drawBitmap(mBackgroundBitmap, 0, 0, paint);
        }
        else{
            //Drawable drawable = this.getContext().getResources().getDrawable(R.drawable.circleover);//获取drawable
           // Bitmap  bitmap =getBitmapFromDrawable(drawable);
//            canvas.drawPath(mPath, mPathPaint);
//            int min = Math.min(mWidth, mHeight);
            //bitmap = Bitmap.createScaledBitmap(bitmap, min, min, false);
//            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
           // canvas.drawBitmap(bitmap, 0, 0, paint);

        }


/*        int centre = getWidth()/2;
        int radius = centre;
        paint.setColor(Color.parseColor("#11ffff00"));
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
        canvas.drawCircle(centre, centre, radius, paint);*/
      //  canvas.drawCircle(centre, centre, radius, paint); //����Բ��
       //
       float m1=getResources().getDimension(R.dimen.circleviewblankline);

        canvas.drawLine(m1,mHeight/2,mWidth-m1,mHeight/2,blankpaint);

        float textWidth = paint.measureText(mCurrentText);
        //更改位置 在此

//        canvas.drawText(mCurrentText, mWidth / 2-textWidth/2, mHeight / 4, mTextPaint);
//        canvas.drawText(mCurrentTextGMKB, mWidth / 2+textWidth*7, mHeight /4 , textpaint2);//*6
//        canvas.drawText(mCurrentTextInfo, mWidth / 2, mHeight *2/5-25, textpaint2);
//        canvas.drawText(getResources().getString(R.string.data_common_used), mWidth / 2, mHeight *2/5+50, textpaint1);

        //更改位置 在此
     /*   canvas.drawText(mCurrentTextAbove, mWidth / 2, mHeight*1 / 3, mTextPaint);

        canvas.drawText(mCurrentText, mWidth / 2, mHeight / 2, mTextPaint);

        canvas.drawText(mCurrentTextBottom, mWidth / 2, mHeight *4/5, mTextPaint);*/

        return finalBmp;
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {

        if (drawable == null) {
            return null;
        }

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        try {
            Bitmap bitmap;
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }
}