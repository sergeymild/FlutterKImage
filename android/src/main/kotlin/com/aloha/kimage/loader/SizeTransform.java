package com.aloha.kimage.loader;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class SizeTransform {
    public static Bitmap transformResult(Bitmap result, int targetWidth, int targetHeight) {
        int inWidth = result.getWidth();
        int inHeight = result.getHeight();

        int drawX = 0;
        int drawY = 0;
        int drawWidth = inWidth;
        int drawHeight = inHeight;

        Matrix matrix = new Matrix();

        float widthRatio = targetWidth / (float) inWidth;
        float heightRatio = targetHeight / (float) inHeight;
        float scaleX, scaleY;
        if (widthRatio > heightRatio) {
            int newSize = (int) Math.ceil(inHeight * (heightRatio / widthRatio));
            drawY = (inHeight - newSize) / 2;
            drawHeight = newSize;
            scaleX = widthRatio;
            scaleY = targetHeight / (float) drawHeight;
        } else {
            int newSize = (int) Math.ceil(inWidth * (widthRatio / heightRatio));
            drawX = (inWidth - newSize) / 2;
            drawWidth = newSize;
            scaleX = targetWidth / (float) drawWidth;
            scaleY = heightRatio;
        }
        if (shouldResize(inWidth, inHeight, targetWidth, targetHeight)) {
            matrix.preScale(scaleX, scaleY);
        }

        Bitmap newResult = Bitmap.createBitmap(result, drawX, drawY, drawWidth, drawHeight, matrix, true);
        if (newResult != result) {
            result.recycle();
            result = newResult;
        }

        return result;
    }

    private static boolean shouldResize(int inWidth, int inHeight,
                                        int targetWidth, int targetHeight) {
        return inWidth > targetWidth || inHeight > targetHeight;
    }
}
