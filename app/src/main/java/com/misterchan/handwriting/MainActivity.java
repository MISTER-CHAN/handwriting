package com.misterchan.handwriting;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final Rect SQUARE_192 = new Rect(0, 0, 126, 126);

    private static final Paint PAINT_SRC = new Paint() {
        {
            setAntiAlias(false);
            setBlendMode(BlendMode.SRC);
            setFilterBitmap(false);
        }
    };

    private Bitmap bitmap;
    private Bitmap brush;
    private Bitmap brushBlack;
    private Bitmap brushBlackRotated;
    private Bitmap brushRed;
    private Bitmap brushRedRotated;
    private Bitmap cursorBitmap;
    private Bitmap cuttingBitmap;
    private Bitmap guideBitmap;
    private Bitmap paper;
    private Bitmap previewBitmap;
    private Bitmap rotatedBitmap;
    private Bitmap textBitmap;
    private boolean autoNewline = false;
    private boolean hasNotLoaded = true;
    private boolean isNotErasing = true;
    private boolean isWriting = false;
    private boolean space = false;
    private Button bColor;
    private Button bNew;
    private Canvas canvas;
    private Canvas cursorCanvas;
    private Canvas cuttingCanvas;
    private Canvas guideCanvas;
    private Canvas previewCanvas;
    private Canvas rotatedCanvas;
    private Canvas textCanvas;
    private float alias = 8.0f;
    private float bottom;
    private float charBottom;
    private float charLength = -1.0f;
    private float charTop;
    private float charWidth = 64.0f;
    private float columnSpacing = 4.0f;
    private float cursorX = 0.0f, cursorY = 0.0f;
    private float curvature = 2.0f;
    private float left;
    private float lineSpacing = 0.0f;
    private float previewX = 0f, previewY = 0.0f;
    private float right;
    private float size;
    private float softness = 0.5f;
    private float strokeWidth = 64.0f;
    private float top;
    private ImageView ivPaper, ivText, ivCursor, ivPreview, ivGuide, iv, ivCutting;
    private int brushColor = Color.BLACK;
    private int handwriting = 16;
    private int width, height;
    private LinearLayout llOptions;
    private LinearLayout llTools;
    private final Matrix rotation = new Matrix();
    private RadioButton rbHorizontalWriting, rbVerticalWriting;
    private Rect rect;
    private Rect rotatedRect;
    private SeekBar sbCharLength;
    private SeekBar sbConcentration;
    private SwitchCompat sRotate;
    private View vTranslucent;

    private final Paint cursor = new Paint() {
        {
            setStrokeWidth(8.0f);
            setStyle(Style.STROKE);
        }
    };

    private final Paint cutter = new Paint() {
        {
            setAntiAlias(true);
            setColor(Color.BLACK);
            setStrokeWidth(2.0f);
        }
    };

    private final Paint eraser = new Paint() {
        {
            setAntiAlias(false);
            setBlendMode(BlendMode.CLEAR);
            setColor(Color.BLACK);
            setDither(false);
        }
    };

    private final Paint guide = new Paint() {
        {
            setAntiAlias(true);
            setColor(Color.BLACK);
            setStrokeWidth(2.0f);
        }
    };

    private final Paint paint = new Paint() {
        {
            setAntiAlias(true);
            setFilterBitmap(true);
            setStrokeWidth(2.0f);
        }
    };

    private final Paint previewer = new Paint() {
        {
            setStrokeWidth(2.0f);
            setStyle(Style.STROKE);
        }
    };

    private final Paint scaler = new Paint() {
        {
            setAntiAlias(true);
            setFilterBitmap(true);
            setStrokeWidth(2.0f);
        }
    };

    private final ActivityResultCallback<Uri> onImagePickedCallback = result -> {
        if (result == null) {
            return;
        }
        try (InputStream inputStream = getContentResolver().openInputStream(result)) {
            recycleBitmap(paper);
            paper = BitmapFactory.decodeStream(inputStream);
            if (isWriting) {
                isWriting = false;
                eraseBitmap(bitmap);
            }
            ivPaper.setImageBitmap(paper);
            preview();
            drawCursor();
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(
                    new ActivityResultContracts.PickVisualMedia(),
                    onImagePickedCallback);

    private final PickVisualMediaRequest pickVisualMediaRequest = new PickVisualMediaRequest.Builder()
            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
            .build();

    private final CompoundButton.OnCheckedChangeListener onCharLengthAutoRadButtCheckedChangeListener = (buttonView, isChecked) -> {
        if (isChecked) {
            sbCharLength.setVisibility(View.GONE);
            charLength = -1.0f;
            preview();
        }
    };

    private final CompoundButton.OnCheckedChangeListener onCharLengthCustomRadButtCheckedChangeListener = (buttonView, isChecked) -> {
        if (isChecked) {
            charLength = sbCharLength.getProgress();
            sbCharLength.setVisibility(View.VISIBLE);
            preview();
        }
    };

    private final CompoundButton.OnCheckedChangeListener onRotateSwitchCheckedChangeListener = (buttonView, isChecked) -> {
        brush = (brushColor == Color.BLACK
                ? isChecked ? brushBlackRotated : brushBlack
                : isChecked ? brushRedRotated : brushRed)
                .copy(Bitmap.Config.ARGB_8888, true);
        setBrushConcentration((double) sbConcentration.getProgress() / 10.0);
        drawGuide();
    };

    private final View.OnClickListener onClickColorButtonListener = v -> {
        isNotErasing = !isNotErasing;
        bColor.setTextColor(isNotErasing ? brushColor : Color.WHITE);
    };

    private final View.OnLongClickListener onLongClickColorButtonListener = v -> {
//        brush.recycle();
        if (brushColor == Color.BLACK) {
//            brush = (sRotate.isChecked() ? brushRedRotated : brushRed).copy(Bitmap.Config.ARGB_8888, true);
            brushColor = Color.RED;
            if (isNotErasing) {
                bColor.setTextColor(Color.RED);
            }
        } else {
//            brush = (sRotate.isChecked() ? brushBlackRotated : brushBlack).copy(Bitmap.Config.ARGB_8888, true);
            brushColor = Color.BLACK;
            if (isNotErasing) {
                bColor.setTextColor(Color.BLACK);
            }
        }
        paint.setColor(brushColor);
//        setBrushConcentration((double) sbConcentration.getProgress() / 10.0);
        return true;
    };

    private final View.OnClickListener onClickNewButtonListener = v ->
            Toast.makeText(this, "長按以確定作廢當前紙張並使用新紙張", Toast.LENGTH_LONG).show();

    private final View.OnClickListener onClickNextButtonListener = v -> {
        if (isWriting) {
            next();
        }
    };

    private final View.OnClickListener onClickPaperButtonListener = v ->
            pickMedia.launch(pickVisualMediaRequest);

    private final View.OnLongClickListener onLongClickNewButtonListener = v -> {
        v.setEnabled(false);
        eraseBitmap(textBitmap);
        iv.invalidate();
        drawCursor();
        return true;
    };

    private final View.OnClickListener onClickReturnButtonListener = v -> {
        if (isWriting) {
            next();
        } else {
            if (rbHorizontalWriting.isChecked()) {
                cursorX = 0.0f;
                cursorY += charWidth + lineSpacing;
            } else {
                cursorY = 0.0f;
                cursorX -= charWidth + lineSpacing;
            }
            drawCursor();
        }
    };

    private final OnSeekBarProgressChangedListener onCharLengthSeekBarProgressChangedListener = progress -> {
        charLength = progress;
        preview();
    };

    private final OnSeekBarProgressChangedListener onCharWidthSeekBarProgressChangedListener = progress -> {
        charWidth = progress;
        preview();
    };

    private final OnSeekBarProgressChangedListener onColSpacingSeekBarProgressChangedListener = progress -> {
        columnSpacing = progress;
        preview();
    };

    private final OnSeekBarProgressChangedListener onLineSpacingSeekBarProgressChangedListener = progress -> {
        lineSpacing = progress;
        preview();
    };

    private final SeekBar.OnSeekBarChangeListener onAlphaSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            setBrushAlpha(seekBar.getProgress());
        }
    };

    private final SeekBar.OnSeekBarChangeListener onConcentrationSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            setBrushConcentration((double) seekBar.getProgress() / 10.0);
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    private final View.OnTouchListener onTouchBackspaceButtonListener = new View.OnTouchListener() {
        private boolean backspace = false;
        private float backspaceX = 0.0f, backspaceY = 0.0f;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    backspaceX = event.getX();
                    backspaceY = event.getY();
                }
                case MotionEvent.ACTION_MOVE -> {
                    if (rbHorizontalWriting.isChecked()) {
                        float x = event.getX();
                        int deltaX = (int) (backspaceX - x);
                        if (deltaX != 0) {
                            backspace(deltaX);
                            backspaceX = x;
                        }
                    } else {
                        float y = event.getY();
                        int deltaY = (int) (backspaceY - y);
                        if (deltaY != 0) {
                            backspace(deltaY);
                            backspaceY = y;
                        }
                    }
                    backspace = true;
                }
                case MotionEvent.ACTION_UP -> {
                    if (backspace) {
                        backspace = false;
                    } else {
                        backspace((int) (charWidth / 4.0f));
                    }
                }
            }
            return true;
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    private final View.OnTouchListener onTouchIVsListener = new View.OnTouchListener() {
        private float lastX = 0.0f, lastY = 0.0f, brushWidth;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final float x = event.getX(), y = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    lastX = x;
                    lastY = y;
                    brushWidth = 0.0f;
                    if (!isWriting) {
                        startWriting();
                    }
                }
                case MotionEvent.ACTION_MOVE -> {

                    if (isNotErasing) {

                        if (x < left) left = x;
                        if (x > right) right = x;
                        if (y < top) top = y;
                        if (y > bottom) bottom = y;

                        float d = (float) Math.sqrt(Math.pow(x - lastX, 2.0) + Math.pow(y - lastY, 2.0)),
                                a = d / (float) Math.pow(d, curvature),
                                w = 0.0f,
                                width = (float) Math.pow(1.0 - d / size, handwriting) * strokeWidth,
                                dBwPD = (width - brushWidth) / d, // Delta brushWidth per d
                                dxPD = (x - lastX) / d, dyPD = (y - lastY) / d; // Delta x per d and delta y per d
                        if (width >= brushWidth) {
                            for (float f = 0; f < d; f += alias) {
                                w = a * (float) Math.pow(f, curvature) * dBwPD + brushWidth;
                                RectF dst = new RectF(dxPD * f + lastX - w, dyPD * f + lastY - w,
                                        dxPD * f + lastX + w, dyPD * f + lastY + w);
                                canvas.drawBitmap(brush, SQUARE_192, dst, paint);
                            }
                        } else {
                            for (float f = 0.0f; f < d; f += alias) {
                                w = (float) Math.pow(f / a, 1.0 / curvature) * dBwPD + brushWidth;
                                RectF dst = new RectF(dxPD * f + lastX - w, dyPD * f + lastY - w,
                                        dxPD * f + lastX + w, dyPD * f + lastY + w);
                                canvas.drawBitmap(brush, SQUARE_192, dst, paint);
                            }
                        }
                        brushWidth = w;
                        lastX = x;
                        lastY = y;

                    } else {
                        int strokeWidth = (int) MainActivity.this.strokeWidth,
                                halfStrokeWidth = strokeWidth >> 1,
                                strokeLeft = (int) (x - halfStrokeWidth),
                                strokeTop = (int) (y - halfStrokeWidth);
                        canvas.drawRect(strokeLeft, strokeTop, strokeLeft + strokeWidth, strokeTop + strokeWidth, eraser);

                    }

                }
                case MotionEvent.ACTION_UP -> {
                    return true;
                }
            }
            iv.invalidate();
            return true;
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    private final View.OnTouchListener onTouchIVsListenerX = new View.OnTouchListener() {
        private float lastX, lastY;
        private float lastTLX = Float.NaN, lastTLY, lastRX, lastRY, lastBX, lastBY;
        private float maxRad;
        private VelocityTracker velocityTracker;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    velocityTracker = VelocityTracker.obtain();
                    velocityTracker.addMovement(event);
                    final float x = event.getX(), y = event.getY();
                    final float rad = strokeWidth / 2.0f;
                    if (!isWriting) {
                        startWriting();
                    }
                    maxRad = rad;
                    lastX = x;
                    lastY = y;
                }
                case MotionEvent.ACTION_MOVE -> {
                    final float x = event.getX(), y = event.getY();
                    if (isNotErasing) {
                        if (x < left) left = x;
                        if (x > right) right = x;
                        if (y < top) top = y;
                        if (y > bottom) bottom = y;

                        velocityTracker.addMovement(event);
                        velocityTracker.computeCurrentVelocity(1);
                        final float vel = (float) Math.hypot(velocityTracker.getXVelocity(), velocityTracker.getYVelocity());
                        final float rad = Math.min(maxRad / vel / softness, maxRad);

                        if (Float.isNaN(lastTLX) /* || ... || Float.isNaN(lastBY) */) {
                            lastTLX = lastX - rad;
                            lastTLY = lastY - rad;
                            lastRX = lastX + rad;
                            lastRY = lastY;
                            lastBX = lastX;
                            lastBY = lastY + rad;
                        }
                        final float
                                tlx = x - rad, tly = y - rad,
                                rx = x + rad, ry = y,
                                bx = x, by = y + rad;

                        final Path pathT = new Path();
                        pathT.moveTo(lastTLX, lastTLY);
                        pathT.lineTo(lastRX, lastRY);
                        pathT.lineTo(rx, ry);
                        pathT.lineTo(tlx, tly);
                        pathT.close();
                        final Path pathBR = new Path();
                        pathBR.moveTo(lastRX, lastRY);
                        pathBR.lineTo(lastBX, lastBY);
                        pathBR.lineTo(bx, by);
                        pathBR.lineTo(rx, ry);
                        pathBR.close();
                        final Path pathL = new Path();
                        pathL.moveTo(lastBX, lastBY);
                        pathL.lineTo(lastTLX, lastTLY);
                        pathL.lineTo(tlx, tly);
                        pathL.lineTo(bx, by);
                        pathL.close();
                        final Path path = new Path();
                        path.op(pathT, Path.Op.UNION);
                        path.op(pathBR, Path.Op.UNION);
                        path.op(pathL, Path.Op.UNION);

                        canvas.drawPath(path, paint);

                        lastX = x;
                        lastY = y;
                        lastTLX = tlx;
                        lastTLY = tly;
                        lastRX = rx;
                        lastRY = ry;
                        lastBX = bx;
                        lastBY = by;

                    } else {
                        int strokeWidth = (int) MainActivity.this.strokeWidth,
                                halfStrokeWidth = strokeWidth >> 1,
                                strokeLeft = (int) (x - halfStrokeWidth),
                                strokeTop = (int) (y - halfStrokeWidth);
                        canvas.drawRect(strokeLeft, strokeTop, strokeLeft + strokeWidth, strokeTop + strokeWidth, eraser);
                    }
                    iv.invalidate();
                }
                case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    velocityTracker.recycle();
                    lastTLX = /* lastTLY = ... = lastRY = */ Float.NaN;
                }
            }
            return true;
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    private final View.OnTouchListener onTouchNextButtonListener = (v, event) -> {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            int[] canvasLocation = new int[2];
            iv.getLocationOnScreen(canvasLocation);
            if (event.getRawY() < canvasLocation[1] + height) {
                if (isWriting) {
                    next();
                }
                int[] buttonLocation = new int[2];
                v.getLocationOnScreen(buttonLocation);
                cursorX = (buttonLocation[0] + event.getX() - canvasLocation[0]) - charWidth / 8.0f;
                cursorY = (buttonLocation[1] + event.getY() - canvasLocation[1]) - charWidth / 2.0f;
                drawCursor(true);
            }
        }
        return false;
    };

    @SuppressLint("ClickableViewAccessibility")
    private final View.OnTouchListener onTouchSpaceButtonListener = new View.OnTouchListener() {
        private float spacing = 0.0f, end = 0.0f;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    if (isWriting) {
                        if (rbHorizontalWriting.isChecked() ^ sRotate.isChecked()) {
                            spacing = event.getX();
                            end = width;
                        } else {
                            spacing = event.getY();
                            end = height;
                        }
                    } else {
                        spacing = rbHorizontalWriting.isChecked() ? event.getX() : event.getY();
                    }
                }
                case MotionEvent.ACTION_MOVE -> {
                    space = true;
                    if (isWriting) {
                        eraseBitmap(cuttingBitmap);
                        if (rbHorizontalWriting.isChecked() ^ sRotate.isChecked()) {
                            float x = event.getX();
                            cuttingCanvas.drawLine(end += x - spacing, 0.0f, end, height, cutter);
                            spacing = x;
                        } else {
                            float y = event.getY();
                            cuttingCanvas.drawLine(0.0f, end += y - spacing, width, end, cutter);
                            spacing = y;
                        }
                        ivCutting.invalidate();
                    } else {
                        if (rbHorizontalWriting.isChecked()) {
                            float x = event.getX();
                            cursorX += x - spacing;
                            spacing = x;
                        } else {
                            float y = event.getY();
                            cursorY += y - spacing;
                            spacing = y;
                        }
                        drawCursor();
                    }
                }
                case MotionEvent.ACTION_UP -> {
                    if (space) {
                        space = false;
                        if (isWriting) {
                            next((int) end);
                        }
                    } else {
                        if (isWriting) {
                            next((int) ((right - left) / 4.0f * 3.0f));
                        } else {
                            if (rbHorizontalWriting.isChecked()) {
                                cursorX += charWidth / 4.0f;
                            } else {
                                cursorY += charWidth / 4.0f;
                            }
                        }
                        drawCursor();
                    }
                }
            }
            return true;
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    private final View.OnTouchListener onTouchPreviewListener = (v, event) -> {
        previewX = event.getX();
        previewY = event.getY();
        preview();
        return true;
    };

    @SuppressLint("ClickableViewAccessibility")
    private final View.OnClickListener onClickOptionsButtonListener = v -> {
        if (llOptions.getVisibility() == View.VISIBLE) {
            llOptions.setVisibility(View.GONE);
            llTools.setVisibility(View.VISIBLE);
            ivPreview.setVisibility(View.INVISIBLE);
        } else {
            ivPreview.setVisibility(View.VISIBLE);
            llTools.setVisibility(View.INVISIBLE);
            llOptions.setVisibility(View.VISIBLE);
            bNew.setEnabled(true);
        }
    };

    private void backspace(int size) {
        if (isWriting) {
            isWriting = false;
            eraseBitmap(bitmap);
            iv.invalidate();
            ivGuide.setVisibility(View.INVISIBLE);
            vTranslucent.setVisibility(View.INVISIBLE);
        } else {
            if (rbHorizontalWriting.isChecked()) {

                if (cursorX > 0) {
                    cursorX -= size;
                } else {
                    cursorY -= charWidth;
                    cursorX = width - size;
                }

                textCanvas.drawRect(cursorX, cursorY, cursorX + size, cursorY + charWidth, eraser);

            } else {

                if (cursorY > 0) {
                    cursorY -= size;
                } else {
                    cursorX += charWidth;
                    cursorY = height - size;
                }

                textCanvas.drawRect(cursorX, cursorY, cursorX + charWidth, cursorY + size, eraser);

            }

            ivText.invalidate();
            drawCursor();
        }
    }

    private void drawCursor() {
        drawCursor(false);
    }

    private void drawCursor(boolean style) {
        eraseBitmap(cursorBitmap);
        cursorCanvas.drawRect(cursorX, style ? cursorY : cursorY + charWidth,
                cursorX + charWidth, cursorY + charWidth,
                cursor);
        ivCursor.invalidate();
    }

    private void drawGuide() {
        eraseBitmap(guideBitmap);
        if (!sRotate.isChecked()) {
            guideCanvas.drawLine(0.0f, charTop, width, charTop, guide);
            guideCanvas.drawLine(0.0f, charBottom, width, charBottom, guide);
        } else {
            guideCanvas.drawLine(width / 2.0f, 0.0f, width / 2.0f, height, guide);
        }
        ivGuide.invalidate();
    }

    private void eraseBitmap(Bitmap bitmap) {
        bitmap.eraseColor(Color.TRANSPARENT);
    }

    private void load() {
        width = iv.getWidth();
        height = iv.getHeight();
        rect = new Rect(0, 0, width, height);
        rotatedRect = new Rect(0, 0, height, width);
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        rotation.postRotate(-90.0f);
        rotation.postTranslate(0.0f, width);
        iv.setImageBitmap(bitmap);

        rotatedBitmap = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);
        rotatedCanvas = new Canvas(rotatedBitmap);

        textBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        textCanvas = new Canvas(textBitmap);
        ivText.setImageBitmap(textBitmap);

        cursorBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        cursorCanvas = new Canvas(cursorBitmap);
        ivCursor.setImageBitmap(cursorBitmap);

        previewBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        previewCanvas = new Canvas(previewBitmap);
        ivPreview.setImageBitmap(previewBitmap);
        previewX = width >> 1;
        previewY = 0;

        guideBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        guideCanvas = new Canvas(guideBitmap);
        ivGuide.setImageBitmap(guideBitmap);

        cuttingBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        cuttingCanvas = new Canvas(cuttingBitmap);
        ivCutting.setImageBitmap(cuttingBitmap);

        size = (float) Math.sqrt(Math.pow(width, 2) + Math.pow(height, 2));
        left = width;
        right = 0.0f;
        top = height;
        bottom = 0.0f;
        charTop = (height - width) / 2.0f;
        charBottom = charTop + width;
        drawCursor();
        drawGuide();
    }

    private void next() {
        next(0);
    }

    private void next(int end) {
        isWriting = false;

        float charLength;

        Bitmap nextBeginning = null;
        if (end >= (rbHorizontalWriting.isChecked() ^ sRotate.isChecked() ? width : height)) {
            end = 0;
        }

        Bitmap bm = !sRotate.isChecked() ? bitmap : rotatedBitmap;
        Rect src;
        RectF dst = new RectF();

        if (rbHorizontalWriting.isChecked()) {

            if (!sRotate.isChecked()) {

                if (end == 0) {
                    charLength = toCharSize(right - left);
                    src = rect;
                    dst.right = charWidth;
                    dst.bottom = toCharSize(height);
                } else {
                    nextBeginning = Bitmap.createBitmap(bitmap, end, 0, width - end, height);
                    charLength = toCharSize(end - left);
                    src = new Rect(0, 0, end, height);
                    dst.right = toCharSize(end);
                    dst.bottom = toCharSize(height);
                }

                if (left > 0) {
                    cursorX += columnSpacing;
                }
                if (autoNewline && cursorX + charLength > width) {
                    cursorY += charWidth + lineSpacing;
                    cursorX = 0.0f;
                }

                dst.offset(cursorX - toCharSize(left), cursorY - toCharSize(charTop));

            } else {

                if (end == 0) {
                    charLength = toCharSize(bottom - top);
                    rotatedCanvas.drawBitmap(bitmap, rotation, PAINT_SRC);
                    src = rotatedRect;
                    dst.right = toCharSize(height);
                    dst.bottom = charWidth;
                } else {
                    nextBeginning = Bitmap.createBitmap(bitmap, 0, end, width, height - end);
                    charLength = toCharSize(end - top);
                    rotatedCanvas.drawBitmap(bitmap, rotation, PAINT_SRC);
                    src = new Rect(0, 0, end, width);
                    dst.right = toCharSize(end);
                    dst.bottom = charWidth;
                }

                if (top > 0) {
                    cursorX += columnSpacing;
                }
                if (autoNewline && cursorX + charLength > width) {
                    cursorY += charWidth + lineSpacing;
                    cursorX = 0.0f;
                }

                dst.offset(cursorX - toCharSize(top), cursorY);
            }

            textCanvas.drawBitmap(bm, src, dst, scaler);
            cursorX += this.charLength == -1.0f ? charLength : this.charLength;
            if (!autoNewline && cursorX > width) {
                cursorY += charWidth + lineSpacing;
                cursorX = 0.0f;
            }

        } else {

            if (!sRotate.isChecked()) {

                if (end == 0) {
                    charLength = toCharSize(bottom - top);
                    src = rect;
                    dst.right = charWidth;
                    dst.bottom = toCharSize(height);
                } else {
                    nextBeginning = Bitmap.createBitmap(bitmap, 0, end, width, height - end);
                    charLength = toCharSize(end - top);
                    src = new Rect(0, 0, width, end);
                    dst.right = charWidth;
                    dst.bottom = toCharSize(end);
                }

                if (top > 0) {
                    cursorY += columnSpacing;
                }
                if (autoNewline && cursorY + charLength > height) {
                    cursorX -= charWidth + lineSpacing;
                    cursorY = 0;
                }

                dst.offset(cursorX, cursorY - toCharSize(top));

            } else {

                if (end == 0) {
                    charLength = toCharSize(right - left);
                    rotatedCanvas.drawBitmap(bitmap, rotation, PAINT_SRC);
                    src = rotatedRect;
                    dst.right = toCharSize(height);
                    dst.bottom = charWidth;
                } else {
                    nextBeginning = Bitmap.createBitmap(bitmap, 0, 0, end, height);
                    charLength = toCharSize(right - end);
                    rotatedCanvas.drawBitmap(bitmap, rotation, PAINT_SRC);
                    src = new Rect(0, 0, height, end);
                    dst.right = toCharSize(height);
                    dst.bottom = toCharSize(end);
                }

                if (right < width) {
                    cursorY += columnSpacing;
                }
                if (autoNewline && cursorY + charLength > height) {
                    cursorX -= charWidth + lineSpacing;
                    cursorY = 0;
                }

                dst.offset(cursorX - toCharSize(top), cursorY);

            }

            textCanvas.drawBitmap(bm, src, dst, scaler);
            cursorY += this.charLength == -1.0f ? charLength : this.charLength;
            if (!autoNewline && cursorY > height) {
                cursorX -= charWidth + lineSpacing;
                cursorY = 0;
            }

        }

        ivText.invalidate();
        ivGuide.setVisibility(View.INVISIBLE);
        eraseBitmap(bitmap);
        iv.invalidate();
        vTranslucent.setVisibility(View.INVISIBLE);
        drawCursor();
        left = width;
        right = 0.0f;
        top = height;
        bottom = 0.0f;

        if (nextBeginning != null) {
            eraseBitmap(cuttingBitmap);
            ivCutting.invalidate();
            canvas.drawBitmap(nextBeginning, 0.0f, 0.0f, PAINT_SRC);
            nextBeginning.recycle();
            if (rbHorizontalWriting.isChecked()) {
                if (!sRotate.isChecked()) {
                    left = 0.0f;
                } else {
                    top = 0.0f;
                }
            } else {
                if (!sRotate.isChecked()) {
                    top = 0.0f;
                } else {
                    right = width;
                }
            }
            startWriting();
        }

    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bColor = findViewById(R.id.b_color);
        bNew = findViewById(R.id.b_new);
        iv = findViewById(R.id.iv);
        ivCursor = findViewById(R.id.iv_cursor);
        ivCutting = findViewById(R.id.iv_cutting);
        ivGuide = findViewById(R.id.iv_guide);
        ivPaper = findViewById(R.id.iv_paper);
        ivPreview = findViewById(R.id.iv_preview);
        ivText = findViewById(R.id.iv_text);
        llOptions = findViewById(R.id.ll_options);
        llTools = findViewById(R.id.ll_tools);
        rbHorizontalWriting = findViewById(R.id.rb_horizontal_writing);
        rbVerticalWriting = findViewById(R.id.rb_vertical_writing);
        sbCharLength = findViewById(R.id.sb_char_length);
        sbConcentration = findViewById(R.id.sb_concentration);
        sRotate = findViewById(R.id.s_rotating);
        vTranslucent = findViewById(R.id.v_translucent);

        findViewById(R.id.b_backspace).setOnTouchListener(onTouchBackspaceButtonListener);
        bColor.setOnClickListener(onClickColorButtonListener);
        bColor.setOnLongClickListener(onLongClickColorButtonListener);
        bNew.setOnClickListener(onClickNewButtonListener);
        bNew.setOnLongClickListener(onLongClickNewButtonListener);
        findViewById(R.id.b_next).setOnClickListener(onClickNextButtonListener);
        findViewById(R.id.b_next).setOnTouchListener(onTouchNextButtonListener);
        findViewById(R.id.b_options).setOnClickListener(onClickOptionsButtonListener);
        findViewById(R.id.b_paper).setOnClickListener(onClickPaperButtonListener);
        findViewById(R.id.b_return).setOnClickListener(onClickReturnButtonListener);
        findViewById(R.id.b_space).setOnTouchListener(onTouchSpaceButtonListener);
        findViewById(R.id.fl_iv).setOnTouchListener(onTouchIVsListenerX);
        ivPreview.setOnTouchListener(onTouchPreviewListener);
        ((RadioButton) findViewById(R.id.rb_char_length_auto)).setOnCheckedChangeListener(onCharLengthAutoRadButtCheckedChangeListener);
        ((RadioButton) findViewById(R.id.rb_char_length_custom)).setOnCheckedChangeListener(onCharLengthCustomRadButtCheckedChangeListener);
        rbHorizontalWriting.setOnCheckedChangeListener((compoundButton, isChecked) -> preview());
        ((SeekBar) findViewById(R.id.sb_alias)).setOnSeekBarChangeListener((OnSeekBarProgressChangedListener) progress -> alias = progress);
        ((SeekBar) findViewById(R.id.sb_alpha)).setOnSeekBarChangeListener(onAlphaSeekBarChangeListener);
        sbCharLength.setOnSeekBarChangeListener(onCharLengthSeekBarProgressChangedListener);
        ((SeekBar) findViewById(R.id.sb_char_width)).setOnSeekBarChangeListener(onCharWidthSeekBarProgressChangedListener);
        ((SeekBar) findViewById(R.id.sb_column_spacing)).setOnSeekBarChangeListener(onColSpacingSeekBarProgressChangedListener);
        ((SeekBar) findViewById(R.id.sb_concentration)).setOnSeekBarChangeListener(onConcentrationSeekBarChangeListener);
        ((SeekBar) findViewById(R.id.sb_curvature)).setOnSeekBarChangeListener((OnSeekBarProgressChangedListener) progress -> curvature = progress / 10.0f);
        ((SeekBar) findViewById(R.id.sb_handwriting)).setOnSeekBarChangeListener((OnSeekBarProgressChangedListener) progress -> handwriting = progress);
        ((SeekBar) findViewById(R.id.sb_line_spacing)).setOnSeekBarChangeListener(onLineSpacingSeekBarProgressChangedListener);
        ((SeekBar) findViewById(R.id.sb_softness)).setOnSeekBarChangeListener((OnSeekBarProgressChangedListener) progress -> softness = progress / 10.0f);
        ((SeekBar) findViewById(R.id.sb_stroke_width)).setOnSeekBarChangeListener((OnSeekBarProgressChangedListener) progress -> strokeWidth = progress);
        ((SwitchCompat) findViewById(R.id.s_newline)).setOnCheckedChangeListener((compoundButton, b) -> autoNewline = b);
        sRotate.setOnCheckedChangeListener(onRotateSwitchCheckedChangeListener);

        Resources res = getResources();
        brushBlack = BitmapFactory.decodeResource(res, R.mipmap.brush);
        brushBlackRotated = BitmapFactory.decodeResource(res, R.mipmap.brush_rotated);
        brushRed = BitmapFactory.decodeResource(res, R.mipmap.brush_red);
        brushRedRotated = BitmapFactory.decodeResource(res, R.mipmap.brush_red_rotated);
        brush = brushBlack.copy(Bitmap.Config.ARGB_8888, true); // Cannot use Bitmap.createBitmap(brushBlack); as it is immutable.
    }

    @Override
    protected void onDestroy() {
        canvas = null;
        bitmap.recycle();
        bitmap = null;
        brushBlack.recycle();
        brushBlack = null;
        guideCanvas = null;
        guideBitmap.recycle();
        guideBitmap = null;
        brush.recycle();
        brush = null;
        cuttingCanvas = null;
        cuttingBitmap.recycle();
        cuttingBitmap = null;
        cursorCanvas = null;
        cursorBitmap.recycle();
        cursorBitmap = null;
        paper.recycle();
        paper = null;
        previewCanvas = null;
        previewBitmap.recycle();
        previewBitmap = null;
        rotatedCanvas = null;
        rotatedBitmap.recycle();
        rotatedBitmap = null;
        brushRed.recycle();
        brushRed = null;
        textCanvas = null;
        textBitmap.recycle();
        textBitmap = null;
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasNotLoaded) {
            hasNotLoaded = false;
            Looper.getMainLooper().getQueue().addIdleHandler(() -> {
                load();
                return false;
            });
        }
    }

    private void preview() {
        eraseBitmap(previewBitmap);
        float charLength = (this.charLength == -1.0f ? charWidth : this.charLength);
        if (rbHorizontalWriting.isChecked()) {
            previewCanvas.drawRect(previewX, previewY,
                    previewX + charLength, previewY + charWidth,
                    previewer);
            previewCanvas.drawRect(previewX - columnSpacing, previewY - lineSpacing,
                    previewX + charLength + columnSpacing, previewY + charWidth + lineSpacing,
                    previewer);
        } else {
            previewCanvas.drawRect(previewX, previewY,
                    previewX + charWidth, previewY + charLength,
                    previewer);
            previewCanvas.drawRect(previewX - lineSpacing, previewY - columnSpacing,
                    previewX + charWidth + lineSpacing, previewY + charLength + columnSpacing,
                    previewer);
        }
        ivPreview.invalidate();
    }

    private void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            bitmap.recycle();
        }
    }

    private void setBrushAlpha(@IntRange(from = 0x01, to = 0x10) int alpha) {
        paint.setAlpha(alpha * alpha - 1);
    }

    private void setBrushConcentration(double concentration) {
        Bitmap concentratedBrush = sRotate.isChecked() ? brushBlackRotated : brushBlack;
        int w = concentratedBrush.getWidth(), h = concentratedBrush.getHeight(), area = w * h;
        int[] pixels = new int[area];
        concentratedBrush.getPixels(pixels, 0, w, 0, 0, w, h);
        for (int i = 0; i < area; ++i) {
            if (pixels[i] != Color.TRANSPARENT) {
                pixels[i] = Math.random() < concentration ? brushColor : Color.TRANSPARENT;
            }
        }
        brush.setPixels(pixels, 0, w, 0, 0, w, h);
    }

    private void setBrushColor(@ColorInt int color) {
        if (color == brushColor) {
            return;
        }
        int w = brushBlack.getWidth(), h = brushBlack.getHeight(), area = w * h;
        int[] pixels = new int[area];
        brushBlack.getPixels(pixels, 0, w, 0, 0, w, h);
        for (int i = 0; i < area; ++i) {
            if (pixels[i] != Color.TRANSPARENT) {
                pixels[i] = color;
            }
        }
        brush.setPixels(pixels, 0, w, 0, 0, w, h);
        brushColor = color;
        bColor.setTextColor(color);
    }

    private void startWriting() {
        isWriting = true;
        vTranslucent.setVisibility(View.VISIBLE);
        ivGuide.setVisibility(View.VISIBLE);
    }

    private float toCharSize(float size) {
        return charWidth / width * size;
    }
}