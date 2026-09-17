package com.hjq.shape.demo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import com.hjq.shape.builder.ShapeDrawableBuilder;
import com.hjq.shape.layout.ShapeRecyclerView;
import com.hjq.shape.view.ShapeButton;
import com.hjq.shape.view.ShapeTextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupTestEntryMenu();

        TextView titleBar = findViewById(R.id.tb_main_bar);
        titleBar.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(titleBar.getText().toString()));
            startActivity(intent);
        });

        // 使用 Java 动态 API 验证多个颜色停靠点；本示例数据为 5 个。
        ShapeTextView javaGradientView = findViewById(R.id.tv_multi_stop_stroke_java);
        javaGradientView.getShapeDrawableBuilder()
                .setStrokeGradientColors(new int[]{
                        0xFF00E5FF,
                        0xFF2979FF,
                        0xFFD500F9,
                        0xFFFF6D00,
                        0xFFFFD600
                })
                .setStrokeGradientPositions(new float[]{
                        0.026f,
                        0.1875f,
                        0.5079f,
                        0.7736f,
                        0.8702f
                })
                // 当前库的角度约定：0 度左到右，90 度上到下；45 度即左上到右下。
                .setStrokeGradientAngle(45f)
                .intoBackground();

        // 通过动态扩大控件高度，验证软件图层缓存超限时的自动安全降级。
        ShapeTextView largeGradientView = findViewById(R.id.tv_multi_stop_stroke_large);
        ShapeButton toggleLargeCacheButton = findViewById(R.id.btn_toggle_large_cache);
        final boolean[] largeViewExpanded = {false};
        toggleLargeCacheButton.setOnClickListener(view -> {
            largeViewExpanded[0] = !largeViewExpanded[0];
            ViewGroup.LayoutParams layoutParams = largeGradientView.getLayoutParams();
            // 10000dp 在常见手机宽度下远大于系统软件绘制缓存上限。
            layoutParams.height = Math.round((largeViewExpanded[0] ? 10000 : 120) *
                    getResources().getDisplayMetrics().density);
            largeGradientView.setLayoutParams(layoutParams);
            largeGradientView.setText(largeViewExpanded[0]
                    ? "已展开为超大 View\n应自动降级：保留圆角、纯色背景和描边"
                    : "正常：五停靠点描边 + 阴影\n展开后：应保留圆角、纯色背景和描边");
            toggleLargeCacheButton.setText(largeViewExpanded[0]
                    ? "恢复普通高度，重新启用完整效果"
                    : "展开超大 View，验证描边保留");
            // 等待新尺寸参与测量后重新应用背景，确保缓存大小按最新高度判定。
            largeGradientView.post(() -> largeGradientView.getShapeDrawableBuilder().intoBackground());
        });

        ShapeButton shapeButton = findViewById(R.id.btn_main_test);
        shapeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                shapeButton.getShapeDrawableBuilder()
                        .setSolidColor(0xFF000000)
                        .setStrokeColor(0xFF5A8DDF)
                        .intoBackground();

                shapeButton.getTextColorBuilder()
                        .setTextColor(0xFFFFFFFF)
                        .intoTextColor();

                shapeButton.setText("颜色已经改变啦");
            }
        });
        View btn1 = findViewById(R.id.btn1);
        ShapeButton btnTest = findViewById(R.id.btnTest);
        btn1.setOnClickListener(view -> {
            btn1.setEnabled(false);
        });
        btnTest.setOnClickListener(view -> btnTest.post(() -> {
            btnTest.setPadding(0, 2500, 0, 0);
        }));

        setupShadowHardwareTestPanel();
    }

    /**
     * 配置硬件阴影策略对比面板。
     * 调试开关仅模拟 API < 28 的硬件 Canvas 能力边界，不会修改设备系统版本。
     */
    private void setupShadowHardwareTestPanel() {
        SwitchCompat forceBelowApi28Switch = findViewById(R.id.switch_force_below_api_28);
        TextView capabilityDescription = findViewById(R.id.tv_shadow_capability_description);
        TextView layerDescription = findViewById(R.id.tv_shadow_layer_description);
        ShapeTextView xmlSoftwareLayerCard = findViewById(R.id.tv_shadow_xml_software_layer);
        ShapeTextView hostHardwareLayerCard = findViewById(R.id.tv_shadow_host_hardware_layer);
        ShapeTextView largeHardwareCard = findViewById(R.id.tv_shadow_large_hardware_card);
        ShapeButton reapplySoftwareLayerButton = findViewById(R.id.btn_reapply_shadow_software_layer);
        ShapeButton toggleLargeHardwareButton = findViewById(R.id.btn_toggle_shadow_large_hardware);
        ShapeButton toggleRecyclerLargeButton = findViewById(R.id.btn_toggle_shadow_recycler_large);
        ShapeButton openSoftwareCanvasPageButton = findViewById(R.id.btn_open_software_canvas_page);
        TextView stressDescription = findViewById(R.id.tv_shadow_stress_description);
        ShapeRecyclerView shadowRecyclerView = findViewById(R.id.rv_shadow_large_card);

        // 宿主主动创建硬件层后重建背景，验证新方案不会将其错误重置为 NONE。
        hostHardwareLayerCard.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        hostHardwareLayerCard.getShapeDrawableBuilder().intoBackground();

        layerDescription.setText("图层校验：XML SOFTWARE → " +
                getLayerTypeName(xmlSoftwareLayerCard.getLayerType()) +
                "；宿主 HARDWARE → " + getLayerTypeName(hostHardwareLayerCard.getLayerType()));

        reapplySoftwareLayerButton.setOnClickListener(view -> {
            // 模拟宿主在初始化完成后再次设置软件层；尺寸变化后新模式应只解除 SOFTWARE。
            xmlSoftwareLayerCard.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            ViewGroup.LayoutParams layoutParams = xmlSoftwareLayerCard.getLayoutParams();
            layoutParams.height += 1;
            xmlSoftwareLayerCard.setLayoutParams(layoutParams);
            xmlSoftwareLayerCard.post(() -> layerDescription.setText("重新设置 SOFTWARE 并触发尺寸变化后：XML → " +
                    getLayerTypeName(xmlSoftwareLayerCard.getLayerType()) +
                    "；宿主 HARDWARE → " + getLayerTypeName(hostHardwareLayerCard.getLayerType())));
        });

        ShadowRecyclerAdapter recyclerAdapter = new ShadowRecyclerAdapter();
        shadowRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // 大卡片展开后由固定高度的列表视口自行滚动，避免父级 ScrollView 截断卡片后半部分。
        shadowRecyclerView.setNestedScrollingEnabled(true);
        shadowRecyclerView.setAdapter(recyclerAdapter);

        final boolean[] largeHardwareCardExpanded = {false};
        toggleLargeHardwareButton.setOnClickListener(view -> {
            largeHardwareCardExpanded[0] = !largeHardwareCardExpanded[0];
            ViewGroup.LayoutParams layoutParams = largeHardwareCard.getLayoutParams();
            // 10000dp 用于验证新模式不会创建 View 软件缓存，即使卡片本身极高也不应空白。
            layoutParams.height = dp(largeHardwareCardExpanded[0] ? 10000 : 120);
            largeHardwareCard.setLayoutParams(layoutParams);
            largeHardwareCard.setText(largeHardwareCardExpanded[0]
                    ? "直接超大新模式卡片（10000dp）：不应空白；切换 API < 28 后仅阴影和虚线消失"
                    : "直接大卡片（120dp）：点击上方按钮扩展到 10000dp");
            toggleLargeHardwareButton.setText(largeHardwareCardExpanded[0]
                    ? "恢复直接大卡片高度"
                    : "扩展直接大卡片到 10000dp");
            stressDescription.setText(largeHardwareCardExpanded[0]
                    ? "直接大卡片已展开：新模式不应调用 isOverLargeCache()，应继续显示填充和实线。"
                    : "压力测试待命：可分别展开直接大卡片或 RecyclerView 内的大卡片。");
        });

        toggleRecyclerLargeButton.setOnClickListener(view -> {
            recyclerAdapter.setLargeCard(!recyclerAdapter.isLargeCard());
            toggleRecyclerLargeButton.setText(recyclerAdapter.isLargeCard()
                    ? "恢复 RecyclerView 大卡片高度"
                    : "展开 RecyclerView 内卡片到 10000dp");
            stressDescription.setText(recyclerAdapter.isLargeCard()
                    ? "RecyclerView 内卡片已展开：请在列表区域内继续上滑，滚动和复用时不应出现空白。"
                    : "压力测试待命：可分别展开直接大卡片或 RecyclerView 内的大卡片。");
        });

        openSoftwareCanvasPageButton.setOnClickListener(view ->
                startActivity(new Intent(this, SoftwareCanvasTestActivity.class)));

        View[] comparisonCards = new View[]{
                findViewById(R.id.tv_shadow_full_effect),
                findViewById(R.id.tv_shadow_radius_direct),
                findViewById(R.id.tv_shadow_radius_divided),
                findViewById(R.id.tv_shadow_inset_only),
                findViewById(R.id.tv_shadow_large_inset),
                findViewById(R.id.tv_shadow_large_offset),
                findViewById(R.id.tv_shadow_xml_software_layer),
                findViewById(R.id.tv_shadow_host_hardware_layer),
                findViewById(R.id.tv_shadow_gradient),
                largeHardwareCard
        };
        forceBelowApi28Switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ShapeDrawableBuilder.setForceBelowApi28ForDebug(isChecked);
            capabilityDescription.setText(isChecked
                    ? "调试中：强制 API < 28 硬件 Canvas。阴影和虚线应隐藏；留白、填充、渐变、实线应保留。"
                    : "正常模式：API 28+ 硬件 Canvas 显示阴影和虚线；真实低版本设备仍会自动降级。");
            // 静态调试开关在 draw(Canvas) 中判断，刷新所有卡片即可切换可见效果。
            for (View comparisonCard : comparisonCards) {
                comparisonCard.invalidate();
            }
            for (int index = 0; index < shadowRecyclerView.getChildCount(); index++) {
                shadowRecyclerView.getChildAt(index).invalidate();
            }
        });
    }

    /** 配置首页测试入口菜单，避免需要在长 Demo 页面中手动寻找各类回归用例。 */
    private void setupTestEntryMenu() {
        View openHardwareStrategyButton = findViewById(R.id.btn_menu_open_shadow_strategy);
        View openPlaceholderLegacyButton = findViewById(R.id.btn_menu_open_placeholder_legacy);
        View openSoftwareCanvasButton = findViewById(R.id.btn_menu_open_software_canvas);
        NestedScrollView mainScrollView = findViewById(R.id.scroll_main);
        View hardwareStrategyPanel = findViewById(R.id.tv_shadow_hardware_test_title);

        openHardwareStrategyButton.setOnClickListener(view ->
                mainScrollView.smoothScrollTo(0, hardwareStrategyPanel.getTop()));
        openPlaceholderLegacyButton.setOnClickListener(view ->
                startActivity(new Intent(this, PlaceholderLegacyTestActivity.class)));
        openSoftwareCanvasButton.setOnClickListener(view ->
                startActivity(new Intent(this, SoftwareCanvasTestActivity.class)));
    }

    /** 将 dp 转为像素，保证压力卡片在不同密度设备上使用一致的逻辑尺寸。 */
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** 将图层常量转换为便于 Demo 观察的文本。 */
    private String getLayerTypeName(int layerType) {
        switch (layerType) {
            case View.LAYER_TYPE_NONE:
                return "NONE";
            case View.LAYER_TYPE_SOFTWARE:
                return "SOFTWARE";
            case View.LAYER_TYPE_HARDWARE:
                return "HARDWARE";
            default:
                return "UNKNOWN(" + layerType + ")";
        }
    }

    @Override
    protected void onDestroy() {
        // 调试开关为进程级静态状态，离开页面时恢复默认，避免影响后续 Demo 页面。
        ShapeDrawableBuilder.setForceBelowApi28ForDebug(false);
        super.onDestroy();
    }

    /** RecyclerView 嵌套大卡片测试适配器，复用同一个新硬件阴影策略。 */
    private static final class ShadowRecyclerAdapter extends RecyclerView.Adapter<ShadowRecyclerAdapter.ViewHolder> {

        private boolean mLargeCard;

        void setLargeCard(boolean largeCard) {
            if (mLargeCard == largeCard) {
                return;
            }
            mLargeCard = largeCard;
            notifyDataSetChanged();
        }

        boolean isLargeCard() {
            return mLargeCard;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            ShapeTextView card = new ShapeTextView(context);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12));
            card.setTextColor(Color.BLACK);
            card.setTextSize(14);
            card.getShapeDrawableBuilder()
                    .setRadius(dp(context, 14))
                    .setSolidColor(Color.WHITE)
                    .setStrokeColor(0xFF009688)
                    .setStrokeSize(dp(context, 2))
                    .setStrokeDashSize(dp(context, 10))
                    .setStrokeDashGap(dp(context, 5))
                    .setShadowHardware(true)
                    .setShadowSize(dp(context, 16))
                    .setShadowInsetSize(dp(context, 16))
                    .setShadowColor(0x66FF0000)
                    .setShadowOffsetY(dp(context, 4))
                    .intoBackground();
            return new ViewHolder(card);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ShapeTextView card = holder.mCard;
            boolean largeItem = mLargeCard && position == 1;
            RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(card.getContext(), largeItem ? 10000 : 120));
            layoutParams.setMargins(dp(card.getContext(), 10), dp(card.getContext(), 10),
                    dp(card.getContext(), 10), dp(card.getContext(), 10));
            card.setLayoutParams(layoutParams);
            if (largeItem) {
                card.setText("RecyclerView 第 2 项超大卡片（10000dp）\n滚动穿过此项后继续查看第 3 项，验证复用与空白问题");
            } else {
                card.setText("RecyclerView 第 " + (position + 1) + " 项（120dp）\n第 2 项可扩展为超大卡片，验证嵌套滚动与复用");
            }
        }

        @Override
        public int getItemCount() {
            // 第 1、3 项为常规高度，第 2 项可变为超大高度，保证滚动存在前后复用路径。
            return 3;
        }

        static final class ViewHolder extends RecyclerView.ViewHolder {

            final ShapeTextView mCard;

            ViewHolder(ShapeTextView itemView) {
                super(itemView);
                mCard = itemView;
            }
        }

        private static int dp(Context context, int value) {
            return Math.round(value * context.getResources().getDisplayMetrics().density);
        }
    }
}
