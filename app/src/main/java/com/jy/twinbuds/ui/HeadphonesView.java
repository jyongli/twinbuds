package com.jy.twinbuds.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/** 装饰性耳机插图，不用虚构的连接指示灯表达设备状态。 */
public final class HeadphonesView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path band = new Path();
    public HeadphonesView(Context context, AttributeSet attrs) { super(context, attrs); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float scale = Math.min(getWidth() / 320f, getHeight() / 140f);
        canvas.save();
        canvas.translate((getWidth() - 320 * scale) / 2, (getHeight() - 140 * scale) / 2);
        canvas.scale(scale, scale);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFE1ECDF);
        canvas.drawOval(28, 8, 149, 129, paint);
        paint.setColor(0xFFECE6D8);
        canvas.drawOval(180, 16, 301, 137, paint);
        drawHeadphones(canvas, 48, 23, 0xFF245F4C, -10);
        drawHeadphones(canvas, 199, 31, 0xFF8B6953, 10);
        paint.setColor(0xFF809B86);
        paint.setStrokeWidth(3);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 5; i++) {
            float height = i == 2 ? 26 : (i % 2 == 0 ? 8 : 16);
            canvas.drawLine(148 + i * 6, 73 - height / 2, 148 + i * 6, 73 + height / 2, paint);
        }
        canvas.restore();
    }

    private void drawHeadphones(Canvas canvas, float x, float y, int color, float angle) {
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(angle, 40, 50);
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6);
        band.reset();
        band.moveTo(7, 59);
        band.lineTo(7, 40);
        band.cubicTo(7, -2, 73, -2, 73, 40);
        band.lineTo(73, 59);
        canvas.drawPath(band, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(0, 44, 20, 81, 8, 8, paint);
        canvas.drawRoundRect(60, 44, 80, 81, 8, 8, paint);
        paint.setColor(0xFFF4F5F0);
        canvas.drawRoundRect(10, 49, 16, 76, 3, 3, paint);
        canvas.drawRoundRect(64, 49, 70, 76, 3, 3, paint);
        canvas.restore();
    }
}
