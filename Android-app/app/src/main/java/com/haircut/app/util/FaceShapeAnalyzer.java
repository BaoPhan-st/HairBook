package com.haircut.app.util;

import android.graphics.PointF;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;

import java.util.List;

/**
 * Phân loại hình dạng khuôn mặt (Oval / Tròn / Vuông / Trái tim / Kim cương / Dài)
 * bằng heuristic dựa trên tỉ lệ các dải ngang của đường viền mặt (FaceContour.FACE)
 * do ML Kit trả về. Đây là ước lượng gần đúng, không phải chẩn đoán chính xác 100%.
 */
public class FaceShapeAnalyzer {

    public static String classify(Face face) {
        FaceContour contour = face.getContour(FaceContour.FACE);
        if (contour == null) return "Oval (trái xoan)";

        List<PointF> points = contour.getPoints();
        if (points.isEmpty()) return "Oval (trái xoan)";

        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
        for (PointF p : points) {
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }
        float faceHeight = maxY - minY;
        if (faceHeight <= 0) return "Oval (trái xoan)";

        float foreheadWidth = bandWidth(points, minY, faceHeight, 0.05f, 0.20f);
        float cheekWidth    = bandWidth(points, minY, faceHeight, 0.45f, 0.60f);
        float jawWidth      = bandWidth(points, minY, faceHeight, 0.85f, 0.98f);

        if (cheekWidth <= 0) return "Oval (trái xoan)";

        float lengthToWidth   = faceHeight / cheekWidth;
        float jawToCheek      = jawWidth / cheekWidth;
        float foreheadToCheek = foreheadWidth / cheekWidth;

        if (lengthToWidth > 1.55f) return "Dài (Oblong)";
        if (jawToCheek > 0.90f && foreheadToCheek > 0.90f && lengthToWidth < 1.3f) return "Vuông (Square)";
        if (foreheadToCheek - jawToCheek > 0.12f) return "Trái tim (Heart)";
        if (jawToCheek < 0.82f && foreheadToCheek < 0.82f) return "Kim cương (Diamond)";
        if (lengthToWidth < 1.15f && jawToCheek > 0.82f) return "Tròn (Round)";
        return "Oval (trái xoan)";
    }

    /** Bề rộng (max X - min X) của các điểm contour rơi vào dải [fromRatio, toRatio] chiều cao mặt. */
    private static float bandWidth(List<PointF> points, float minY, float faceHeight,
                                   float fromRatio, float toRatio) {
        float yFrom = minY + faceHeight * fromRatio;
        float yTo = minY + faceHeight * toRatio;
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        boolean found = false;
        for (PointF p : points) {
            if (p.y >= yFrom && p.y <= yTo) {
                found = true;
                minX = Math.min(minX, p.x);
                maxX = Math.max(maxX, p.x);
            }
        }
        return found ? (maxX - minX) : 0f;
    }
}