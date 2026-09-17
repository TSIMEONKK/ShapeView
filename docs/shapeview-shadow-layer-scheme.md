# ShapeView 硬件阴影方案分析（shape_shadowHardware / setShadowLayer）

## 1. 背景与目标

当前 ShapeView 的阴影通过 `BlurMaskFilter` + 独立阴影画笔（`mShadowPaint`）实现。
`MaskFilter` 系 API 在硬件加速（hwui）下**任何版本都不被支持**，所以只要配置了阴影（或虚线），
View 就必须切换到软件图层（`LAYER_TYPE_SOFTWARE`，俗称"软解"）才能正常显示，带来性能与稳定性问题。

目标一：新增 `shape_shadowHardware` 属性开关，开启后改用 `Paint.setShadowLayer()` 绘制阴影：

- **API >= 28**：硬件加速原生支持非文本的 `setShadowLayer`，全程硬绘，不使用软解；
- **API < 28 的硬件 Canvas**：`setShadowLayer` 在硬件加速下**仅对文本生效**，而 ShapeDrawable
  绘制的全部是非文本形状，硬绘拿不到阴影；`DashPathEffect` 也不能可靠绘制。此时保留阴影占位、
  不绘制阴影与虚线，其余逻辑保留。若最终 Canvas 本身是软件 Canvas，则两种效果均可正常软件绘制；
  新方案不会为此主动创建软件层。

目标二（根治）：将临时内部字段 `shadowInsetSize` 扶正为 XML 属性 `shape_shadowInsetSize`，
解耦 `shape_shadowSize` 的双重职责（阴影大小 + 占位尺寸）。为保证旧模式零行为变化，
`shape_strokeGradientEnableShadow` 与多停靠点旧规则继续保留；仅新开关开启时不采用该旧规则（见 3.7）。

## 2. 现状分析

### 2.1 阴影绘制链路

```
XML 属性(shape_shadowSize / shape_shadowColor / shape_shadowOffsetX / shape_shadowOffsetY)
  -> 各 Styleable 类(IShapeDrawableStyleable 实现)
  -> ShapeDrawableBuilder(读取 TypedArray，refreshShapeDrawable 应用)
  -> ShapeDrawable.draw()(真正绘制)
```

`ShapeDrawable.draw()` 中的旧阴影实现（`ShapeDrawable.java:564-598`）：

- `mShadowPaint`：STROKE 风格、透明色强制 alpha 254、strokeWidth 取描边宽度或 `shadowSize / 4`；
- `BlurMaskFilter(radius, Blur.NORMAL)`，radius = `shadowSize / 2`（API >= 28）或 `shadowSize / 3`（< 28）；
- 每种形状（矩形 / 圆角矩形 / 椭圆 / 线条 / 圆环）先画一遍阴影形状（`mShadowRect` / `mShadowPath`），
  再画填充色和描边。

几何预留（`ShapeDrawable.java:876-906`，`ensureValidRect()`）：

- 形状区域按 `max(shadowSize, shadowInsetSize)` 四周内缩，保证阴影不超出 View 边界被裁剪；
- `mShadowRect` 在此基础上按 `shadowOffsetX / shadowOffsetY` 做非对称偏移。

### 2.2 软解的触发点（共两处）

1. **`ShapeDrawable.intoBackground(View)`**（`ShapeDrawable.java:481-484`）：
   `strokeDashGap > 0 || shadowSize > 0` 时直接 `view.setLayerType(LAYER_TYPE_SOFTWARE, null)`。
   代码方式构建 Drawable 时走这里。

2. **`ShapeDrawableBuilder.intoBackground()`**（`ShapeDrawableBuilder.java:1207-1228`）：
   `isStrokeDashLineEnable() || isShadowRenderEnable() || isSolidGradientColorsEnable()`
   （虚线 / 实际绘制阴影 / 填充渐变）时，`post` 到下一帧计算 `isOverLargeCache()`：
   - 未超限：设软件图层（状态 0）；
   - 超限：恢复原图层并使用安全降级背景（状态 1，`buildSafeBackgroundDrawable()`，
     只保留圆角 + 纯色 + 多停靠点描边，舍弃阴影 / 虚线 / 填充渐变）。

其中 `isShadowRenderEnable()`（`ShapeDrawableBuilder.java:745-748`）是"多停靠点默认只占位不画阴影"
的特殊规则（配合 `shape_strokeGradientEnableShadow`）。为保证旧模式零行为变化，旧分支继续使用它；
新开关分支绕开该规则并直接透传原始阴影参数。

### 2.3 软解的代价

- View 整体退回 UI 线程软件光栅化，位图内存 = 宽 x 高 x 4 字节，滚动 / 动画性能差；
- 软件缓存超过 `ViewConfiguration.getScaledMaximumDrawingCacheSize()` 时内容直接不绘制（白块），
  现有代码被迫做了一套安全降级兜底（`isOverLargeCache()` + `buildSafeBackgroundDrawable()`）；
- 无法使用任何硬件渲染特性（如 RenderThread 特效）。

### 2.4 API 28 分界的技术事实（方案依据）

| 特性 | API < 28（硬件加速） | API >= 28（硬件加速） | 软件图层（任意 API） |
|---|---|---|---|
| `Paint.setShadowLayer()` 非文本形状 | 不生效（仅文本生效） | **生效**（Android P 起 hwui 支持） | 生效（Skia 软件实现，一直支持） |
| `DashPathEffect` / `PathEffect` | 不生效，退化成实线（错误视觉） | **生效**（Android P 起 hwui 支持） | 生效 |
| `BlurMaskFilter` / `MaskFilter` | 不生效 | 仍然不生效（只能软件绘制） | 生效 |

> 注意："仅对文本生效"的限制**只存在于硬件加速下**。API < 28 时如果切软件图层，
> `setShadowLayer` 画非文本阴影同样生效——也就是说 28 以下"做得到"，但代价恰恰是软解本身。
> 本方案的目标是去软解，所以不做此选择。

**已确认的决策**：新开关按最终 Canvas 能力绘制。硬件 Canvas 且 API >= 28 绘制硬件阴影与虚线；
硬件 Canvas API < 28 仅降级平台不支持的非文本阴影与虚线；软件 Canvas 在任意 API 都可完整绘制。
无论哪种情况，新开关均不由本库主动启用软件图层。

### 2.5 `shadowInsetSize` 的历史调研结论

`shadowInsetSize` 由 commit `d0e3780`（2026-09-03，多停靠点描边方案）一次性引入，
是**纯内部字段**——attrs.xml、`IShapeDrawableStyleable`、`ShapeDrawableBuilder` 均无对应 XML 入口，
业务侧始终无法直接声明"我只要占位"。当前仅两处内部使用：

1. `refreshShapeDrawable()`：多停靠点模式下 `drawable.setShadowInsetSize(mShadowSize)`，
   借 `shadowSize` 的值承接占位（本方案要根治的 hack）；
2. `buildSafeShapeDrawable()`：软解缓存超限降级时保留占位。

它是在多停靠点方案里为"默认占位不画阴影"临时设计的承接机制，现扶正为正式 XML 属性。

## 3. 方案设计

### 3.1 属性体系变化

| 属性 | 变化 | 根治后职责 |
|---|---|---|
| `shape_shadowSize` | 新模式语义扩展 | 新开关开启时表示**阴影意图 + 大小**，半径采用混合映射（见 3.3）：无偏移时**直传全量**（所见即所得，4dp = 4dp 模糊），有偏移时按旧方案 API 分档（28+ 取 S/2、< 28 取 S/3）；旧模式仍保留多停靠点的既有“默认仅占位”语义 |
| `shape_shadowInsetSize` | **新增 XML 属性**（内部字段已有） | **纯占位**：配了只内缩形状区域、永不画阴影 |
| `shape_shadowHardware` | **新增** | 阴影渲染方式：`false`（默认）走旧 BlurMaskFilter 软解；`true` 走 setShadowLayer 硬绘（28+）/ 占位（< 28） |
| `shape_strokeGradientEnableShadow` | 旧模式保留 | 继续维持既有多停靠点兼容语义；新开关开启时不参与阴影决策 |

```xml
<!-- 阴影是否走硬件渲染方案：28+ 用 setShadowLayer 绘制，28 以下仅保留占位；
     默认 false，走旧 BlurMaskFilter 软解方案 -->
<attr name="shape_shadowHardware" format="boolean" />

<!-- 阴影占位大小：只内缩形状区域保留留白，不绘制阴影、不触发软解 -->
<attr name="shape_shadowInsetSize" format="dimension" />
```

组合规则：

- 新开关开启时，`shadowSize` 与 `shadowInsetSize` 可共存，语义为**包含关系**：`shadowInsetSize`
  表示**总留白**，阴影绘制在留白之内；占位（形状内缩）= `max(shadowSize, shadowInsetSize)`
  （常规偏移下的基础逻辑）；若阴影偏移超过基础留白，新模式会仅补足对应侧的最小空间，避免裁剪；
- 核心特性：**渐进启用阴影不改布局**——已占位的卡片（insetSize=I）后续加开阴影
  （shadowSize ≤ I 时）总占位仍为 I，卡片尺寸稳定，阴影画进既有留白；
- 边缘行为：`inset < shadow` 时被阴影吸收（留白下限已被阴影需求超越，配了无额外效果，
  属"总留白"语义的合理推论，文档明示即可）；
- 旧模式不读取新的 `shadowInsetSize`，继续沿用原有多停靠点占位和安全降级逻辑，保证存量零变化；
- 新模式提供“纯占位”能力：仅配置 `shadowInsetSize` 时不画阴影，对业务是纯增量。

### 3.2 行为矩阵

| 新属性 | API | 阴影绘制（`shadowSize > 0` 时） | 占位（内缩） | 软解 | 虚线 | 其他（填充 / 渐变 / 描边 / 圆角） |
|---|---|---|---|---|---|---|
| false（默认） | 任意 | 旧 BlurMaskFilter 方案 | max(shadowSize, insetSize) | 触发（现状） | 软解下绘制（现状） | 现状 |
| true | 硬件 Canvas，API >= 28 | setShadowLayer 硬绘 | 基础 `max(shadowSize, insetSize)`，再按四侧 `layoutRadius ± offset` 补足 | **不使用** | 硬件正常绘制 | 正常绘制 |
| true | 硬件 Canvas，API < 28 | **不绘制**（平台不能实现） | 与 API >= 28 保持相同的四侧留白，避免降级时内容区域变化 | **不使用** | 仅实际配置了虚线段时整条不画 | 正常绘制 |
| true | 软件 Canvas，任意 API | setShadowLayer 软件绘制 | 与 API >= 28 保持相同的四侧留白 | 不额外设置软件图层 | 正常绘制 | 正常绘制 |

> 注 1：填充渐变已确认随新属性一并豁免软解（渐变走硬绘，`rInto` 的 `setDither(true)`
> 单独保留防色带），故"true"行的"不使用软解"对渐变组合同样适用。
> 注 2：虚线"整条不画"仅发生于 API < 28 的硬件 Canvas，且仅指 `shape_strokeDashSize > 0`
> 的描边整条不画；`shape_strokeDashGap` 单独配置时仍按实线绘制。
> 纯实线描边与软件 Canvas 下的虚线均正常绘制。
> 注 3：`shadowInsetSize` 单独配置（未配 `shadowSize`）时，任何 API 等级行为一致——仅内缩，无阴影。

说明：占位内缩在所有分支下都保留，卡片视觉尺寸与旧方案一致，
同时 28+ 的硬件阴影也正好绘制在预留的内缩区域内，不会被 View 边界裁掉。

**阴影半径的混合映射（已定稿）**：新模式下 `setShadowLayer` 的半径不统一取 `shadowSize / 2`，
而是按偏移状态分流——**无偏移直传全量，有偏移沿用旧方案 API 分档**（规则与依据见 3.3）。
占位采用基础值加方向性补足（`max(shadowSize, shadowInsetSize, ceil(layoutRadius ± offset))`），
且无偏移时 `radius = shadowSize` 与 max 中的 `shadowSize` 分支恒相等，
radius 项永远不会额外放大留白，`shadowInsetSize` 的共存语义（纯占位 / 渐进稳定 / 总额吸收）零变化。

### 3.3 API >= 28 的绘制方式

不再单独画"阴影形状"，而是按实际绘制内容把 `setShadowLayer` 挂到对应画笔上，
一次 `drawXxx` 同时完成"形状 + 阴影"（偏移由 `setShadowLayer` 的 dx / dy 参数承担，
替代旧方案的 `mShadowRect` 手动偏移）。`haveFill / haveStroke` 是 `draw()` 的现成局部变量
（`ShapeDrawable.java:512-513`），纯描边无需退化为占位。

**画笔状态隔离（必须实现）**：`Paint` 在 Drawable 内会跨帧复用，ShadowLayer 也会残留。
每次进入硬件阴影分支前必须先清除 `mSolidPaint` 和 `mStrokePaint` 两者的 ShadowLayer，再只给
本帧实际投影源设置阴影；不能只在“没有阴影”的分支清理。否则从“纯描边”切到“填充 + 描边”
（或反向）时，未选中的画笔会携带上一帧的阴影继续绘制，造成双重阴影。

**形状选择规则（必须以实际绘制调用为准）**：LINE 仅使用描边画笔；RING 当前先以
`mSolidPaint` 绘制 ring path、再可选地使用 `mStrokePaint` 描边，不能把 RING 一概视作描边形状。
填充与描边同时存在时，本方案以填充画笔作为投影源；这与旧方案的描边投影并非像素等价，宽描边、
圆角及 RING 必须纳入视觉回归。

```java
// 新方案：API 28+ 硬件加速原生支持非文本 setShadowLayer
if (haveShadow && mShapeState.shadowHardware) {
    // 混合映射：无偏移直传全量；有偏移按旧方案 API 分档（规则见下方）
    float radius = resolveShadowBlurRadius();
    if (haveFill) {
        // 有填充：挂填充画笔，一次绘制完成形状 + 满铺阴影
        mSolidPaint.setShadowLayer(radius,
                mShapeState.shadowOffsetX,   // 偏移由系统处理，不再手动平移阴影矩形
                mShapeState.shadowOffsetY,
                shadowColor);                // 阴影深浅由 shadowColor 的 alpha 控制
    } else if (haveStroke) {
        // 纯描边：挂描边画笔，产生框状阴影
        // 旧方案 mShadowPaint 本就是 STROKE 风格（框的模糊），两者视觉同构
        mStrokePaint.setShadowLayer(radius,
                mShapeState.shadowOffsetX, mShapeState.shadowOffsetY, shadowColor);
    }
}
```

注意细描边（如 1dp）时阴影投射源很细、hwui 阴影偏淡，联调时如观感不足仅对描边分支适当增大 radius。

**半径映射规则（混合映射，已定稿）**：绘制使用 `resolveShadowBlurRadius()`；
`ensureValidRect()` 另使用跨 API 稳定的 `resolveShadowLayoutRadius()`。两者分离的原因是：
API < 28 硬件 Canvas 虽会跳过阴影，但不能因此改变卡片内容区域。

```java
/** 硬件阴影模糊半径：无偏移直传；有偏移参考旧 BlurMaskFilter 的 API 分档 */
private float resolveShadowBlurRadius() {
    if (!mShapeState.shadowHardware) {
        return mShapeState.shadowSize / 2f;   // 旧模式占位（legacy 路径实际不消费）
    }
    // 无偏移：直传，所见即所得（4dp = 4dp）
    if (mShapeState.shadowOffsetX == 0 && mShapeState.shadowOffsetY == 0) {
        return mShapeState.shadowSize;
    }
    // 有偏移：完整继承旧逻辑分档，保留边界安全余量
    // isBelowApi28() 含调试开关，便于在 28+ 测试机模拟 < 28 档位对比
    return isBelowApi28() ? mShapeState.shadowSize / 3f : mShapeState.shadowSize / 2f;
}

/** 占位半径不跟随 API / 调试档位变化，保证降级前后几何一致。 */
private float resolveShadowLayoutRadius() {
    if (!mShapeState.shadowHardware) return mShapeState.shadowSize / 2f;
    if (mShapeState.shadowOffsetX == 0 && mShapeState.shadowOffsetY == 0) {
        return mShapeState.shadowSize;
    }
    return mShapeState.shadowSize / 2f;
}
```

| Canvas / API | offset | radius | 占位 |
|---|---|---|---|
| 任意可达路径 | 无 | **shadowSize（直传）** | 恰好 = max(shadowSize, insetSize)，radius 项不额外扩张 |
| 硬件 28+ | 有 | shadowSize / 2 | 余量 shadowSize/2 吸收 offset，与旧方案观感逐档一致 |
| 软件 Canvas，< 28 | 有 | shadowSize / 3（保守档） | 按固定 `layoutRadius = shadowSize / 2` 预留，几何与 28+ 一致 |
| 软件 Canvas，≥ 28 | 有 | shadowSize / 2 | 按固定 `layoutRadius = shadowSize / 2` 预留 |
| 硬件 < 28 | — | 不绘制 | 按固定 `layoutRadius` 预留，降级不改变内容区域 |

混合映射的三个特性声明：

1. **所见即所得**：无偏移（最主流的对称阴影）时 4dp 配置即 4dp 模糊。旧模式下同配置观感为
   2dp，业务迁移时需将 `shadowSize` **减半**以保持旧观感（发布说明需写明该护栏）；
2. **有偏移视觉等价**：有偏移场景与旧方案同 API 同档视觉一致，迁移零观感差异；
3. **偏移跳档**：offset 在 0 ↔ 非 0 之间切换时半径会在 `shadowSize` 与 `shadowSize/2`（或 /3）
   之间跳变。现有 `setShadowOffsetX/Y` 已置 `mPathDirty/mRectDirty + invalidateSelf`，
   跳档后几何自动重算，无需额外处理。

尾部校准预案：直传档余量为 0，高斯衰减尾部（<1% 能量）理论上可能微弱越界；`ceil` 已兜底整数化，
若真机目视仍有裁切，在 `resolveShadowBlurRadius()` 返回值上加 `0.5f` epsilon 即可（单点修改）。

`useLayer` 分支的 `saveCanvasLayer()` 边界也必须覆盖阴影：现有边界只向外扩了描边宽度；
若硬件阴影绘制进该 layer，超出边界的模糊与偏移部分会被裁切。应按
`strokeWidth + resolveShadowBlurRadius() + max(abs(shadowOffsetX), abs(shadowOffsetY))`
扩张相应边界（实现沿用原有的描边宽度全额口径，较 strokeWidth/2 更保守），半径必须与挂载处同源。

### 3.4 API < 28 硬件 Canvas 的能力降级方式

**降级规则**：只在硬件 Canvas 的 API < 28 分支，降级平台明确不支持的非文本
`setShadowLayer` 与线条 `PathEffect`。软件 Canvas 对两者都能正确绘制，必须完整保留。
Builder 不能提前清空 `shadowSize` 或 `strokeSize`，以保持占位与描边 inset 的几何稳定；最终只在
`ShapeDrawable.draw(Canvas)` 根据 Canvas 能力跳过对应绘制调用。

最终判断必须在 `ShapeDrawable.draw(Canvas)` 完成：

```java
boolean hardwareBelow28 = mShapeState.shadowHardware
        && canvas.isHardwareAccelerated() && isBelowApi28();

// Builder 始终保留原始几何与绘制参数：shadowSize、strokeSize、DashPathEffect 均不清空。
// API < 28 的硬件 Canvas：跳过阴影；若配置虚线，跳过本次整条描边绘制。
// 不修改 strokeSize，因此仍保留原有描边 inset，避免卡片内容区因降级而变化。
boolean drawHardwareShadow = mShapeState.shadowHardware && !hardwareBelow28;
boolean skipDashedStroke = hardwareBelow28 && mShapeState.strokeDashSize > 0;
```

软件 Canvas（任意 API）下 `hardwareBelow28` 为 false，`setShadowLayer` 与虚线均照常通过软件管线绘制；
不额外设置 `LAYER_TYPE_SOFTWARE`。

**代码方式构建也必须走同一降级策略**：不能只在
`ShapeDrawableBuilder.refreshShapeDrawable()` 中清除 API < 28 的虚线，因为
`new ShapeDrawable().setShadowHardware(true).intoBackground(view)` 会绕过 Builder。
应将 API、调试开关和绘制能力判断收敛至 `ShapeDrawable` 的共享策略中，使 XML 与代码方式
共用同一个行为矩阵与测试集。

### 3.4.1 Canvas 硬件加速状态与 `isOverLargeCache`

API 等级只表示平台具备能力，不能证明当前 `draw()` 拿到的是硬件 Canvas；最终绘制阶段还应检查
`canvas.isHardwareAccelerated()`。但 Canvas 为软件状态时，**不应自动进入 `isOverLargeCache()`**：

- `isOverLargeCache()` 的历史用途是库主动调用 `setLayerType(LAYER_TYPE_SOFTWARE)` 后，为该 View
  额外创建的软件位图层做容量保护；
- 若 Application、Activity 或 Window 本身就是软件渲染，或当前 Canvas 是软件离屏 Canvas，库没有新建
  这块 View 软件层，不能把该启发式阈值直接用于安全降级，否则会无故丢失阴影、虚线或渐变；
- 若宿主主动为该 View 设置了 `LAYER_TYPE_SOFTWARE`，软件缓存是宿主策略。库可以提供诊断信息，
  但不应擅自改层类型或替换宿主视觉；
- 只有库为了旧 `BlurMaskFilter` 路径主动请求软件图层时，才保留现有 `post → isOverLargeCache() → 安全降级`
  链路。

Canvas 为软件状态且新属性开启时，直接使用 `setShadowLayer` 的软件实现，完整保留阴影与虚线。
**仅当本次绘制未由库主动设置 `LAYER_TYPE_SOFTWARE` 时**，才不走 `isOverLargeCache()`。

换言之，是否检查 `isOverLargeCache()` 的唯一判据不是 `canvas.isHardwareAccelerated()`，而是
**库是否即将或已经为该 View 主动创建 `LAYER_TYPE_SOFTWARE` 软件位图层**：

- 是：必须先按现有 `post → isOverLargeCache() → 安全降级` 流程判断，避免库创建超限缓存；
- 否：不走该流程。全局软件 Window、宿主自设软件层、软件 Bitmap Canvas 都不是库可安全接管的
  “新建软件层”场景；其中宿主自设层即使改用安全背景也仍保留同一软件层，检查后无法从根源消除超限。

**`android:layerType="software"` 与新开关同时配置时的特殊规则**：两者目标冲突。用户已确认
新开关优先，Builder 检测到 View 当前为 `LAYER_TYPE_SOFTWARE` 后，直接将该 View 恢复为
`LAYER_TYPE_NONE`，而不是保留软件层再做 `isOverLargeCache()` 检查。**不使用安全背景替换当前
Drawable**：移除本地软件层后，应按本方案的 API/Canvas 能力矩阵继续绘制，尽可能保留填充、填充渐变、
圆角、普通描边及可支持的阴影/虚线；只有硬件 Canvas API < 28 不支持的阴影、虚线才单独降级。
这样真正释放本地 View 软件图层，同时避免“为消除一个冲突而无差别丢弃其他可正确绘制效果”。

**实现约束（必须满足）**：`ShapeDrawableBuilder` 当前在构造时把原始 `layerType` 保存为字段，
而 `rInto()` 在非软解分支会恢复该字段。仅在入口调用一次 `setLayerType(NONE)` 是不够的——
后续 `rInto()` 会把 XML 的 SOFTWARE 又设回去。新开关应在 `rInto()` 统一计算最终图层类型：

```java
// 新开关优先，不允许恢复构造时从 XML 读取到的软件层；旧模式完全保留原逻辑。
if (lastShowState == 0) {
    mView.setLayerType(View.LAYER_TYPE_SOFTWARE, null); // 仅旧模式的既有软解分支可达
} else if (mShadowHardware && mView.getLayerType() == View.LAYER_TYPE_SOFTWARE) {
    // 只解除软件层，保留宿主主动创建的 LAYER_TYPE_HARDWARE。
    mView.setLayerType(View.LAYER_TYPE_NONE, null);
} else if (!mShadowHardware) {
    mView.setLayerType(layerType, null);
}
```

代码方式 `ShapeDrawable.intoBackground(View)` 也必须采用同一优先级：新开关开启且 View 当前为
`LAYER_TYPE_SOFTWARE` 时，先恢复 `LAYER_TYPE_NONE`，再设置背景，不能只修改 Builder 链路。

此规则意味着：需要软件层来支持 View 其他自定义绘制效果的业务，不能同时启用
`shape_shadowHardware="true"`；应保持新开关关闭并由宿主自行管理软件层及超限策略。

Drawable/View 不能把软件 Window 强行升级为硬件 Window：View 层只能关闭硬件加速，不能启用它。
宿主如需 GPU 路径，应在 Manifest 的 `<application>` / `<activity>` 配置
`android:hardwareAccelerated="true"`，或在 Window 创建早期设置 `FLAG_HARDWARE_ACCELERATED`。

### 3.5 软解分支的豁免

`ShapeDrawableBuilder.intoBackground()` 与 `ShapeDrawable.intoBackground(View)`
的软解条件在**新属性开启时整体不成立**。`isShadowRenderEnable()` 保留给旧模式的多停靠点兼容逻辑，
新模式不进入该条件：

```java
// 新属性开启时：阴影走 setShadowLayer 或占位，虚线 28+ 硬绘 / 28 以下不绘制，
// 均不需要软件图层，也不再进入 post + isOverLargeCache + 安全降级流程
if (!mShadowHardware && (isStrokeDashLineEnable() || isShadowRenderEnable()
        || isSolidGradientColorsEnable())) {
    // ... 现有软解逻辑原样保留 ...
}
```

附带收益：新属性开启且宿主为硬件 Window 的 View 不会因本库主动设置软件图层而命中
`isOverLargeCache()` 安全降级（该特定大 View 白块路径随之消失），
`onSizeChanged()` 的重刷也随之**不再必要**（见下）。

**`onSizeChanged()` 重刷豁免**：13 个 View / Layout 类（`2431353` 引入）在尺寸变化时
重新调用 `intoBackground()`，其历史动机是重新评估 `isOverLargeCache()`——View 动态变高
（如 RecyclerView item 复用）后软解缓存超限判定可能翻转，需切换安全降级背景。
新属性开启后：软解分支不存在、`isOverLargeCache()` 永不被调用，没有任何依赖尺寸的决策；
Drawable 的尺寸适配由系统机制天然覆盖（View 尺寸变化 → `setBackground` 的 Drawable
自动 `setBounds` → `ShapeDrawable.onBoundsChange()` 置 dirty 标记 → 下次 `draw()` 时
`ensureValidRect()` 重算几何与渐变 shader）。因此 View 层加条件跳过重刷：

```java
if ((oldw != 0 && w != oldw) || (oldh != 0 && h != oldh)) {
    // 新属性开启时通常无需重建；但外部若重新设置软件层，需在此入口执行显示优先降级。
    if (!mShapeDrawableBuilder.isShadowHardware()
            || getLayerType() == View.LAYER_TYPE_SOFTWARE) {
        mShapeDrawableBuilder.intoBackground();
    }
}
```

> 等价性说明：对新属性开启且未被外部重新设为软件层的实例，这段重写**等价于不存在**（只留 `super.onSizeChanged`
> 行为完全一致：`setBounds → onBoundsChange → ensureValidRect` 链路自动完成几何与
> 渐变适配，尺寸变化本身必然触发重绘）。但不能**物理删除**——一个类静态服务两种配置，
> 新属性 off 的存量实例仍依赖重刷（软解卡动态变高后 `isOverLargeCache()` 判定可能翻转，
> 不重刷会白块）；且"是否跳过"只能在调用方判断（`intoBackground()` 无法区分调用来源，
> 构造 / `setEnabled` 也调用它且新属性 on 时仍必须执行）。物理删除的时机是未来业务
> 全面迁移、旧软解路径废弃时，13 个 `onSizeChanged` 重写可整体移除（长期清理项，
> 不在本次范围）。

**显示优先的图层冲突处理**：新开关开启时，不以 `isOverLargeCache()` 的结果决定是否保留
`android:layerType="software"`，因为该结果只是估算，无法保证避免白块。每个 `intoBackground()`
入口（构造、`setEnabled`、以及尺寸变化后的入口）都按以下确定性规则处理：

1. 若当前 `mView.getLayerType() == LAYER_TYPE_SOFTWARE`，立即设置为 `LAYER_TYPE_NONE`；
2. 保持当前 Drawable，随后按 API/Canvas 能力矩阵重绘：可正确绘制的填充、渐变、圆角、普通描边
   必须保留；仅阴影和虚线按能力边界单独取舍；
3. 不创建软件层、不进入 `post + isOverLargeCache()`；
4. 后续 `onSizeChanged()` 不因尺寸在不同背景之间来回切换，只需在入口处再次检查是否有外部代码
   重新设置了软件层并重复执行上述图层恢复。

**异步回调隔离**：旧模式的缓存检查通过 `post` 延后执行。每次 `intoBackground()` 都递增背景请求版本；
回调执行前必须确认版本仍一致且新开关仍关闭，否则直接丢弃。这样业务在首帧或运行时切换到
`shape_shadowHardware` 后，先前排队的旧模式回调不会重新写入 `LAYER_TYPE_SOFTWARE` 或旧背景。

这是一条“可显示优先于保留显式软件图层”的规则，而不是“可显示优先于保留所有视觉效果”。
业务若必须保留该 View 的软件层，应关闭 `shape_shadowHardware`，回到旧模式并接受旧模式的缓存保护。

`buildSafeShapeDrawable()`（新属性 off 时的降级路径）占位保持 max 语义不变
（多停靠点分支 `setShadowInsetSize(mShadowSize)` 现状写法即 max，仅确认无需改动）。

注意：`rInto()` 末尾的 `setDither(true)` 保留现有条件，不属于本方案的能力判断范围；
新开关不因该设置创建软件图层或进入安全降级。

### 3.6 调试开关（强制模拟 API < 28 分支）

为了在 28+ 测试机上直接对比"28+ 硬绘 / < 28 降级"两种表现，
调试开关实际收敛在 `ShapeDrawable`，使 XML Builder 与直接 `new ShapeDrawable()` 两个入口共用
同一能力判断；`ShapeDrawableBuilder` 只提供同名静态委托方法，供 Demo 调用（不新增 XML 属性，避免污染业务布局）：

```java
/** 调试开关：在硬件 Canvas 上强制按 API < 28 的能力降级处理。 */
private static volatile boolean sForceBelowApi28ForDebug;

public static void setForceBelowApi28ForDebug(boolean enabled) {
    sForceBelowApi28ForDebug = enabled;
}

public static boolean isForceBelowApi28ForDebug() {
    return sForceBelowApi28ForDebug;
}
```

SDK 判断统一收敛为一个方法，调试开关只需在这处生效：

```java
/** 是否按 API < 28 处理：真机低于 28，或调试开关强制开启 */
private static boolean isBelowApi28() {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.P || sForceBelowApi28ForDebug;
}

// draw(Canvas) 中的最终判断：软件 Canvas 仍保留可实现的效果。
boolean hardwareBelow28 = mShapeState.shadowHardware
        && canvas.isHardwareAccelerated() && isBelowApi28();
```

说明：

1. 强制开关只模拟“API < 28 的硬件 Canvas 不支持”这一能力边界；在 28+ 设备的硬件 Canvas 上
   开启后，阴影仅占位、虚线整条不画。软件 Canvas 仍按能力优先规则完整绘制；
2. 开关是**进程级静态**状态，影响当前页面所有**开启了新属性**的 ShapeView 实例
   （`mShadowHardware && isBelowApi28()` 在未开启时短路，旧方案卡片不受影响），便于整页对比；
3. 修改开关后调用目标 View 的 `invalidate()` 触发下一帧绘制即可生效；
   测试时建议在 `setContentView` 之前设置，便于整页对比；
4. 仅调试使用，默认 `false`，正式包行为不受影响（无需 Proguard 混淆处理，
   开关不被业务引用即无副作用）。

### 3.7 多停靠点描边交互（根治后）

多停靠点模式按开关分流：**旧模式**继续使用 `isShadowRenderEnable()` 与
`shape_strokeGradientEnableShadow`，保证现有页面零变化；**新开关开启时**忽略该旧规则，
`shadowSize` 表达阴影意图，`shadowInsetSize` 表达纯占位，最终由 3.2 的 Canvas/API 能力矩阵决定渲染。

与多停靠点相关的仅剩两条通用规则：

1. **虚线口径覆盖多停靠点描边**：仅在 `< 28 + 硬件 Canvas + 新属性 on + 虚线` 时，
   跳过本次整条描边绘制（不修改 `strokeSize`，保留原有几何 inset）；多停靠点的渐变描边也遵循该规则。
   软件 Canvas 与 28+ 硬件 Canvas 均正常绘制；
2. **描边渐变本身硬绘友好**：LinearGradient 硬件加速一直支持，多停靠点描边不触发软解、
   不参与安全降级取舍（`buildSafeShapeDrawable()` 保留多停靠点描边的既有行为不变）。

这样新功能可采用清晰语义，而不破坏已上线的多停靠点页面；旧规则的清理应留待未来主版本，
并在完成业务迁移后单独执行。

## 4. 代码改动清单

| # | 文件 | 改动 |
|---|---|---|
| 1 | `library/src/main/res/values/attrs.xml` | **+** `shape_shadowHardware`、`shape_shadowInsetSize`；同步全部 **13 个** `declare-styleable`，既有 `shape_strokeGradientEnableShadow` 保留不动 |
| 2 | `IShapeDrawableStyleable.java` | **+** `default int getShadowHardwareStyleable() { return -1; }`、`default int getShadowInsetSizeStyleable() { return -1; }`；既有多停靠点接口保留 |
| 3 | 13 个 Styleable 实现类 | **+** `R.styleable.Xxx_shape_shadowHardware` 与 `_shape_shadowInsetSize` 的 override |
| 4 | `ShapeDrawableBuilder.java` | **+** 字段 `mShadowHardware`（含 `setShadowHardware() / isShadowHardware()`）、`mShadowInsetSize`（含 setter/getter）、调试开关的静态委托；旧模式继续保留 `mStrokeGradientEnableShadow`、`isShadowRenderEnable()` 与原有软解条件；新开关才透传原始阴影/虚线参数、绕开 `isOverLargeCache()`，并在 `rInto()` 仅解除 `LAYER_TYPE_SOFTWARE`、保留宿主硬件层 |
| 5 | `ShapeState.java` | **+** `public boolean shadowHardware;` 字段，拷贝构造同步复制（`shadowInsetSize` 字段已有，无需新增） |
| 6 | `ShapeDrawable.java` | **+** `setShadowHardware(boolean)` 链式 API（含脏标记）；**+** `resolveShadowBlurRadius()` 混合映射用于 `draw()` 与 `saveLayer` 扩张；**+** `resolveShadowLayoutRadius()` 用于 `ensureValidRect()` 的跨 API 稳定四侧留白；`draw()` 增加基于 `canvas.isHardwareAccelerated()` 与 API 的最终能力分支，并每帧隔离两个 Paint 的 ShadowLayer；`intoBackground(View)` 在新开关开启时**仅解除** `LAYER_TYPE_SOFTWARE`（条件式，不覆盖宿主 `LAYER_TYPE_HARDWARE`）、绝不主动设 SOFTWARE |
| 7 | 13 个 Shape View / Layout 类 | `onSizeChanged()` 中仅当旧模式，或检测到外部重新设置了 `LAYER_TYPE_SOFTWARE` 时调用 `intoBackground()`；`setEnabled` 等其余入口不动 |
| 8 | 文档与 Demo | 旧多停靠点属性继续保留；补充新开关与旧模式的分流说明即可 |

`refreshShapeDrawable()` 的新模式目标形态（示意）：不在 Builder 按 SDK 清空任何效果；
最终能力取舍见 3.4 的 `draw(Canvas)`。旧模式仍先按原有多停靠点兼容规则转换参数。

```java
// 新模式告诉 Drawable 当前阴影渲染方式；具体走硬件、软件还是占位，由 draw(Canvas) 决定。
drawable.setShadowHardware(mShadowHardware);
// 新模式保留阴影和描边原始参数，保证软件 Canvas 仍能完整实现效果。
drawable.setShadowSize(mShadowSize)
        .setShadowInsetSize(mShadowInsetSize)
        .setShadowColor(mShadowColor)
        .setShadowOffsetX(mShadowOffsetX)
        .setShadowOffsetY(mShadowOffsetY);

// 不在 Builder 清除虚线；API < 28 的硬件 Canvas 在 draw() 中仅跳过本次虚线描边。
```

> 几何一致性说明：虚线降级只跳过绘制调用，不修改 `strokeSize`，因此
> `ensureValidRect()` 仍按原描边宽度计算 inset，卡片内容区域不因降级发生变化。

## 5. 兼容性与存量迁移

1. **默认关闭零影响**：`shape_shadowHardware` 默认 `false`；不启用新开关时，存量页面仍走原有逻辑；
2. **新属性为增量能力**：`shape_shadowInsetSize` 仅由新开关使用。需要纯占位时应同时开启
   `shape_shadowHardware`，避免改变旧模式的多停靠点兼容语义；
3. **旧公开属性/API 保留**：`shape_strokeGradientEnableShadow` 继续可用，不产生 XML 或 Java 的编译期破坏；
4. **新模式降级几何稳定**：基础占位 = `max(shadowSize, shadowInsetSize)`，带偏移时仅按
   `layoutRadius ± offset` 补足对应侧；虚线或阴影降级只跳过对应绘制调用，API 前后内容区域不变化；
5. **状态切换一致**：`ExtendStateListDrawable` 的 pressed / checked / disabled 等状态 Drawable
   都经由 `refreshShapeDrawable()` 生成，新逻辑对全部状态自动生效；
6. **代码方式构建**：`ShapeDrawable.setShadowHardware(true)` + `intoBackground(View)` 同样跳过软解。

## 6. 风险与已确认决策

| # | 问题 | 结论 / 建议 |
|---|---|---|
| 1 | **28+ 视觉差异**：`setShadowLayer` 的阴影衰减曲线与 `BlurMaskFilter` 不完全相同，同 `shadowSize` 下观感略有差别；无偏移直传档余量为 0，衰减尾部理论上可能微弱越界 | 半径采用混合映射（3.3）：无偏移直传、有偏移按旧方案分档；尾部若真机目视有裁切，`resolveShadowBlurRadius()` 返回值加 `0.5f` 单点修复；凹多边形 / 自交 Path 的 hwui 阴影轮廓可能不精确（圆角矩形、椭圆等凸形状安全） |
| 2 | **纯描边（透明填充）+ 28+**：透明填充时 hwui 满铺阴影投射源缺失 | **已确认：条件挂载**（见 3.3）——`draw()` 内按现成的 `haveFill / haveStroke` 选择挂载画笔：有填充挂 `mSolidPaint`，纯描边挂 `mStrokePaint`（框状阴影与旧方案 STROKE 模糊视觉同构，Builder 层零改动）；细描边阴影偏淡时仅对描边分支增大 radius |
| 3 | **< 28 "不绘制虚线"的口径** | **已确认：仅硬件 Canvas 整条不画**——API < 28 的硬件 Canvas 配置虚线时跳过本次整条描边（含多停靠点渐变描边），不退化为实线、不修改 `strokeSize`；软件 Canvas 下保留虚线 |
| 4 | **填充渐变的软解豁免** | **已确认：随新属性一并豁免**——渐变改走硬绘（`LinearGradient` 硬件加速一直支持），`rInto` 的 `setDither(true)` 按 3.5 说明单独保留防色带 |
| 5 | **阴影偏移裁剪**：`setShadowLayer` 的 dx/dy 由系统偏移，四周对称的 `shadowSize` 内缩在偏移较大时会裁剪，且 `saveLayer` 的内部边界也可能裁切模糊部分 | **已修复**：新模式以 `radius ± offset` 计算四侧最小留白，并同步扩张 `saveLayer` 边界；常规偏移不改变原有几何，超出原留白时仅补足对应侧 |
| 6 | **多停靠点兼容**：旧模式与新模式对 `shadowSize` 的语义不同 | **已处理**：旧模式保留 `shape_strokeGradientEnableShadow` 与原有占位逻辑；仅新开关开启时，`shadowSize` 表达阴影意图 |
| 7 | **Paint 阴影状态残留**：填充与描边画笔跨帧复用，状态切换后未选中的画笔可能保留 ShadowLayer | **上线前修复**：每帧先清理两个画笔，再仅配置当前投影源；增加“纯描边 ↔ 填充+描边”的回归 |
| 8 | **代码方式与 XML 不一致**：Builder 可做 API 分支而直接使用 `ShapeDrawable` 时会绕过它 | **上线前修复**：把 API、调试开关与 Canvas 能力判断收敛到 Drawable 的共享策略；两种入口共用测试集 |
| 9 | **宿主软件 Canvas**：API >= 28 也可能得到软件 Canvas，库不能将其强行升级为硬件 | **能力优先**：软件 Canvas 能实现阴影与虚线时完整保留；不额外建软件层、不走 `isOverLargeCache()`。若同时显式设置 `android:layerType="software"`，按 3.4.1 恢复 NONE 后再按最终 Canvas 能力绘制 |
| 10 | **删除公开属性/API 的升级风险**：外部工程 XML、style 与 Java 调用会编译失败 | 建议先废弃并保留一版兼容解析；下一主版本再删除。若本次删除，发布说明必须标为 breaking change |
| 11 | **无偏移视觉语义升级**：新模式无偏移时半径直传全量，与旧模式同配置观感（约一半）不同；offset 0 ↔ 非 0 切换时半径跳档（`shadowSize` ↔ `shadowSize/2` 或 /3） | **已确认为特性**：发布说明写明"无偏移时半径 = `shadowSize` 全量，需保持旧观感请减半"；跳档几何重算由现有 `setShadowOffsetX/Y` 的脏标记天然覆盖；属性未发布，当前是语义定型的唯一零成本窗口 |
| 12 | **旧模式 `saveLayer` 裁切（既有）**：半透明描边 + 阴影 + `setAlpha` 时，旧 `BlurMaskFilter` 阴影可能超出旧 layer 边界 | **明确保留为遗留行为**：本次仅扩张新模式 `setShadowLayer` 的 layer 边界，避免改变存量旧模式视觉；后续如修复，应单独作为旧模式兼容性改动评估与回归 |

## 7. 测试要点

1. **回归（新属性不配置）**：存量阴影 / 虚线 / 渐变页面在 28 上下设备的软解行为、
   安全降级行为与改动前完全一致，包含多停靠点与 `isShadowRenderEnable()` 分支；
2. **新模式纯占位验证**：开启新开关且仅配置 `shape_shadowInsetSize` 时，仅内缩且不触发阴影或软解；
3. **28+ 设备 / 模拟器（新属性开启）**：
   - 阴影正常显示、颜色 / 偏移 / 大小符合预期，纯描边卡框状阴影正常；
     断言 `view.getLayerType() == View.LAYER_TYPE_NONE`，确认新开关不会恢复 XML 或外部设置的软件层；
   - 无偏移卡片半径直传全量（4dp 配置即 4dp 模糊），有偏移卡片与旧方案（BlurMaskFilter 同档）观感一致；
   - 虚线正常硬件绘制；
   - 大尺寸 View（超过 `getScaledMaximumDrawingCacheSize()`）不再白块、不再触发安全降级；
   - 用 Layout Inspector 或开发者选项 Profile HWUI rendering 确认走 GPU 渲染；
4. **< 28 硬件 Canvas（新属性开启）**：
   - 阴影不绘制，但占位内缩保留（卡片内容尺寸与 28+ 一致）；
   - 配置了虚线的卡片：描边整条不画、无实线误画；纯实线描边正常绘制；
   - `shape_shadowInsetSize` 单独配置时仅内缩，无阴影、无软解，各 API 等级一致；
   - 填充渐变走硬绘正常显示、无色带（dither 保留）；
5. **状态切换**：pressed / checked / disabled / selected 下新方案行为一致；
6. **尺寸动态变化**：新属性 on 的卡片在 RecyclerView 中复用 / 动态变高时，
   背景几何与渐变随 `onBoundsChange` 正确适配（未被外部设置软件层时不重刷背景）；
   外部重新设置软件层后，下一次尺寸变化需恢复 `LAYER_TYPE_NONE`；
7. **Demo 硬件阴影策略对比面板**：主页面底部提供 API < 28 调试开关及 A～H 卡片，
   对比完整阴影、纯占位、更大占位、大偏移、XML 软件层恢复、宿主硬件层保留和填充渐变；
8. **超大卡片压力入口**：主页面可分别将直接卡片与嵌套 `ShapeRecyclerView` 的卡片扩展至
   10000dp。新模式预期不出现空白、不进入安全降级；页面上方已有旧模式的大卡片入口，
   预期触发安全背景降级；
9. **软件 Canvas 入口**：主页面“打开软件 Canvas 验证页”启动
   `android:hardwareAccelerated="false"` 的独立 Activity；阴影和虚线应完整显示，
   View 图层仍为 `LAYER_TYPE_NONE`，且不进入 `isOverLargeCache()`。
   新属性 off 的存量卡片重刷与软解重评估行为不变；
7. **代码方式**：`new ShapeDrawable().setShadowHardware(true)` 链路同样不触发软解；
8. **调试开关对比（28+ 测试机）**：`ShapeDrawableBuilder.setForceBelowApi28ForDebug(true)`
   后重建页面，验收阴影仅占位、虚线整条不画、无软解，与真机 < 28 行为一致；
   关闭开关重建后恢复 28+ 硬绘表现，两组对比截图核对视觉尺寸（占位内缩）一致。
9. **软件 Canvas（任意 API）**：关闭宿主硬件加速或使用软件 Canvas，验证阴影与虚线完整绘制；
   验证库不额外设置软件图层，也不触发 `isOverLargeCache()` 安全降级。
10. **XML 软件层冲突**：同时配置 `android:layerType="software"` 与新开关，验证无需等待尺寸测量即将
    View 图层恢复为 `LAYER_TYPE_NONE`、不替换当前 Drawable、不进入 `isOverLargeCache()`，并按最终
    Canvas 的 API 能力矩阵绘制。
11. **高风险组合**：填充+半透明描边（触发 `saveLayer`）、大正负 dx/dy、宽描边圆角、RING、LINE、
    纯描边与填充+描边互相切换；确认无裁切、无双重阴影。
12. **双入口一致性**：同一参数分别通过 XML Builder 和 `new ShapeDrawable()` 构建，在 API 27/28/35
    以及调试强制降级下比对阴影、虚线、占位与图层类型。
13. **混合映射专项**：
    - 无偏移直传档：`4dp` 配置视觉模糊即 4dp，占位恰好 = `max(shadowSize, insetSize)`，无额外扩张；
    - 有偏移分档档：28+ 真机 radius = S/2、调试开关 / 真机 < 28 的软件 Canvas radius = S/3；
      但所有路径均按 `layoutRadius = S/2` 保留相同几何；
    - offset 0 ↔ 非 0 动态切换：绘制半径跳档后，四侧留白与 `saveLayer` 边界同步核对，无残影 / 裁切；
    - 与 `shape_shadowInsetSize` 共存：S ≤ I 时留白仍 = I（渐进启用不改布局），
      S > I 时留白 = S（总额吸收），inset 三条语义零变化；
    - 直传档边界目视：放大检查卡片四边无高斯尾部裁切；若有，验证 `+0.5f` epsilon 修复。

## 8. 性能收益评估（新属性开启的 View）

> 以下为待实测验证的工程目标，不构成性能承诺；精确数值依赖设备、View 尺寸、动画、
> 宿主硬件加速状态和 GPU。建议用 Profile HWUI rendering / Macrobenchmark 记录 P50/P95 帧耗、
> 掉帧率、RSS/Graphics 内存后再填写具体数值。

收益来源：

| # | 收益项 | 旧方案（软解） | 新方案（硬绘） | 量级 |
|---|---|---|---|---|
| 1 | 渲染管线 | UI 线程 CPU 光栅化整张位图，每次 invalidate 全量重画 | DisplayList 录制 + RenderThread GPU 渲染 | 需以 Macrobenchmark 实测 |
| 2 | 内存 | 额外全尺寸软件位图 `w x h x 4` 字节（1080 x 2400 卡片约 10MB） | 无独立位图（DisplayList 极小） | 每卡片省约 10MB，列表 x N 项成倍放大 |
| 3 | 稳定性 | 超 `getScaledMaximumDrawingCacheSize()` 白块，需安全降级兜底 | 不触发由本库创建的软件层缓存超限路径 | 消除该特定白块路径，仍需实测整页稳定性 |
| 4 | 绘制调用（28+ 阴影） | 阴影 pass + 填充 + 描边共 3 次全形状绘制 | 填充（含 shadowLayer）+ 描边共 2 次 | 小幅（少一次 Path 光栅化） |
| 5 | 尺寸变化重刷 | 每次动态变高 / 复用重建背景（Drawable + Shader 分配 + setBackground 连锁） | 跳过（onBoundsChange 适配） | 列表复用场景中等收益 |
| 6 | 初始化时序 | post 下一帧二次 rInto（两次 setBackground / setLayerType） | 同步一次完成 | 首帧少一次布局抖动 |

分场景结论：

- **静态小卡片（无动画、不在列表）**：收益最小（位图小、一次性绘制），主要是内存与稳定性兜底；
- **大卡片 / 长列表 / 滚动动画**：收益最大——帧耗时显著下降（软解大位图每帧 CPU
  重光栅化是主要卡顿源）、内存节省成倍、白块风险清零；
- **API < 28**：硬件 Canvas 下阴影与虚线会按能力边界降级为占位/不绘制；软件 Canvas 下仍完整绘制。
  两者都不会因新属性由本库额外创建软件图层，但整体性能仍取决于宿主的渲染管线。
