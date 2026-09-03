package com.hjq.shape.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;

import com.hjq.bar.OnTitleBarListener;
import com.hjq.bar.TitleBar;
import com.hjq.shape.view.ShapeButton;
import com.hjq.shape.view.ShapeTextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TitleBar titleBar = findViewById(R.id.tb_main_bar);
        titleBar.setOnTitleBarListener(new OnTitleBarListener() {
            @Override
            public void onTitleClick(TitleBar titleBar) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(titleBar.getTitle().toString()));
                startActivity(intent);
            }
        });

        // 使用 Java 动态 API 验证多个颜色停靠点；本示例数据为 5 个。
        ShapeTextView javaGradientView = findViewById(R.id.tv_multi_stop_stroke_java);
        javaGradientView.getShapeDrawableBuilder()
                .setStrokeGradientColors(new int[]{
                        0x33FFFFFF,
                        0x0DFFFFFF,
                        0x00FFFFFF,
                        0x00FFFFFF,
                        0x14FFFFFF
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
    }
}
