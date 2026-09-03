package com.hjq.shape.config;

/**
 * author : Android 轮子哥
 * github : https://github.com/getActivity/ShapeView
 * time   : 2021/08/28
 * desc   : ShapeDrawable View 属性收集接口
 */
public interface IShapeDrawableStyleable {

    int getShapeTypeStyleable();

    int getShapeWidthStyleable();

    int getShapeHeightStyleable();

    int getRadiusStyleable();

    int getRadiusInTopLeftStyleable();

    int getRadiusInTopStartStyleable();

    int getRadiusInTopRightStyleable();

    int getRadiusInTopEndStyleable();

    int getRadiusInBottomLeftStyleable();

    int getRadiusInBottomStartStyleable();

    int getRadiusInBottomRightStyleable();

    int getRadiusInBottomEndStyleable();

    int getSolidColorStyleable();

    int getSolidPressedColorStyleable();

    default int getSolidCheckedColorStyleable() {
        return 0;
    }

    int getSolidDisabledColorStyleable();

    int getSolidFocusedColorStyleable();

    int getSolidSelectedColorStyleable();

    int getSolidGradientStartColorStyleable();

    int getSolidGradientStartDisabledColorStyleable();

    int getSolidGradientCenterColorStyleable();

    int getSolidGradientCenterDisabledColorStyleable();

    int getSolidGradientEndColorStyleable();

    int getSolidGradientEndDisabledColorStyleable();

    int getSolidGradientOrientationStyleable();

    int getSolidGradientTypeStyleable();

    int getSolidGradientCenterXStyleable();

    int getSolidGradientCenterYStyleable();

    int getSolidGradientRadiusStyleable();

    int getStrokeColorStyleable();

    int getStrokePressedColorStyleable();

    default int getStrokeCheckedColorStyleable() {
        return 0;
    }

    int getStrokeDisabledColorStyleable();

    int getStrokeFocusedColorStyleable();

    int getStrokeSelectedColorStyleable();

    int getStrokeGradientStartColorStyleable();

    int getStrokeGradientCenterColorStyleable();

    int getStrokeGradientEndColorStyleable();

    int getStrokeGradientOrientationStyleable();

    /** 多停靠点描边颜色数组资源；返回 -1 表示当前 Styleable 未声明该属性。 */
    default int getStrokeGradientColorsStyleable() {
        return -1;
    }

    /** 多停靠点描边位置数组资源；返回 -1 表示当前 Styleable 未声明该属性。 */
    default int getStrokeGradientPositionsStyleable() {
        return -1;
    }

    /** 描边渐变角度，0 度从左至右，90 度从上至下；返回 -1 表示未声明。 */
    default int getStrokeGradientAngleStyleable() {
        return -1;
    }

    /** 多停靠点描边是否绘制阴影；返回 -1 表示未声明。 */
    default int getStrokeGradientEnableShadowStyleable() {
        return -1;
    }

    int getStrokeSizeStyleable();

    int getStrokeDashSizeStyleable();

    int getStrokeDashGapStyleable();

    int getShadowSizeStyleable();

    int getShadowColorStyleable();

    int getShadowOffsetXStyleable();

    int getShadowOffsetYStyleable();

    int getRingInnerRadiusSizeStyleable();

    int getRingInnerRadiusRatioStyleable();

    int getRingThicknessSizeStyleable();

    int getRingThicknessRatioStyleable();

    int getLineGravityStyleable();
}
