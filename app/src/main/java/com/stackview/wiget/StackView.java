package com.stackview.wiget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;

import com.stackview.R;

/**
 * @Author zhiqiang
 * @Description N张图片叠加并依次消失的控件
 * @Date 2019.5.1
 */
public class StackView extends FrameLayout {

    private static final String TAG = "StackView";
    private BaseAdapter mAdapter;
    private DataSetObserver mDataSetObserver;
    private int mFirstIndex;
    private boolean mIsAnimation;//是否在执行动画
    private StackRunnable mRunnable;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Context mContext;
    private Config mConfig;

    public StackView(Context context) {
        this(context, null);
    }

    public StackView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mConfig = new Config();

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StackView);

        mConfig.MAX_VISIBLE_COUNT = a.getInt(
                R.styleable.StackView_visibleCount, mConfig.MAX_VISIBLE_COUNT);
        mConfig.DURATION = a.getInt(
                R.styleable.StackView_animDuration, mConfig.DURATION);
        mConfig.OFFSET_X = a.getDimension(
                R.styleable.StackView_offset, mConfig.OFFSET_X);
        mConfig.BASE_SCALE = a.getFloat(R.styleable.StackView_scale, mConfig.BASE_SCALE);
        a.recycle();
    }


    public class Config {
        int MAX_VISIBLE_COUNT = 3;
        int DURATION = 2000;
        float OFFSET_X = dip2px(mContext, 10);
        float BASE_SCALE = 0.08F;
    }

    public void setAdapter(BaseAdapter adapter) {
        if (mAdapter != null && mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }
        mAdapter = adapter;
        if (mAdapter != null) {
            mDataSetObserver = new StackDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);
        }
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        float parentHeight;
        float parentWidth;

        resizedChildrenSize(widthMeasureSpec, heightMeasureSpec);
        parentWidth = getMeasuredSize(widthMeasureSpec, true);
        parentHeight = getMeasuredSize(heightMeasureSpec, false);
        setMeasuredDimension((int) parentWidth, (int) parentHeight);

    }

    /**
     * 重新设置子view的大小，动画需要额外空间
     */
    private void resizedChildrenSize(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        for (int i = 0; i < getChildCount(); i++) {
            View childView = getChildAt(i);
            int childWidth = childView.getMeasuredWidth();
            int childHeight = childView.getMeasuredHeight();
            if (childWidth * 1.7 > widthSize) {
                widthMeasureSpec = MeasureSpec.makeMeasureSpec((int) (widthSize / 1.7), widthMode);
            }
            if (childHeight*1.1 > heightSize) {
                heightMeasureSpec = MeasureSpec.makeMeasureSpec((int) (heightSize / 1.1), heightMode);
            }
            measureChild(childView, widthMeasureSpec, heightMeasureSpec);

        }
    }

    /**
     * 获取计算尺寸
     */
    private int getMeasuredSize(int measureSpec, boolean isWidth) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;//确切大小,所以将得到的尺寸给view
        } else {
            //结合父控件给子控件的最多大小(要不然会填充父控件),所以采用最小值
            float parentWidth;
            float parentHeight;

            float offsetWidth = mConfig.OFFSET_X * 2;
            float offsetHeight = 0;

            int childWidth = 0;
            int childHeight = 0;
            if (getChildCount() > 1) {
                View childView = getChildAt(getChildCount() - 1);
                if (childView != null) {
                    childWidth = childView.getMeasuredWidth();
                    childHeight = childView.getMeasuredHeight();
                    Log.d(TAG, "child w:" + childWidth + ",child h:" + childHeight);
                    offsetWidth = (float) (offsetWidth + childWidth * 0.6);//第一页消失动画有一个X方向的偏移和X方向的缩放，0.6是大约数。
                    offsetHeight = childHeight * 0.1f;//第一页消失动画有一个Y方向的偏移和Y方向的缩放，0.1是大约数。
                }
            }

            parentWidth = childWidth + offsetWidth;
            parentHeight = childHeight + offsetHeight;
            if (isWidth) {
                result = (int) Math.min(parentWidth, specSize);
            } else {
                result = (int) Math.min(parentHeight, specSize);
            }

        }
        return result;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (getChildCount() == 0) {
            initView();
        }
    }

    //初始化布局
    private void initView() {
        int itemCount = mAdapter.getCount();

        int layoutCount = Math.min(itemCount, mConfig.MAX_VISIBLE_COUNT);
        int viewLevel;//View的层级

        for (int pos = 0; pos < layoutCount; pos++) {
            mFirstIndex = pos % itemCount;
            View childView = mAdapter.getView(mFirstIndex, null, this);
            viewLevel = getChildCount();
            Log.d(TAG, "initView viewLevel:" + viewLevel);

            childView.setTranslationX(viewLevel * mConfig.OFFSET_X);
            childView.setScaleX(1 - viewLevel * mConfig.BASE_SCALE);
            childView.setScaleY(1 - viewLevel * mConfig.BASE_SCALE);
            //元素居右
            FrameLayout.LayoutParams layoutParams = (LayoutParams) childView.getLayoutParams();
            layoutParams.gravity = Gravity.END;
            layoutParams.setMargins(0, 0, (int) mConfig.OFFSET_X, 0);
            addViewInLayout(childView, 0, layoutParams, true);
            childView.requestLayout();
        }
    }


    private void exitWithAnimation() {
        firstViewAnim(getChildCount() - 1);
        otherViewAnim(getChildCount() - 2);
        otherViewAnim(getChildCount() - 3);
    }

    private void firstViewAnim(int index) {
        final View childView = getChildAt(index);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(childView, "scaleX", 1, 0.5f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(childView, "scaleY", childView.getScaleY(), 0.5f);
        ObjectAnimator translateX = ObjectAnimator.ofFloat(childView, "translationX", childView.getTranslationX(), childView.getTranslationX() - childView.getWidth());
        ObjectAnimator translateY = ObjectAnimator.ofFloat(childView, "translationY", childView.getTranslationY(), childView.getTranslationY() + childView.getHeight() * 0.3f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(childView, "alpha", 1f, 0f);

        AnimatorSet set = new AnimatorSet();
        set.play(scaleX).with(scaleY).with(alpha).with(translateX).with(translateY);

        set.setDuration(mConfig.DURATION);
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Log.d(TAG, "onAnimationStart");
                makeView();

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "onAnimationEnd child id" + childView);

                mHandler.post(mRunnable);//循环
                removeViewInLayout(childView);

            }

            @Override
            public void onAnimationCancel(Animator animation) {
                Log.d(TAG, "onAnimationCancel");

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                Log.d(TAG, "onAnimationRepeat");

            }
        });
        set.start();
        /*Animation animation = AnimationUtils.loadAnimation(getContext(),R.anim.out);
        childView.startAnimation(animation);
        Log.d(TAG, "firstViewAnim child id"+childView);*/


    }

    private void otherViewAnim(int index) {
        Log.d(TAG, "secondViewAnim index:" + index);

        View childView = getChildAt(index);
        View lastView = getChildAt(index + 1);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(childView, "scaleX", childView.getScaleX(), lastView.getScaleX());
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(childView, "scaleY", childView.getScaleY(), lastView.getScaleY());
        ObjectAnimator translateX = ObjectAnimator.ofFloat(childView, "translationX", childView.getTranslationX(), lastView.getTranslationX());
        AnimatorSet set = new AnimatorSet();
        set.play(scaleX).with(scaleY).with(translateX);
        set.setDuration(mConfig.DURATION);

        set.start();
    }


    private void makeView() {

        mFirstIndex++;
        if (mFirstIndex >= mAdapter.getCount()) {
            mFirstIndex = 0;
        }
        View covertView = mAdapter.getView(mFirstIndex, null, this);
        int level = getChildCount() - 1;
        covertView.setTranslationX(level * mConfig.OFFSET_X);
        covertView.setScaleX(1 - (level * mConfig.BASE_SCALE));
        covertView.setScaleY(1 - (level * mConfig.BASE_SCALE));
        //元素居右
        FrameLayout.LayoutParams layoutParams = (LayoutParams) covertView.getLayoutParams();
        layoutParams.gravity = Gravity.END;
        layoutParams.setMargins(0, 0, (int) mConfig.OFFSET_X, 0);
        addView(covertView, 0, layoutParams);
        Log.d(TAG, "makeView: " + getChildCount());
    }


    public void startLoop() {

        if (mRunnable != null || mIsAnimation) {
            return;
        }
        mRunnable = new StackRunnable();
        mHandler.post(mRunnable);
        mIsAnimation = true;
    }

    @Override
    protected void onDetachedFromWindow() {//view从屏幕移除时候及时销毁动画
        super.onDetachedFromWindow();
        mRunnable = null;
        mHandler.removeCallbacks(null);
        mIsAnimation = false;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new FrameLayout.LayoutParams(mContext, attrs);
    }

    private static int dip2px(Context context, float dpValue) {

        final float scale = context.getResources().getDisplayMetrics().density;

        return (int) (dpValue * scale + 0.5f);

    }


    private class StackRunnable implements Runnable {

        @Override
        public void run() {
            exitWithAnimation();
        }
    }

    private class StackDataSetObserver extends DataSetObserver {

        StackDataSetObserver() {
            super();
        }

        @Override
        public void onChanged() {
            removeAllViewsInLayout();
            requestLayout();
            super.onChanged();
        }

        @Override
        public void onInvalidated() {
            super.onInvalidated();
        }
    }

}
