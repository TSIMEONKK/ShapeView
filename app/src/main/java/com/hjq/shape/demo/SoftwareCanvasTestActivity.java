package com.hjq.shape.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.hjq.shape.view.ShapeTextView;

/**
 * 软件 Window 验证页。
 * Manifest 关闭本 Activity 的硬件加速，用于验证新方案在软件 Canvas 下仍完整绘制，
 * 同时不会由 ShapeView 额外创建 View 级软件图层。
 */
public class SoftwareCanvasTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_software_canvas_test);

        ShapeTextView effectCard = findViewById(R.id.tv_software_canvas_effect);
        TextView layerDescription = findViewById(R.id.tv_software_canvas_layer_description);
        layerDescription.setText("当前 Window 已关闭硬件加速；测试卡片 View 图层为 " +
                getLayerTypeName(effectCard.getLayerType()) +
                "。预期：阴影和虚线完整显示，且不会触发 isOverLargeCache()。 ");
    }

    /** 将图层常量转换为测试页可直接核对的文本。 */
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
