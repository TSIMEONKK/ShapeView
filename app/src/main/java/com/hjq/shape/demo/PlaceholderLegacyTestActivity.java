package com.hjq.shape.demo;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hjq.shape.drawable.ShapeDrawable;
import com.hjq.shape.view.ShapeButton;
import com.hjq.shape.view.ShapeTextView;

/**
 * 占位与旧模式回归页。
 * 用同一尺寸卡片对照旧普通阴影、旧多停靠点占位、新模式阴影和纯占位，
 * 同时保留旧模式的大卡片安全降级及直接 ShapeDrawable API 动态切换验证。
 */
public class PlaceholderLegacyTestActivity extends AppCompatActivity {

    private ShapeDrawable mDirectDrawable;
    private boolean mDirectHardware;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_placeholder_legacy_test);

        setupLegacyLargeCardTest();
        setupDirectDrawableSwitchTest();
    }

    /** 验证旧模式超大 View 仍会进入原有安全降级，而不是沿用新模式路径。 */
    private void setupLegacyLargeCardTest() {
        ShapeTextView legacyLargeCard = findViewById(R.id.tv_legacy_large_card);
        ShapeButton toggleButton = findViewById(R.id.btn_toggle_legacy_large_card);
        TextView statusView = findViewById(R.id.tv_legacy_large_status);
        final boolean[] expanded = {false};

        toggleButton.setOnClickListener(view -> {
            expanded[0] = !expanded[0];
            ViewGroup.LayoutParams layoutParams = legacyLargeCard.getLayoutParams();
            layoutParams.height = dp(expanded[0] ? 10000 : 120);
            legacyLargeCard.setLayoutParams(layoutParams);
            legacyLargeCard.setText(expanded[0]
                    ? "旧模式超大卡片（10000dp）\n预期：触发 isOverLargeCache() 后保留安全背景，不出现白块"
                    : "旧模式卡片（120dp）\n点击按钮扩展，验证原有安全降级");
            toggleButton.setText(expanded[0] ? "恢复旧模式卡片高度" : "扩展旧模式卡片到 10000dp");
            statusView.setText(expanded[0]
                    ? "预期：旧模式将异步评估缓存上限并切换安全背景；阴影、虚线和填充渐变可被舍弃。"
                    : "旧模式正常高度：应使用原有软件层完整绘制。");
        });

        legacyLargeCard.post(() -> statusView.setText("旧模式初始 View 图层：" +
                getLayerTypeName(legacyLargeCard.getLayerType()) + "（配置阴影和虚线时预期为 SOFTWARE）"));
    }

    /** 验证直接 ShapeDrawable API 切换模式时，四侧阴影留白会重新计算。 */
    private void setupDirectDrawableSwitchTest() {
        ShapeTextView directCard = findViewById(R.id.tv_direct_drawable_switch);
        ShapeButton toggleButton = findViewById(R.id.btn_toggle_direct_drawable_mode);
        TextView statusView = findViewById(R.id.tv_direct_drawable_status);

        mDirectDrawable = new ShapeDrawable()
                .setRadius(dp(14))
                .setSolidColor(Color.WHITE)
                .setStrokeColor(0xFFFF9800)
                .setStrokeSize(dp(2))
                .setShadowColor(0x66FF0000)
                .setShadowSize(dp(16))
                .setShadowInsetSize(dp(16))
                .setShadowOffsetY(dp(24));
        mDirectDrawable.intoBackground(directCard);

        toggleButton.setOnClickListener(view -> {
            mDirectHardware = !mDirectHardware;
            mDirectDrawable.setShadowHardware(mDirectHardware);
            mDirectDrawable.intoBackground(directCard);
            directCard.setText(mDirectHardware
                    ? "直接 ShapeDrawable：新模式，大偏移留白应重算"
                    : "直接 ShapeDrawable：旧模式，回到 BlurMaskFilter 路径");
            toggleButton.setText(mDirectHardware ? "切回直接 Drawable 旧模式" : "切换直接 Drawable 新模式");
            statusView.setText("直接 API 当前模式：" + (mDirectHardware ? "新模式" : "旧模式") +
                    "；View 图层：" + getLayerTypeName(directCard.getLayerType()));
        });
    }

    /** 将 dp 转为像素，保证布局 XML 与代码方式构建的视觉尺寸可比。 */
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** 将图层常量转换为页面可直接核对的文本。 */
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
}
