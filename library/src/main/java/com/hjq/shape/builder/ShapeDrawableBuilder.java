package com.hjq.shape.builder;

import android.content.res.TypedArray;
import android.util.TypedValue;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.hjq.shape.config.IShapeDrawableStyleable;
import com.hjq.shape.drawable.ShapeDrawable;
import com.hjq.shape.drawable.ShapeGradientOrientation;
import com.hjq.shape.drawable.ShapeGradientType;
import com.hjq.shape.drawable.ShapeGradientTypeLimit;
import com.hjq.shape.drawable.ShapeType;
import com.hjq.shape.drawable.ShapeTypeLimit;
import com.hjq.shape.other.ExtendStateListDrawable;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * author : Android 轮子哥
 * github : https://github.com/getActivity/ShapeView
 * time   : 2021/08/28
 * desc   : ShapeDrawable 构建类
 */
public final class ShapeDrawableBuilder {

    private static final int NO_COLOR = Color.TRANSPARENT;

    private final View mView;

    @ShapeTypeLimit
    private int mType;
    private int mWidth;
    private int mHeight;

    private int mSolidColor;
    private Integer mSolidPressedColor;
    private Integer mSolidCheckedColor;
    private Integer mSolidDisabledColor;
    private Integer mSolidFocusedColor;
    private Integer mSolidSelectedColor;

    private float mTopLeftRadius;
    private float mTopRightRadius;
    private float mBottomLeftRadius;
    private float mBottomRightRadius;

    private int[] mSolidGradientColors;

    public int[] getSolidGradientEnableColors() {
        return solidGradientEnableColors;
    }

    public int[] getSolidGradientDisableColors() {
        return solidGradientDisableColors;
    }

    private int[] solidGradientEnableColors;
    private int[] solidGradientDisableColors;
    private ShapeGradientOrientation mSolidGradientOrientation;
    @ShapeGradientTypeLimit
    private int mSolidGradientType;
    private float mSolidGradientCenterX;
    private float mSolidGradientCenterY;
    private int mSolidGradientRadius;

    private int mStrokeColor;
    private Integer mStrokePressedColor;
    private Integer mStrokeCheckedColor;
    private Integer mStrokeDisabledColor;
    private Integer mStrokeFocusedColor;
    private Integer mStrokeSelectedColor;

    private int[] mStrokeGradientColors;
    /** 多停靠点专用颜色，避免覆盖旧描边渐变数据。 */
    private int[] mMultiStopStrokeGradientColors;
    private float[] mStrokeGradientPositions;
    private float mStrokeGradientAngle = Float.NaN;
    /** 多停靠点模式下是否实际绘制阴影，默认只保留阴影占位。 */
    private boolean mStrokeGradientEnableShadow;
    private ShapeGradientOrientation mStrokeGradientOrientation;

    private int mStrokeSize;
    private int mStrokeDashSize;
    private int mStrokeDashGap;

    private int mShadowSize;
    private int mShadowColor;
    private int mShadowOffsetX;
    private int mShadowOffsetY;

    private int mRingInnerRadiusSize;
    private float mRingInnerRadiusRatio;
    private int mRingThicknessSize;
    private float mRingThicknessRatio;

    private int mLineGravity;
    private final int layerType;

    public ShapeDrawableBuilder(View view, TypedArray typedArray, IShapeDrawableStyleable styleable) {
        mView = view;
        layerType = mView.getLayerType();
        mType = typedArray.getInt(styleable.getShapeTypeStyleable(), ShapeType.RECTANGLE);
        mWidth = typedArray.getDimensionPixelSize(styleable.getShapeWidthStyleable(), -1);
        mHeight = typedArray.getDimensionPixelSize(styleable.getShapeHeightStyleable(), -1);

        mSolidColor = typedArray.getColor(styleable.getSolidColorStyleable(), NO_COLOR);
        if (typedArray.hasValue(styleable.getSolidPressedColorStyleable())) {
            mSolidPressedColor = typedArray.getColor(styleable.getSolidPressedColorStyleable(), NO_COLOR);
        }
        if (styleable.getSolidCheckedColorStyleable() > 0 && typedArray.hasValue(styleable.getSolidCheckedColorStyleable())) {
            mSolidCheckedColor = typedArray.getColor(styleable.getSolidCheckedColorStyleable(), NO_COLOR);
        }
        if (typedArray.hasValue(styleable.getSolidDisabledColorStyleable())) {
            mSolidDisabledColor = typedArray.getColor(styleable.getSolidDisabledColorStyleable(), NO_COLOR);
        }
        if (typedArray.hasValue(styleable.getSolidFocusedColorStyleable())) {
            mSolidFocusedColor = typedArray.getColor(styleable.getSolidFocusedColorStyleable(), NO_COLOR);
        }
        if (typedArray.hasValue(styleable.getSolidSelectedColorStyleable())) {
            mSolidSelectedColor = typedArray.getColor(styleable.getSolidSelectedColorStyleable(), NO_COLOR);
        }

        int layoutDirection = view.getLayoutDirection();

        int radius = typedArray.getDimensionPixelSize(styleable.getRadiusStyleable(), 0);
        mTopLeftRadius = mTopRightRadius = mBottomLeftRadius = mBottomRightRadius = radius;

        if (typedArray.hasValue(styleable.getRadiusInTopStartStyleable())) {
            switch (layoutDirection) {
                case View.LAYOUT_DIRECTION_RTL:
                    mTopRightRadius = typedArray.getDimensionPixelSize(styleable.getRadiusInTopStartStyleable(), radius);
                    break;
                case View.LAYOUT_DIRECTION_LTR:
                default:
                    mTopLeftRadius = typedArray.getDimensionPixelSize(styleable.getRadiusInTopStartStyleable(), radius);
                    break;
            }
        }
        if (typedArray.hasValue(styleable.getRadiusInTopEndStyleable())) {
            switch (layoutDirection) {
                case View.LAYOUT_DIRECTION_RTL:
                    mTopLeftRadius = typedArray.getDimensionPixelSize(styleable.getRadiusInTopEndStyleable(), radius);
                    break;
                case View.LAYOUT_DIRECTION_LTR:
                default:
                    mTopRightRadius = typedArray.getDimensionPixelSize(styleable.getRadiusInTopEndStyleable(), radius);
                    break;
            }
        }
        if (typedArray.hasValue(styleable.getRadiusInBottomStartStyleable())) {
            switch (layoutDirection) {
                case View.LAYOUT_DIRECTION_RTL:
                    mBottomRightRadius = typedArray.getDimensionPixelSize(styleable.getRadiusInBottomStartStyleable(), radius);
                    break;
                case View.LAYOUT_DIRECTION_LTR:
                default:
                    mBottomLeftRadius = typedArray.getDimensionPixelSize(styleable.getRadiusInBottomStartStyleable(), radius);
                    break;
            }
        }
        if (typedArray.hasValue(styleable.getRadiusInBottomEndStyleable())) {
            switch (layoutDirection) {
                case View.LAYOUT_DIRECTION_RTL:
                    mBottomLeftRadius = typedArray.getDimensionPixelSize(styleable.getRadiusInBottomEndStyleable(), radius);
                    break;
                case View.LAYOUT_DIRECTION_LTR:
                default:
                    mBottomRightRadius = typedArray.getDimensionPixelSize(styleable.getRadiusInBottomEndStyleable(), radius);
                    break;
            }
        }

        if (typedArray.hasValue(styleable.getRadiusInTopLeftStyleable())) {
            mTopLeftRadius = typedArray.getDimensionPixelSize(styleable.getRadiusInTopLeftStyleable(), radius);
        }
        if (typedArray.hasValue(styleable.getRadiusInTopRightStyleable())) {
            mTopRightRadius = typedArray.getDimensionPixelSize(styleable.getRadiusInTopRightStyleable(), radius);
        }
        if (typedArray.hasValue(styleable.getRadiusInBottomLeftStyleable())) {
            mBottomLeftRadius = typedArray.getDimensionPixelSize(styleable.getRadiusInBottomLeftStyleable(), radius);
        }
        if (typedArray.hasValue(styleable.getRadiusInBottomRightStyleable())) {
            mBottomRightRadius = typedArray.getDimensionPixelSize(styleable.getRadiusInBottomRightStyleable(), radius);
        }


        if (typedArray.hasValue(styleable.getSolidGradientStartColorStyleable()) && typedArray.hasValue(styleable.getSolidGradientEndColorStyleable())) {
            if (typedArray.hasValue(styleable.getSolidGradientCenterColorStyleable())) {
                solidGradientEnableColors = new int[]{typedArray.getColor(styleable.getSolidGradientStartColorStyleable(), NO_COLOR),
                        typedArray.getColor(styleable.getSolidGradientCenterColorStyleable(), NO_COLOR),
                        typedArray.getColor(styleable.getSolidGradientEndColorStyleable(), NO_COLOR)};
            } else {
                solidGradientEnableColors = new int[]{typedArray.getColor(styleable.getSolidGradientStartColorStyleable(), NO_COLOR),
                        typedArray.getColor(styleable.getSolidGradientEndColorStyleable(), NO_COLOR)};
            }
        }

        //新增逻辑,处理disable渐变
        if (typedArray.hasValue(styleable.getSolidGradientStartDisabledColorStyleable()) && typedArray.hasValue(styleable.getSolidGradientEndDisabledColorStyleable())) {
            if (typedArray.hasValue(styleable.getSolidGradientCenterDisabledColorStyleable())) {
                solidGradientDisableColors = new int[]{typedArray.getColor(styleable.getSolidGradientStartDisabledColorStyleable(), NO_COLOR),
                        typedArray.getColor(styleable.getSolidGradientCenterDisabledColorStyleable(), NO_COLOR),
                        typedArray.getColor(styleable.getSolidGradientEndDisabledColorStyleable(), NO_COLOR)};
            } else {
                solidGradientDisableColors = new int[]{typedArray.getColor(styleable.getSolidGradientStartDisabledColorStyleable(), NO_COLOR),
                        typedArray.getColor(styleable.getSolidGradientEndDisabledColorStyleable(), NO_COLOR)};
            }
        }
        //如果禁用按钮没设置,默认使用已可用的
        if (solidGradientDisableColors == null) {
            solidGradientDisableColors = solidGradientEnableColors;
        }
        //可用和不可可用使用不同的颜色
        if (view.isEnabled()) {
            mSolidGradientColors = solidGradientEnableColors;
        } else {
            mSolidGradientColors = solidGradientDisableColors;
        }

        mSolidGradientOrientation = transformGradientOrientation(typedArray.getInt(styleable.getSolidGradientOrientationStyleable(), 0));
        mSolidGradientType = typedArray.getInt(styleable.getSolidGradientTypeStyleable(), ShapeGradientType.LINEAR_GRADIENT);
        mSolidGradientCenterX = typedArray.getFloat(styleable.getSolidGradientCenterXStyleable(), 0.5f);
        mSolidGradientCenterY = typedArray.getFloat(styleable.getSolidGradientCenterYStyleable(), 0.5f);
        mSolidGradientRadius = typedArray.getDimensionPixelSize(styleable.getSolidGradientRadiusStyleable(), radius);

        mStrokeColor = typedArray.getColor(styleable.getStrokeColorStyleable(), NO_COLOR);
        if (typedArray.hasValue(styleable.getStrokePressedColorStyleable())) {
            mStrokePressedColor = typedArray.getColor(styleable.getStrokePressedColorStyleable(), NO_COLOR);
        }
        if (styleable.getStrokeCheckedColorStyleable() > 0 && typedArray.hasValue(styleable.getStrokeCheckedColorStyleable())) {
            mStrokeCheckedColor = typedArray.getColor(styleable.getStrokeCheckedColorStyleable(), NO_COLOR);
        }
        if (typedArray.hasValue(styleable.getStrokeDisabledColorStyleable())) {
            mStrokeDisabledColor = typedArray.getColor(styleable.getStrokeDisabledColorStyleable(), NO_COLOR);
        }
        if (typedArray.hasValue(styleable.getStrokeFocusedColorStyleable())) {
            mStrokeFocusedColor = typedArray.getColor(styleable.getStrokeFocusedColorStyleable(), NO_COLOR);
        }
        if (typedArray.hasValue(styleable.getStrokeSelectedColorStyleable())) {
            mStrokeSelectedColor = typedArray.getColor(styleable.getStrokeSelectedColorStyleable(), NO_COLOR);
        }

        if (typedArray.hasValue(styleable.getStrokeGradientStartColorStyleable()) && typedArray.hasValue(styleable.getStrokeGradientEndColorStyleable())) {
            if (typedArray.hasValue(styleable.getStrokeGradientCenterColorStyleable())) {
                mStrokeGradientColors = new int[]{typedArray.getColor(styleable.getStrokeGradientStartColorStyleable(), NO_COLOR),
                        typedArray.getColor(styleable.getStrokeGradientCenterColorStyleable(), NO_COLOR),
                        typedArray.getColor(styleable.getStrokeGradientEndColorStyleable(), NO_COLOR)};
            } else {
                mStrokeGradientColors = new int[]{typedArray.getColor(styleable.getStrokeGradientStartColorStyleable(), NO_COLOR),
                        typedArray.getColor(styleable.getStrokeGradientEndColorStyleable(), NO_COLOR)};
            }
        }

        mStrokeGradientOrientation = transformGradientOrientation(typedArray.getInt(styleable.getStrokeGradientOrientationStyleable(), 0));
        readMultiStopStrokeGradient(typedArray, styleable);

        mStrokeSize = typedArray.getDimensionPixelSize(styleable.getStrokeSizeStyleable(), 0);
        mStrokeDashSize = typedArray.getDimensionPixelSize(styleable.getStrokeDashSizeStyleable(), 0);
        mStrokeDashGap = typedArray.getDimensionPixelSize(styleable.getStrokeDashGapStyleable(), 0);

        mShadowSize = typedArray.getDimensionPixelSize(styleable.getShadowSizeStyleable(), 0);
        mShadowColor = typedArray.getColor(styleable.getShadowColorStyleable(), 0x10000000);
        mShadowOffsetX = typedArray.getDimensionPixelOffset(styleable.getShadowOffsetXStyleable(), 0);
        mShadowOffsetY = typedArray.getDimensionPixelOffset(styleable.getShadowOffsetYStyleable(), 0);

        mRingInnerRadiusSize = typedArray.getDimensionPixelOffset(styleable.getRingInnerRadiusSizeStyleable(), -1);
        mRingInnerRadiusRatio = typedArray.getFloat(styleable.getRingInnerRadiusRatioStyleable(), 3.0f);
        mRingThicknessSize = typedArray.getDimensionPixelOffset(styleable.getRingThicknessSizeStyleable(), -1);
        mRingThicknessRatio = typedArray.getFloat(styleable.getRingThicknessRatioStyleable(), 9.0f);

        mLineGravity = typedArray.getInt(styleable.getLineGravityStyleable(), Gravity.CENTER);
    }

    public ShapeDrawableBuilder setType(@ShapeTypeLimit int type) {
        mType = type;
        return this;
    }

    @ShapeTypeLimit
    public int getType() {
        return mType;
    }

    public ShapeDrawableBuilder setWidth(int width) {
        mWidth = width;
        return this;
    }

    public int getWidth() {
        return mWidth;
    }

    public ShapeDrawableBuilder setHeight(int height) {
        mHeight = height;
        return this;
    }

    public int getHeight() {
        return mHeight;
    }

    public ShapeDrawableBuilder setRadius(float radius) {
        return setRadius(radius, radius, radius, radius);
    }

    public ShapeDrawableBuilder setRadius(float topLeftRadius, float topRightRadius,
                                          float bottomLeftRadius, float bottomRightRadius) {
        mTopLeftRadius = topLeftRadius;
        mTopRightRadius = topRightRadius;
        mBottomLeftRadius = bottomLeftRadius;
        mBottomRightRadius = bottomRightRadius;
        return this;
    }

    public ShapeDrawableBuilder setRadiusRelative(float topStartRadius, float topEndRadius,
                                                  float bottomStartRadius, float bottomEndRadius) {
        int layoutDirection = mView.getLayoutDirection();
        switch (layoutDirection) {
            case View.LAYOUT_DIRECTION_RTL:
                mTopLeftRadius = topEndRadius;
                mTopRightRadius = topStartRadius;
                mBottomLeftRadius = bottomEndRadius;
                mBottomRightRadius = bottomStartRadius;
                break;
            case View.LAYOUT_DIRECTION_LTR:
            default:
                mTopLeftRadius = topStartRadius;
                mTopRightRadius = topEndRadius;
                mBottomLeftRadius = bottomStartRadius;
                mBottomRightRadius = bottomEndRadius;
                break;
        }
        return this;
    }

    public ShapeDrawableBuilder setTopLeftRadius(float radius) {
        mTopLeftRadius = radius;
        return this;
    }

    public float getTopLeftRadius() {
        return mTopLeftRadius;
    }

    public ShapeDrawableBuilder setTopRightRadius(float radius) {
        mTopRightRadius = radius;
        return this;
    }

    public float getTopRightRadius() {
        return mTopRightRadius;
    }

    public ShapeDrawableBuilder setBottomLeftRadius(float radius) {
        mBottomLeftRadius = radius;
        return this;
    }

    public float getBottomLeftRadius() {
        return mBottomLeftRadius;
    }

    public ShapeDrawableBuilder setBottomRightRadius(float radius) {
        mBottomRightRadius = radius;
        return this;
    }

    public float getBottomRightRadius() {
        return mBottomRightRadius;
    }

    public ShapeDrawableBuilder setSolidColor(int color) {
        mSolidColor = color;
        clearSolidGradientColors();
        return this;
    }

    public int getSolidColor() {
        return mSolidColor;
    }

    public ShapeDrawableBuilder setSolidPressedColor(Integer color) {
        mSolidPressedColor = color;
        return this;
    }

    @Nullable
    public Integer getSolidPressedColor() {
        return mSolidPressedColor;
    }

    public ShapeDrawableBuilder setSolidCheckedColor(Integer color) {
        mSolidCheckedColor = color;
        return this;
    }

    @Nullable
    public Integer getSolidCheckedColor() {
        return mSolidCheckedColor;
    }

    public ShapeDrawableBuilder setSolidDisabledColor(Integer color) {
        mSolidDisabledColor = color;
        return this;
    }

    @Nullable
    public Integer getSolidDisabledColor() {
        return mSolidDisabledColor;
    }

    public ShapeDrawableBuilder setSolidFocusedColor(Integer color) {
        mSolidFocusedColor = color;
        return this;
    }

    @Nullable
    public Integer getSolidFocusedColor() {
        return mSolidFocusedColor;
    }

    public ShapeDrawableBuilder setSolidSelectedColor(Integer color) {
        mSolidSelectedColor = color;
        return this;
    }

    @Nullable
    public Integer getSolidSelectedColor() {
        return mSolidSelectedColor;
    }

    public ShapeDrawableBuilder setSolidGradientColors(int startColor, int endColor) {
        return setSolidGradientColors(new int[]{startColor, endColor});
    }

    public ShapeDrawableBuilder setSolidGradientColors(int startColor, int centerColor, int endColor) {
        return setSolidGradientColors(new int[]{startColor, centerColor, endColor});
    }

    public ShapeDrawableBuilder setSolidGradientColors(int[] colors) {
        mSolidGradientColors = colors;
        return this;
    }

    @Nullable
    public int[] getSolidGradientColors() {
        return mSolidGradientColors;
    }

    public boolean isSolidGradientColorsEnable() {
        return mSolidGradientColors != null &&
                mSolidGradientColors.length > 0;
    }

    public void clearSolidGradientColors() {
        mSolidGradientColors = null;
    }

    public ShapeDrawableBuilder setSolidGradientOrientation(ShapeGradientOrientation orientation) {
        mSolidGradientOrientation = orientation;
        return this;
    }

    public ShapeGradientOrientation getSolidGradientOrientation() {
        return mSolidGradientOrientation;
    }

    public ShapeDrawableBuilder setSolidGradientType(@ShapeGradientTypeLimit int type) {
        mSolidGradientType = type;
        return this;
    }

    @ShapeGradientTypeLimit
    public int getSolidGradientType() {
        return mSolidGradientType;
    }

    public ShapeDrawableBuilder setSolidGradientCenterX(float centerX) {
        mSolidGradientCenterX = centerX;
        return this;
    }

    public float getSolidGradientCenterX() {
        return mSolidGradientCenterX;
    }

    public ShapeDrawableBuilder setSolidGradientCenterY(float centerY) {
        mSolidGradientCenterY = centerY;
        return this;
    }

    public float getSolidGradientCenterY() {
        return mSolidGradientCenterY;
    }

    public ShapeDrawableBuilder setSolidGradientRadius(int radius) {
        mSolidGradientRadius = radius;
        return this;
    }

    public int getSolidGradientRadius() {
        return mSolidGradientRadius;
    }

    public ShapeDrawableBuilder setStrokeColor(int color) {
        mStrokeColor = color;
        clearStrokeGradientColors();
        return this;
    }

    public int getStrokeColor() {
        return mStrokeColor;
    }

    public ShapeDrawableBuilder setStrokePressedColor(Integer color) {
        mStrokePressedColor = color;
        return this;
    }

    @Nullable
    public Integer getStrokePressedColor() {
        return mStrokePressedColor;
    }

    public ShapeDrawableBuilder setStrokeCheckedColor(Integer color) {
        mStrokeCheckedColor = color;
        return this;
    }

    @Nullable
    public Integer getStrokeCheckedColor() {
        return mStrokeCheckedColor;
    }

    public ShapeDrawableBuilder setStrokeDisabledColor(Integer color) {
        mStrokeDisabledColor = color;
        return this;
    }

    @Nullable
    public Integer getStrokeDisabledColor() {
        return mStrokeDisabledColor;
    }

    public ShapeDrawableBuilder setStrokeFocusedColor(Integer color) {
        mStrokeFocusedColor = color;
        return this;
    }

    @Nullable
    public Integer getStrokeFocusedColor() {
        return mStrokeFocusedColor;
    }

    public ShapeDrawableBuilder setStrokeSelectedColor(Integer color) {
        mStrokeSelectedColor = color;
        return this;
    }

    @Nullable
    public Integer getStrokeSelectedColor() {
        return mStrokeSelectedColor;
    }

    public ShapeDrawableBuilder setStrokeGradientColors(int startColor, int endColor) {
        return setStrokeGradientColors(new int[]{startColor, endColor});
    }

    public ShapeDrawableBuilder setStrokeGradientColors(int startColor, int centerColor, int endColor) {
        return setStrokeGradientColors(new int[]{startColor, centerColor, endColor});
    }

    public ShapeDrawableBuilder setStrokeGradientColors(int[] colors) {
        mStrokeGradientColors = colors == null ? null : colors.clone();
        mMultiStopStrokeGradientColors = colors == null ? null : colors.clone();
        if (colors == null) {
            mStrokeGradientPositions = null;
            // 清除新方案时同步清除角度，避免下次重新设置数组时意外复用旧角度。
            mStrokeGradientAngle = Float.NaN;
        }
        validateMultiStopStrokeGradient();
        return this;
    }

    @Nullable
    public int[] getStrokeGradientColors() {
        int[] colors = mStrokeGradientColors;
        // XML 只配置新属性时，返回当前生效的多停靠点颜色；非法配置不暴露给调用方。
        if (colors == null && isMultiStopStrokeGradientEnable()) {
            colors = mMultiStopStrokeGradientColors;
        }
        return colors == null ? null : colors.clone();
    }

    /**
     * 设置多停靠点描边的位置，位置必须在 0 到 1 之间且单调不减。
     */
    public ShapeDrawableBuilder setStrokeGradientPositions(float[] positions) {
        mStrokeGradientPositions = positions == null ? null : positions.clone();
        if (positions == null) {
            // 没有停靠位置时新方案未启用，角度也不应残留到下一次配置。
            mStrokeGradientAngle = Float.NaN;
        }
        validateMultiStopStrokeGradient();
        return this;
    }

    @Nullable
    public float[] getStrokeGradientPositions() {
        return mStrokeGradientPositions == null ? null : mStrokeGradientPositions.clone();
    }

    /**
     * 设置描边渐变角度，0 度从左至右，90 度从上至下。
     */
    public ShapeDrawableBuilder setStrokeGradientAngle(float angle) {
        if (Float.isNaN(angle) || Float.isInfinite(angle)) {
            // 角度无效时继续使用离散方向，不向业务方抛出异常。
            mStrokeGradientAngle = Float.NaN;
            return this;
        }
        mStrokeGradientAngle = angle;
        return this;
    }

    public float getStrokeGradientAngle() {
        return mStrokeGradientAngle;
    }

    /**
     * 设置多停靠点描边是否绘制阴影。默认不绘制，仅保留既有 shadowSize 的占位。
     */
    public ShapeDrawableBuilder setStrokeGradientEnableShadow(boolean enabled) {
        mStrokeGradientEnableShadow = enabled;
        return this;
    }

    public boolean isStrokeGradientEnableShadow() {
        return mStrokeGradientEnableShadow;
    }

    public boolean isStrokeGradientColorsEnable() {
        return mStrokeGradientColors != null &&
                mStrokeGradientColors.length > 0;
    }

    public void clearStrokeGradientColors() {
        mStrokeGradientColors = null;
        mMultiStopStrokeGradientColors = null;
        mStrokeGradientPositions = null;
        mStrokeGradientAngle = Float.NaN;
    }

    public ShapeDrawableBuilder setStrokeGradientOrientation(ShapeGradientOrientation orientation) {
        mStrokeGradientOrientation = orientation;
        // 显式切回旧离散方向时，不再复用此前多停靠点设置的任意角度。
        mStrokeGradientAngle = Float.NaN;
        return this;
    }

    public ShapeGradientOrientation getStrokeGradientOrientation() {
        return mStrokeGradientOrientation;
    }

    public ShapeDrawableBuilder setStrokeSize(int size) {
        mStrokeSize = size;
        return this;
    }

    public int getStrokeSize() {
        return mStrokeSize;
    }

    public ShapeDrawableBuilder setStrokeDashSize(int size) {
        mStrokeDashSize = size;
        return this;
    }

    public int getStrokeDashSize() {
        return mStrokeDashSize;
    }

    public ShapeDrawableBuilder setStrokeDashGap(int gap) {
        mStrokeDashGap = gap;
        return this;
    }

    public int getStrokeDashGap() {
        return mStrokeDashGap;
    }

    public boolean isStrokeDashLineEnable() {
        return mStrokeDashGap > 0;
    }

    public ShapeDrawableBuilder setRingInnerRadiusSize(int size) {
        mRingInnerRadiusSize = size;
        return this;
    }

    public int getRingInnerRadiusSize() {
        return mRingInnerRadiusSize;
    }

    public ShapeDrawableBuilder setRingInnerRadiusRatio(float ratio) {
        mRingInnerRadiusRatio = ratio;
        return this;
    }

    public float getRingInnerRadiusRatio() {
        return mRingInnerRadiusRatio;
    }

    public ShapeDrawableBuilder setRingThicknessSize(int size) {
        mRingThicknessSize = size;
        return this;
    }

    public int getRingThicknessSize() {
        return mRingThicknessSize;
    }

    public ShapeDrawableBuilder setRingThicknessRatio(float ratio) {
        mRingThicknessRatio = ratio;
        return this;
    }

    public float getRingThicknessRatio() {
        return mRingThicknessRatio;
    }

    public boolean isShadowEnable() {
        return mShadowSize > 0;
    }

    /** 多停靠点默认不画阴影；仅显式开启后才进入软件图层绘制。 */
    private boolean isShadowRenderEnable() {
        return isShadowEnable() && (!isMultiStopStrokeGradientEnable() ||
                mStrokeGradientEnableShadow);
    }

    public ShapeDrawableBuilder setShadowSize(int size) {
        mShadowSize = size;
        return this;
    }

    public int getShadowSize() {
        return mShadowSize;
    }

    public ShapeDrawableBuilder setShadowColor(int color) {
        mShadowColor = color;
        return this;
    }

    public int getShadowColor() {
        return mShadowColor;
    }

    public ShapeDrawableBuilder setShadowOffsetX(int offsetX) {
        mShadowOffsetX = offsetX;
        return this;
    }

    public int getShadowOffsetX() {
        return mShadowOffsetX;
    }

    public ShapeDrawableBuilder setShadowOffsetY(int offsetY) {
        mShadowOffsetY = offsetY;
        return this;
    }

    public int getShadowOffsetY() {
        return mShadowOffsetY;
    }

    public int getLineGravity() {
        return mLineGravity;
    }

    public ShapeDrawableBuilder setLineGravity(int gravity) {
        mLineGravity = gravity;
        return this;
    }

    public Drawable buildBackgroundDrawable() {
        Drawable viewBackground = mView.getBackground();

        boolean hasSolidColorState = mSolidPressedColor != null || mSolidCheckedColor != null ||
                mSolidDisabledColor != null || mSolidFocusedColor != null || mSolidSelectedColor != null;

        // 多停靠点描边优先级最高，旧状态描边不能覆盖它。
        boolean hasStrokeColorState = !isMultiStopStrokeGradientEnable() &&
                (mStrokePressedColor != null || mStrokeCheckedColor != null ||
                        mStrokeDisabledColor != null || mStrokeFocusedColor != null || mStrokeSelectedColor != null);

        if (!isSolidGradientColorsEnable() && !isStrokeGradientColorsEnable() &&
                !isMultiStopStrokeGradientEnable() &&
                mSolidColor == NO_COLOR && !hasSolidColorState && mStrokeColor == NO_COLOR && !hasStrokeColorState) {
            // 如果什么属性都没有设置，直接返回原先 View 的背景
            // Github issue 地址：https://github.com/getActivity/ShapeView/issues/104
            return viewBackground;
        }

        ShapeDrawable defaultDrawable;

        if (viewBackground instanceof ExtendStateListDrawable) {
            defaultDrawable = convertShapeDrawable(((ExtendStateListDrawable) viewBackground).getDefaultDrawable());
        } else {
            defaultDrawable = convertShapeDrawable(viewBackground);
        }

        refreshShapeDrawable(defaultDrawable, null, null);

        if (!hasSolidColorState && !hasStrokeColorState) {
            return defaultDrawable;
        }

        ExtendStateListDrawable stateListDrawable = new ExtendStateListDrawable();
        if (mSolidPressedColor != null || mStrokePressedColor != null) {
            ShapeDrawable drawable = convertShapeDrawable(stateListDrawable.getPressedDrawable());
            refreshShapeDrawable(drawable, mSolidPressedColor, mStrokePressedColor);
            stateListDrawable.setPressedDrawable(drawable);
        }

        if (mSolidCheckedColor != null || mStrokeCheckedColor != null) {
            ShapeDrawable drawable = convertShapeDrawable(stateListDrawable.getCheckDrawable());
            refreshShapeDrawable(drawable, mSolidCheckedColor, mStrokeCheckedColor);
            stateListDrawable.setCheckDrawable(drawable);
        }

        if (mSolidDisabledColor != null || mStrokeDisabledColor != null) {
            ShapeDrawable drawable = convertShapeDrawable(stateListDrawable.getDisabledDrawable());
            refreshShapeDrawable(drawable, mSolidDisabledColor, mStrokeDisabledColor);
            stateListDrawable.setDisabledDrawable(drawable);
        }

        if (mSolidFocusedColor != null || mStrokeFocusedColor != null) {
            ShapeDrawable drawable = convertShapeDrawable(stateListDrawable.getFocusedDrawable());
            refreshShapeDrawable(drawable, mSolidFocusedColor, mStrokeFocusedColor);
            stateListDrawable.setFocusedDrawable(drawable);
        }

        if (mSolidSelectedColor != null || mStrokeSelectedColor != null) {
            ShapeDrawable drawable = convertShapeDrawable(stateListDrawable.getSelectDrawable());
            refreshShapeDrawable(drawable, mSolidSelectedColor, mStrokeSelectedColor);
            stateListDrawable.setSelectDrawable(drawable);
        }

        stateListDrawable.setDefaultDrawable(defaultDrawable);
        return stateListDrawable;
    }

    /**
     * 构建软件图层缓存超限时使用的安全背景。
     * <p>
     * 仅保留形状、圆角、填充色与可硬件绘制的多停靠点描边，
     * 避免图片、阴影、虚线和填充渐变继续占用大缓存。
     */
    @NonNull
    private Drawable buildSafeBackgroundDrawable() {
        ShapeDrawable defaultDrawable = buildSafeShapeDrawable(resolveSafeSolidColor(null));
        boolean hasSolidColorState = mSolidPressedColor != null || mSolidCheckedColor != null ||
                mSolidDisabledColor != null || mSolidFocusedColor != null || mSolidSelectedColor != null;
        if (!hasSolidColorState) {
            return defaultDrawable;
        }

        ExtendStateListDrawable stateListDrawable = new ExtendStateListDrawable();
        if (mSolidPressedColor != null) {
            stateListDrawable.setPressedDrawable(buildSafeShapeDrawable(resolveSafeSolidColor(mSolidPressedColor)));
        }
        if (mSolidCheckedColor != null) {
            stateListDrawable.setCheckDrawable(buildSafeShapeDrawable(resolveSafeSolidColor(mSolidCheckedColor)));
        }
        if (mSolidDisabledColor != null) {
            stateListDrawable.setDisabledDrawable(buildSafeShapeDrawable(resolveSafeSolidColor(mSolidDisabledColor)));
        }
        if (mSolidFocusedColor != null) {
            stateListDrawable.setFocusedDrawable(buildSafeShapeDrawable(resolveSafeSolidColor(mSolidFocusedColor)));
        }
        if (mSolidSelectedColor != null) {
            stateListDrawable.setSelectDrawable(buildSafeShapeDrawable(resolveSafeSolidColor(mSolidSelectedColor)));
        }
        stateListDrawable.setDefaultDrawable(defaultDrawable);
        return stateListDrawable;
    }

    /** 仅设置安全降级所需的基础形状属性和可硬件绘制的多停靠点描边。 */
    @NonNull
    private ShapeDrawable buildSafeShapeDrawable(int solidColor) {
        ShapeDrawable drawable = new ShapeDrawable()
                .setType(mType)
                .setWidth(mWidth)
                .setHeight(mHeight)
                .setRadius(mTopLeftRadius, mTopRightRadius, mBottomLeftRadius, mBottomRightRadius)
                .setSolidColor(solidColor);

        // 安全降级必须复用正常分支的形状参数，避免圆环和线条在大 View 时改变几何形态。
        if (mRingInnerRadiusRatio > 0) {
            drawable.setRingInnerRadiusRatio(mRingInnerRadiusRatio);
        } else if (mRingInnerRadiusSize > -1) {
            drawable.setRingInnerRadiusSize(mRingInnerRadiusSize);
        }
        if (mRingThicknessRatio > 0) {
            drawable.setRingThicknessRatio(mRingThicknessRatio);
        } else if (mRingThicknessSize > -1) {
            drawable.setRingThicknessSize(mRingThicknessSize);
        }
        drawable.setLineGravity(mLineGravity);

        if (!isMultiStopStrokeGradientEnable()) {
            return drawable;
        }
        // 安全降级同样保留旧 shadowSize 的内缩，避免已上线卡片的视觉尺寸变化。
        drawable.setShadowInsetSize(mShadowSize);
        // LinearGradient 描边可由硬件 Canvas 绘制，不需要软件位图缓存，安全降级时仍可保留。
        drawable.setStrokeSize(mStrokeSize)
                .setStrokeGradientOrientation(mStrokeGradientOrientation)
                .setStrokeColor(mMultiStopStrokeGradientColors)
                .setStrokeGradientPositions(mStrokeGradientPositions);
        if (!Float.isNaN(mStrokeGradientAngle)) {
            drawable.setStrokeGradientAngle(mStrokeGradientAngle);
        }
        return drawable;
    }

    /** 优先使用状态填充色，其次取填充渐变的首个颜色作为稳定的纯色回退。 */
    private int resolveSafeSolidColor(@Nullable Integer stateColor) {
        if (stateColor != null) {
            return stateColor;
        }
        if (mSolidColor != NO_COLOR) {
            return mSolidColor;
        }
        if (isSolidGradientColorsEnable()) {
            return mSolidGradientColors[0];
        }
        return NO_COLOR;
    }

    public void refreshShapeDrawable(ShapeDrawable drawable,
                                     @Nullable Integer solidStateColor,
                                     @Nullable Integer strokeStateColor) {
        drawable.setType(mType)
                .setWidth(mWidth)
                .setHeight(mHeight)
                .setRadius(mTopLeftRadius, mTopRightRadius,
                        mBottomLeftRadius, mBottomRightRadius);

        drawable.setSolidGradientType(mSolidGradientType)
                .setSolidGradientOrientation(mSolidGradientOrientation)
                .setSolidGradientRadius(mSolidGradientRadius)
                .setSolidGradientCenterX(mSolidGradientCenterX)
                .setSolidGradientCenterY(mSolidGradientCenterY);

        drawable.setStrokeGradientOrientation(mStrokeGradientOrientation)
                .setStrokeSize(mStrokeSize)
                .setStrokeDashSize(mStrokeDashSize)
                .setStrokeDashGap(mStrokeDashGap);

        boolean multiStopStrokeGradientEnable = isMultiStopStrokeGradientEnable();
        // 多停靠点默认保留旧阴影留白，但不绘制阴影，从而兼容存量卡片的视觉尺寸。
        drawable.setShadowSize(isShadowRenderEnable() ? mShadowSize : 0)
                .setShadowInsetSize(multiStopStrokeGradientEnable ? mShadowSize : 0)
                .setShadowColor(mShadowColor)
                .setShadowOffsetX(mShadowOffsetX)
                .setShadowOffsetY(mShadowOffsetY);

        if (mRingInnerRadiusRatio > 0) {
            drawable.setRingInnerRadiusRatio(mRingInnerRadiusRatio);
        } else if (mRingInnerRadiusSize > -1) {
            drawable.setRingInnerRadiusSize(mRingInnerRadiusSize);
        }

        if (mRingThicknessRatio > 0) {
            drawable.setRingThicknessRatio(mRingThicknessRatio);
        } else if (mRingThicknessSize > -1) {
            drawable.setRingThicknessSize(mRingThicknessSize);
        }

        drawable.setLineGravity(mLineGravity);

        // 填充色设置
        if (solidStateColor != null) {
            drawable.setSolidColor(solidStateColor);
        } else if (isSolidGradientColorsEnable()) {
            drawable.setSolidColor(mSolidGradientColors);
        } else {
            drawable.setSolidColor(mSolidColor);
        }

        // 边框色设置
        // 多停靠点描边在所有状态下都优先，旧状态描边不能覆盖它。
        if (strokeStateColor != null && !isMultiStopStrokeGradientEnable()) {
            drawable.setStrokeColor(strokeStateColor);
        } else if (isMultiStopStrokeGradientEnable() || isStrokeGradientColorsEnable()) {
            drawable.setStrokeColor(isMultiStopStrokeGradientEnable() ?
                    mMultiStopStrokeGradientColors : mStrokeGradientColors);
        } else {
            drawable.setStrokeColor(mStrokeColor);
        }
        // 颜色先写入 Drawable，随后再设置依赖颜色长度校验的停靠位置。
        if (isMultiStopStrokeGradientEnable()) {
            drawable.setStrokeGradientPositions(mStrokeGradientPositions);
        } else {
            // 动态切回旧渐变时清除旧停靠点，避免颜色和位置数量不匹配。
            drawable.setStrokeGradientPositions(null);
        }
        // 角度属于完整多停靠点方案的一部分；方案未启用时保持旧离散方向。
        if (isMultiStopStrokeGradientEnable() && !Float.isNaN(mStrokeGradientAngle)) {
            drawable.setStrokeGradientAngle(mStrokeGradientAngle);
        }
    }

    /**
     * XML 配置无效时回退旧描边逻辑，避免因资源错误导致页面崩溃。
     */
    private void readMultiStopStrokeGradient(TypedArray typedArray, IShapeDrawableStyleable styleable) {
        int colorsStyleable = styleable.getStrokeGradientColorsStyleable();
        int positionsStyleable = styleable.getStrokeGradientPositionsStyleable();
        int angleStyleable = styleable.getStrokeGradientAngleStyleable();
        int enableShadowStyleable = styleable.getStrokeGradientEnableShadowStyleable();
        if (enableShadowStyleable >= 0) {
            mStrokeGradientEnableShadow = typedArray.getBoolean(enableShadowStyleable, false);
        }
        // -1 表示自定义 Styleable 未实现该属性；0 是 Android 合法的属性索引。
        if (colorsStyleable < 0 && positionsStyleable < 0 && angleStyleable < 0) {
            return;
        }
        boolean hasColors = colorsStyleable >= 0 && typedArray.hasValue(colorsStyleable);
        boolean hasPositions = positionsStyleable >= 0 && typedArray.hasValue(positionsStyleable);
        if (!hasColors && !hasPositions) {
            if (angleStyleable >= 0 && typedArray.hasValue(angleStyleable)) {
                mStrokeGradientAngle = typedArray.getFloat(angleStyleable, Float.NaN);
            }
            return;
        }
        if (!hasColors || !hasPositions) {
            Log.w("ShapeDrawableBuilder", "多停靠点描边必须同时配置颜色和位置数组，已回退旧逻辑");
            return;
        }
        try {
            mMultiStopStrokeGradientColors = readColorArray(typedArray.getResourceId(colorsStyleable, 0));
            mStrokeGradientPositions = readPositionArray(typedArray.getResourceId(positionsStyleable, 0));
            if (!validateMultiStopStrokeGradient()) {
                return;
            }
            if (angleStyleable >= 0 && typedArray.hasValue(angleStyleable)) {
                mStrokeGradientAngle = typedArray.getFloat(angleStyleable, Float.NaN);
                if (Float.isNaN(mStrokeGradientAngle) || Float.isInfinite(mStrokeGradientAngle)) {
                    mStrokeGradientAngle = Float.NaN;
                }
            }
        } catch (RuntimeException exception) {
            Log.w("ShapeDrawableBuilder", "多停靠点描边配置无效，已回退旧逻辑：" + exception.getMessage());
            clearMultiStopStrokeGradient();
            mStrokeGradientAngle = Float.NaN;
        }
    }

    private int[] readColorArray(int resourceId) {
        if (resourceId == 0) {
            throw new IllegalArgumentException("颜色数组必须引用 array 资源");
        }
        TypedArray colors = mView.getResources().obtainTypedArray(resourceId);
        try {
            int[] result = new int[colors.length()];
            for (int index = 0; index < result.length; index++) {
                TypedValue value = colors.peekValue(index);
                if (value == null || (value.type != TypedValue.TYPE_REFERENCE &&
                        (value.type < TypedValue.TYPE_FIRST_COLOR_INT || value.type > TypedValue.TYPE_LAST_COLOR_INT))) {
                    throw new IllegalArgumentException("颜色数组包含非颜色项");
                }
                result[index] = colors.getColor(index, NO_COLOR);
            }
            return result;
        } finally {
            colors.recycle();
        }
    }

    private float[] readPositionArray(int resourceId) {
        if (resourceId == 0) {
            throw new IllegalArgumentException("位置数组必须引用 array 或 string-array 资源");
        }
        // obtainTypedArray 同时兼容普通 array 和 string-array，避免资源类型不同导致误回退。
        TypedArray positions = mView.getResources().obtainTypedArray(resourceId);
        try {
            float[] result = new float[positions.length()];
            for (int index = 0; index < result.length; index++) {
                TypedValue value = positions.peekValue(index);
                if (value == null) {
                    throw new IllegalArgumentException("位置数组包含空项");
                }
                if (value.type == TypedValue.TYPE_FLOAT) {
                    result[index] = value.getFloat();
                } else if (value.type >= TypedValue.TYPE_FIRST_INT &&
                        value.type <= TypedValue.TYPE_LAST_INT) {
                    result[index] = value.data;
                } else if (value.type == TypedValue.TYPE_STRING ||
                        value.type == TypedValue.TYPE_REFERENCE) {
                    String text = positions.getString(index);
                    if (text == null) {
                        throw new IllegalArgumentException("位置数组包含空字符串");
                    }
                    result[index] = Float.parseFloat(text.trim());
                } else {
                    throw new IllegalArgumentException("位置数组包含非数字项");
                }
            }
            return result;
        } finally {
            positions.recycle();
        }
    }

    private boolean isMultiStopStrokeGradientEnable() {
        // 仅在颜色和位置同时合法时启用新方案；不合法时保留数据，便于调用方修正后恢复。
        return validateMultiStopStrokeGradient();
    }

    /**
     * 校验多停靠点配置。校验失败只返回 false，不清除另一侧数据，
     * 这样调用方修正颜色或位置后可以重新启用多停靠点方案。
     */
    private boolean validateMultiStopStrokeGradient() {
        if (mMultiStopStrokeGradientColors == null || mStrokeGradientPositions == null) {
            return false;
        }
        if (mMultiStopStrokeGradientColors.length < 2 ||
                mMultiStopStrokeGradientColors.length != mStrokeGradientPositions.length) {
            return false;
        }
        float previous = -1f;
        for (float position : mStrokeGradientPositions) {
            if (Float.isNaN(position) || Float.isInfinite(position) || position < 0f || position > 1f || position < previous) {
                return false;
            }
            previous = position;
        }
        return true;
    }

    /** 清除仅属于新功能的数据，保留旧描边颜色以实现兼容回退。 */
    private void clearMultiStopStrokeGradient() {
        mMultiStopStrokeGradientColors = null;
        mStrokeGradientPositions = null;
        // 多停靠点配置失败时，同时回退到旧的离散渐变方向。
        mStrokeGradientAngle = Float.NaN;
    }

    @NonNull
    public ShapeDrawable convertShapeDrawable(Drawable drawable) {
        if (drawable instanceof ShapeDrawable) {
            return (ShapeDrawable) drawable;
        }
        return new ShapeDrawable();
    }


    //获取child的高度包含padding
    private int getChildHeight(View view) {
        int totalHeight = 0;
        if (view instanceof ViewGroup) {
            int count = ((ViewGroup) view).getChildCount();
            for (int i = 0; i < count; i++) {
                View childView = ((ViewGroup) view).getChildAt(i);
                totalHeight += childView.getMeasuredHeight() + childView.getPaddingTop() + childView.getPaddingBottom();
            }
        } else {
            totalHeight = view.getMeasuredHeight() + view.getPaddingTop() + view.getPaddingBottom();
        }
        return totalHeight;
    }

    /**
     * 是否超过View的最大缓存,算法参考系统源码可以查看View.buildDrawingCacheImpl()方法
     *
     * @return 是否超过内存
     */
    public boolean isOverLargeCache() {
        //*4表示RGB888,直接按大的计算
        int width = mView.getMeasuredWidth() + mView.getPaddingLeft() + mView.getPaddingRight();
        int height = mView.getMeasuredHeight() + mView.getPaddingTop() + mView.getPaddingBottom();
        int childMeasuredHeight = getChildHeight(mView);
        int lastHeight = Math.max(height, childMeasuredHeight);
        final long projectedBitmapSize = (long) width * lastHeight * 4;
        final long drawingCacheSize = ViewConfiguration.get(mView.getContext()).getScaledMaximumDrawingCacheSize();
        //项目缓存大于最大值
        if (projectedBitmapSize >= drawingCacheSize) {
            Log.w("ShapeDrawableBuilder", mView.getClass().getSimpleName() + " cache too lange,width:" + width + ",height:" + lastHeight + ",projectedBitmapSize:" + projectedBitmapSize + ",maxDrawingCacheSize:" + drawingCacheSize + " use android:background params to set");
            return true;
        }
        return false;
    }

    public void intoBackground() {
        //老的背景,用于当子view超过屏幕的时候,容错,不使用渐变,使用
        // 获取到的 Drawable 有可能为空
        Drawable drawable = buildBackgroundDrawable();
        //-1不设置软解、0设置软解、1使用view的background属性
        AtomicInteger atomicState = new AtomicInteger(-1);
        if (isStrokeDashLineEnable() || isShadowRenderEnable() || isSolidGradientColorsEnable()) {
            // 需要关闭硬件加速，否则虚线或者阴影在某些手机上面无法生效，关闭硬件加速当View的内容大小超过屏幕,不会绘制内容,此时舍去阴影是比较好的方案
            // https://developer.android.com/guide/topics/graphics/hardware-accel?hl=zh-cn
            //当View的缓存计算小于最大值才使用软解
            mView.post(() -> {
                if (!isOverLargeCache()) {
                    atomicState.set(0);
                } else {
                    atomicState.set(1);
                }
                rInto(atomicState, drawable);
            });
        }
        //设置View,再设置一次
        rInto(atomicState, drawable);
    }

    // -1：保持原图层；0：启用软件图层；1：恢复原图层并使用安全降级背景。
    private void rInto(AtomicInteger atomicState, Drawable drawable) {
        int lastShowState = atomicState.get();
        if (lastShowState == 0) {
            //关闭硬件加速
            mView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        } else {
            //使用之前的layerType
            mView.setLayerType(layerType, null);
        }
        // 软件图层缓存超限时，保留可稳定绘制的圆角、纯色背景和多停靠点描边，舍弃阴影、虚线及填充渐变。
        // 不能回退 View 原有背景：它可能是图片或复杂 Drawable，仍有超大缓存导致空白的风险。
        Drawable lastDrawable = lastShowState == 1 ? buildSafeBackgroundDrawable() : drawable;
        mView.setBackground(lastDrawable);
        //新增逻辑
        if (isStrokeDashLineEnable() || isShadowRenderEnable() || isSolidGradientColorsEnable()) {
            if (lastDrawable != null) {
                lastDrawable.setDither(true);
            }
            if (mView.getBackground() != null) {
                mView.getBackground().setDither(true);
            }
        }
    }

    /**
     * 将 ShapeView 框架中渐变色的 xml 属性值转换成 ShapeDrawable 中的枚举值
     */
    private ShapeGradientOrientation transformGradientOrientation(int value) {
        switch (value) {
            case 10:
                return ShapeGradientOrientation.START_TO_END;
            case 180:
                return ShapeGradientOrientation.RIGHT_TO_LEFT;
            case 1800:
                return ShapeGradientOrientation.END_TO_START;
            case 90:
                return ShapeGradientOrientation.BOTTOM_TO_TOP;
            case 270:
                return ShapeGradientOrientation.TOP_TO_BOTTOM;
            case 315:
                return ShapeGradientOrientation.TOP_LEFT_TO_BOTTOM_RIGHT;
            case 3150:
                return ShapeGradientOrientation.TOP_START_TO_BOTTOM_END;
            case 45:
                return ShapeGradientOrientation.BOTTOM_LEFT_TO_TOP_RIGHT;
            case 450:
                return ShapeGradientOrientation.BOTTOM_START_TO_TOP_END;
            case 225:
                return ShapeGradientOrientation.TOP_RIGHT_TO_BOTTOM_LEFT;
            case 2250:
                return ShapeGradientOrientation.TOP_END_TO_BOTTOM_START;
            case 135:
                return ShapeGradientOrientation.BOTTOM_RIGHT_TO_TOP_LEFT;
            case 1350:
                return ShapeGradientOrientation.BOTTOM_END_TO_TOP_START;
            case 0:
            default:
                return ShapeGradientOrientation.LEFT_TO_RIGHT;
        }
    }
}
