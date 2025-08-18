package com.hjq.shape.layout;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RadioGroup;

import com.hjq.shape.R;
import com.hjq.shape.builder.ShapeDrawableBuilder;
import com.hjq.shape.config.IGetShapeDrawableBuilder;
import com.hjq.shape.styleable.ShapeRadioGroupStyleable;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/ShapeView
 *    time   : 2021/09/07
 *    desc   : 支持直接定义 Shape 背景的 RadioGroup
 */
public class ShapeRadioGroup extends RadioGroup implements IGetShapeDrawableBuilder {

    private static final ShapeRadioGroupStyleable STYLEABLE = new ShapeRadioGroupStyleable();

    private final ShapeDrawableBuilder mShapeDrawableBuilder;

    public ShapeRadioGroup(Context context) {
        this(context, null);
    }

    public ShapeRadioGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ShapeRadioGroup);
        mShapeDrawableBuilder = new ShapeDrawableBuilder(this, typedArray, STYLEABLE);
        typedArray.recycle();

        mShapeDrawableBuilder.intoBackground();
    }

    @Override
    public ShapeDrawableBuilder getShapeDrawableBuilder() {
        return mShapeDrawableBuilder;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (mShapeDrawableBuilder != null) {
            if (enabled) {
                mShapeDrawableBuilder.setSolidGradientColors(mShapeDrawableBuilder.getSolidGradientEnableColors());
                mShapeDrawableBuilder.intoBackground();
            } else {
                mShapeDrawableBuilder.setSolidGradientColors(mShapeDrawableBuilder.getSolidGradientDisableColors());
                mShapeDrawableBuilder.intoBackground();
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if ((oldw != 0 && w < oldw) || (oldh != 0 && h > oldh)) {
            mShapeDrawableBuilder.intoBackground();
        }
    }

}