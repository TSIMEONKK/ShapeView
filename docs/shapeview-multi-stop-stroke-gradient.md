# ShapeView 多停靠点边框渐变接入方案

## 目标

在不改动既有 `ShapeLinearLayout`、`ShapeFrameLayout`、`ShapeTextView` 等控件标签的前提下，为 ShapeView 增加任意数量的边框渐变色标和停靠位置。

本方案用于复刻 Figma 导出的非均匀边框渐变，例如：

```css
linear-gradient(
    129.57deg,
    rgba(255, 255, 255, 0.2) 2.6%,
    rgba(255, 255, 255, 0.05) 18.75%,
    rgba(255, 255, 255, 0) 50.79%,
    rgba(255, 255, 255, 0) 77.36%,
    rgba(255, 255, 255, 0.08) 87.02%
)
```

## 兼容性与优先级

新增以下四个 XML 属性：

```xml
<attr name="shape_strokeGradientColors" format="reference" />
<attr name="shape_strokeGradientPositions" format="reference" />
<attr name="shape_strokeGradientAngle" format="float" />
<attr name="shape_strokeGradientEnableShadow" format="boolean" />
```

属性处理规则：

1. 三个新属性中颜色和位置同时存在、颜色数不少于 2、颜色数与位置数相同，且每个位置在 `0f..1f` 内并单调不减时，启用多停靠点边框渐变；角度为可选项。
2. 启用多停靠点模式后，边框绘制优先使用新属性，忽略旧的 `shape_strokeColor`、`shape_strokeGradientStartColor`、`shape_strokeGradientCenterColor`、`shape_strokeGradientEndColor`。
3. 启用多停靠点模式后始终使用新 Drawable，不使用旧的 `android:background` 图片背景；`shape_solidColor` 仍然生效，可用于半透明玻璃填充。若 View 因阴影、虚线或填充渐变需要软件图层且缓存超限，库会自动降级为“圆角 + 纯色背景 + 多停靠点描边”，不绘制图片、阴影、虚线和填充渐变。
4. 多停靠点模式默认保留 `shape_shadowSize` 造成的卡片内缩，但不绘制阴影；设置 `shape_strokeGradientEnableShadow="true"` 后才实际绘制阴影并进入软件层保护。缓存超限时阴影自动舍弃，内缩与多停靠点描边仍保留。
5. 新属性缺失、不完整或校验失败时，完全回退到当前 ShapeView 的旧逻辑，保证所有存量页面外观不变。

> `#AARRGGBB` 中的 `AA` 是透明度。`#33FFFFFF` 是 20% 白色，`#0DFFFFFF` 是 5% 白色，`#14FFFFFF` 是 8% 白色。

## XML 配置

在 `res/values/arrays.xml` 定义颜色与位置：

```xml
<resources>
    <!-- Figma 边框渐变颜色，顺序与停靠位置一一对应。 -->
    <array name="style_card_1_stroke_gradient_colors">
        <item>#33FFFFFF</item>
        <item>#0DFFFFFF</item>
        <item>#00FFFFFF</item>
        <item>#00FFFFFF</item>
        <item>#14FFFFFF</item>
    </array>

    <!-- 使用 string-array 保存浮点停靠位置，也兼容普通 array。 -->
    <string-array name="style_card_1_stroke_gradient_positions">
        <item>0.026</item>
        <item>0.1875</item>
        <item>0.5079</item>
        <item>0.7736</item>
        <item>0.8702</item>
    </string-array>
</resources>
```

在 `STYLE_CARD_1` 中接入：

```xml
<style name="STYLE_V1_STYLE_CARD_1">
    <item name="shape_radius">16dp</item>
    <item name="shape_shadowSize">6dp</item>
    <item name="shape_shadowColor">@color/style_v1_color_card_1_shadow_color</item>
    <item name="shape_solidColor">#991C1D22</item>
    <item name="shape_strokeSize">1dp</item>
    <!-- Figma 129.57 度换算为 Android 屏幕坐标约 39.57 度。 -->
    <item name="shape_strokeGradientAngle">39.57</item>
    <item name="shape_strokeGradientColors">@array/style_card_1_stroke_gradient_colors</item>
    <item name="shape_strokeGradientPositions">@array/style_card_1_stroke_gradient_positions</item>
    <item name="shape_type">rectangle</item>

    <!-- 多停靠点模式下库会忽略该旧图片背景；建议从新样式中移除。 -->
    <!-- <item name="android:background">@mipmap/bg_v1_card_1_16_d</item> -->
</style>
```

业务布局无需修改：

```xml
<com.hjq.shape.layout.ShapeLinearLayout
    style="@style/STYLE_V1_STYLE_CARD_1"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

## 库侧绘制实现

> 本工程已将 `ShapeDrawable 3.3` 源码作为 `library` 的源码目录合并，最终只发布一个 AAR，
> 不再依赖远程 `com.github.getActivity:ShapeDrawable:3.3`。描边的停靠位置必须保存在独立的
> `strokePositions` 字段中，不能复用填充渐变的 `positions`。位置资源支持 `array` 和 `string-array`。

库需要在 Shape Drawable 的边框绘制分支中，将颜色与位置传给 Android 的 `LinearGradient`：

```kotlin
// 使用多停靠点数据创建精确的线性渐变 Shader。
val strokeShader = LinearGradient(
    startX,
    startY,
    endX,
    endY,
    strokeGradientColors,
    strokeGradientPositions,
    Shader.TileMode.CLAMP
)

// 边框画笔保持 STROKE 模式，只替换其 Shader。
strokePaint.style = Paint.Style.STROKE
strokePaint.shader = strokeShader
canvas.drawPath(shapePath, strokePaint)
```

`startX`、`startY`、`endX`、`endY` 应根据控件尺寸和渐变方向计算。若需要精确支持 Figma 的 `129.57deg`，渐变角度 API 应支持 `Float`，不要只限定为 ShapeView 现有的离散方向枚举。

本地模块的角度坐标约定为 Android 屏幕坐标：`0` 度从左至右，`90` 度从上至下。Figma/CSS 导出的角度接入前需要按其导出坐标约定转换。

## 动态设置 API

建议在现有 `ShapeDrawableBuilder` 中增加如下链式 API：

```kotlin
fun setStrokeGradientColors(colors: IntArray): ShapeDrawableBuilder
fun setStrokeGradientPositions(positions: FloatArray): ShapeDrawableBuilder
fun setStrokeGradientAngle(angle: Float): ShapeDrawableBuilder
fun setStrokeGradientEnableShadow(enabled: Boolean): ShapeDrawableBuilder
```

Kotlin 调用示例：

```kotlin
val colors = intArrayOf(
    0x33FFFFFF, // 20% 白色
    0x0DFFFFFF, // 5% 白色
    0x00FFFFFF, // 完全透明
    0x00FFFFFF, // 完全透明
    0x14FFFFFF  // 8% 白色
)
val positions = floatArrayOf(0.026f, 0.1875f, 0.5079f, 0.7736f, 0.8702f)

shapeLinearLayout.getShapeDrawableBuilder()
    // 设置 Figma 导出的五个颜色停靠点。
    .setStrokeGradientColors(colors)
    .setStrokeGradientPositions(positions)
    // Figma 的 129.57 度换算为 Android 屏幕坐标约 39.57 度。
    .setStrokeGradientAngle(39.57f)
    // 将配置重新应用为控件背景。
    .intoBackground()
```

Java 调用示例：

```java
int[] colors = new int[] {
        0x33FFFFFF, // 20% 白色
        0x0DFFFFFF, // 5% 白色
        0x00FFFFFF, // 完全透明
        0x00FFFFFF, // 完全透明
        0x14FFFFFF  // 8% 白色
};
float[] positions = new float[] {0.026f, 0.1875f, 0.5079f, 0.7736f, 0.8702f};

shapeLinearLayout.getShapeDrawableBuilder()
        // 设置 Figma 导出的五个颜色停靠点。
        .setStrokeGradientColors(colors)
        .setStrokeGradientPositions(positions)
        // Figma 的 129.57 度换算为 Android 屏幕坐标约 39.57 度。
        .setStrokeGradientAngle(39.57f)
        .intoBackground();
```

动态 API 应在 `setStrokeGradientColors` 或 `setStrokeGradientPositions` 调用时执行相同校验；校验失败时静默退出多停靠点模式并回退旧描边逻辑，不能向业务方抛出异常。

## 发布步骤

1. 在 `TSsimeon/ShapeView` 中实现新属性、解析、校验、绘制与动态 API。
2. 创建 Git Tag，例如 `v9.4.8`，由 JitPack 构建 `com.github.TSsimeon:ShapeView:v9.4.8`。
3. 将本项目的 `BaseDependencies.shape_view_tsimeon` 升级到 `v9.4.8`。
4. 在 `moduleCoreC:Style` 的 `STYLE_V1_STYLE_CARD_1` 使用新属性，并将 Style 库发布为新版本，例如 `0.3.6`。
5. 在示例页验证黑、绿、紫三种背景下的描边，确认旧卡片样式和按钮样式无回归。

## 验收清单

- 五个停靠点在黑色背景下能观察到“亮 → 弱亮 → 透明 → 透明 → 弱亮”的边框变化。
- 未添加新属性的页面与升级前截图一致。
- 仅配置颜色或仅配置位置时安全回退旧逻辑。
- 颜色数与位置数不一致时，动态 API 和 XML 配置都静默回退旧逻辑，不向业务方抛出异常。
- `STYLE_CARD_1` 的半透明 `shape_solidColor` 仍然可见，旧图片背景不会叠加。
