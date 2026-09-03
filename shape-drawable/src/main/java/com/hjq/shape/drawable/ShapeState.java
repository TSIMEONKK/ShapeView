package com.hjq.shape.drawable;

import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/ShapeDrawable
 *    time   : 2021/08/15
 *    desc   : ShapeDrawable 参数构建
 */
public class ShapeState extends Drawable.ConstantState {

    public int changingConfigurations;
    @ShapeTypeLimit
    public int shapeType = ShapeType.RECTANGLE;
    @ShapeGradientTypeLimit
    public int solidGradientType = ShapeGradientType.LINEAR_GRADIENT;
    public ShapeGradientOrientation solidGradientOrientation = ShapeGradientOrientation.TOP_TO_BOTTOM;
    public int[] solidColors;
    public int[] strokeColors;
    public int[] tempSolidColors; // no need to copy
    public float[] tempSolidPositions; // no need to copy
    public float[] positions;
    /** 描边渐变的停靠位置，和填充渐变的位置独立保存。 */
    public float[] strokePositions;
    public boolean hasSolidColor;
    public boolean hasStrokeColor;
    public int solidColor;
    public int strokeSize = -1;   // if >= 0 use stroking.
    public ShapeGradientOrientation strokeGradientOrientation = ShapeGradientOrientation.TOP_TO_BOTTOM;
    /** 描边渐变角度：以屏幕坐标系为准，0 度从左至右，90 度从上至下。 */
    public float strokeGradientAngle = Float.NaN;
    public int strokeColor;
    public float strokeDashSize;
    public float strokeDashGap;
    public float radius;    // use this if mRadiusArray is null
    public float[] radiusArray;
    public Rect padding;
    public int width = -1;
    public int height = -1;
    public float ringInnerRadiusRatio;
    public float ringThicknessRatio;
    public int ringInnerRadiusSize = -1;
    public int ringThicknessSize = -1;
    public float solidCenterX = 0.5f;
    public float solidCenterY = 0.5f;
    public float gradientRadius = 0.5f;
    public boolean useLevel;
    public boolean useLevelForShape;
    public boolean opaque;

    public int shadowSize;
    /** 仅用于保留阴影留白，不触发阴影绘制。 */
    public int shadowInsetSize;
    public int shadowColor;
    public int shadowOffsetX;
    public int shadowOffsetY;

    public int lineGravity = Gravity.CENTER;

    public ShapeState() {}

    public ShapeState(ShapeState state) {
        changingConfigurations = state.changingConfigurations;
        shapeType = state.shapeType;
        solidGradientType = state.solidGradientType;
        solidGradientOrientation = state.solidGradientOrientation;
        if (state.solidColors != null) {
            solidColors = state.solidColors.clone();
        }
        if (state.strokeColors != null) {
            strokeColors = state.strokeColors.clone();
        }
        if (state.positions != null) {
            positions = state.positions.clone();
        }
        if (state.strokePositions != null) {
            strokePositions = state.strokePositions.clone();
        }
        hasSolidColor = state.hasSolidColor;
        hasStrokeColor = state.hasStrokeColor;
        solidColor = state.solidColor;
        strokeSize = state.strokeSize;
        strokeGradientOrientation = state.strokeGradientOrientation;
        strokeGradientAngle = state.strokeGradientAngle;
        strokeColor = state.strokeColor;
        strokeDashSize = state.strokeDashSize;
        strokeDashGap = state.strokeDashGap;
        radius = state.radius;
        if (state.radiusArray != null) {
            radiusArray = state.radiusArray.clone();
        }
        if (state.padding != null) {
            padding = new Rect(state.padding);
        }
        width = state.width;
        height = state.height;
        ringInnerRadiusRatio = state.ringInnerRadiusRatio;
        ringThicknessRatio = state.ringThicknessRatio;
        ringInnerRadiusSize = state.ringInnerRadiusSize;
        ringThicknessSize = state.ringThicknessSize;
        solidCenterX = state.solidCenterX;
        solidCenterY = state.solidCenterY;
        gradientRadius = state.gradientRadius;
        useLevel = state.useLevel;
        useLevelForShape = state.useLevelForShape;
        opaque = state.opaque;

        shadowSize = state.shadowSize;
        shadowInsetSize = state.shadowInsetSize;
        shadowColor = state.shadowColor;
        shadowOffsetX = state.shadowOffsetX;
        shadowOffsetY = state.shadowOffsetY;

        lineGravity = state.lineGravity;
    }

    @Override
    public Drawable newDrawable() {
        return new ShapeDrawable(this);
    }

    @Override
    public Drawable newDrawable(Resources res) {
        return new ShapeDrawable(this);
    }

    @Override
    public int getChangingConfigurations() {
        return changingConfigurations;
    }

    public void setType(int shape) {
        shapeType = shape;
        computeOpacity();
    }

    public void setSolidGradientType(int gradientType) {
        this.solidGradientType = gradientType;
    }

    public void setSolidColor(int... colors) {
        if (colors == null || colors.length == 0) {
            // 空数组和 null 都表示清除填充，避免后续创建 0 色 LinearGradient。
            solidColor = 0;
            hasSolidColor = true;
            solidColors = null;
            computeOpacity();
            return;
        }

        if (colors.length == 1) {
            hasSolidColor = true;
            solidColor = colors[0];
            solidColors = null;
        } else {
            hasSolidColor = false;
            solidColor = 0;
            solidColors = colors;
        }
        computeOpacity();
    }

    public void setSolidColor(int argb) {
        hasSolidColor = true;
        solidColor = argb;
        solidColors = null;
        computeOpacity();
    }

    private void computeOpacity() {
        if (shapeType != ShapeType.RECTANGLE) {
            opaque = false;
            return;
        }

        if (radius > 0 || radiusArray != null) {
            opaque = false;
            return;
        }

        if (shadowSize > 0 || shadowInsetSize > 0) {
            opaque = false;
            return;
        }

        if (strokeSize > 0 && !isOpaque(strokeColor)) {
            opaque = false;
            return;
        }

        if (hasSolidColor) {
            opaque = isOpaque(solidColor);
            return;
        }

        if (solidColors != null) {
            for (int color : solidColors) {
                if (!isOpaque(color)) {
                    opaque = false;
                    return;
                }
            }
        }

        if (hasStrokeColor) {
            opaque = isOpaque(strokeColor);
            return;
        }

        if (strokeColors != null) {
            for (int color : strokeColors) {
                if (!isOpaque(color)) {
                    opaque = false;
                    return;
                }
            }
        }

        opaque = true;
    }

    private static boolean isOpaque(int color) {
        return ((color >> 24) & 0xff) == 0xff;
    }

    public void setStrokeSize(int size) {
        strokeSize = size;
        computeOpacity();
    }

    /**
     * 设置仅用于形状内缩的阴影占位，并同步刷新 Drawable 的不透明度。
     */
    public void setShadowInsetSize(int size) {
        shadowInsetSize = Math.max(0, size);
        computeOpacity();
    }

    public void setStrokeColor(int... colors) {
        if (colors == null || colors.length == 0) {
            // 空数组和 null 都表示清除描边，避免后续创建 0 色 LinearGradient。
            strokeColor = 0;
            hasStrokeColor = true;
            strokeColors = null;
            computeOpacity();
            return;
        }

        if (colors.length == 1) {
            hasStrokeColor = true;
            strokeColor = colors[0];
            strokeColors = null;
        } else {
            hasStrokeColor = false;
            strokeColor = 0;
            strokeColors = colors;
        }
        computeOpacity();
    }

    public void setCornerRadius(float radius) {
        if (radius < 0) {
            radius = 0;
        }
        this.radius = radius;
        radiusArray = null;
    }

    public void setCornerRadii(float[] radii) {
        radiusArray = radii;
        if (radii == null) {
            radius = 0;
        }
    }
}
