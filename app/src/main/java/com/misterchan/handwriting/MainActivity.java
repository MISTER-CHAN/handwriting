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
import android.widget.CompoundButton;
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

import com.misterchan.handwriting.databinding.ActivityMainBinding;

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

    private ActivityMainBinding binding;
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
    private float strokeWidth = 144.0f;
    private float top;
    private int brushColor = Color.BLACK;
    private int handwriting = 16;
    private int width, height;
    private final Matrix rotation = new Matrix();
    private Rect rect;
    private Rect rotatedRect;

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
            binding.ivPaper.setImageBitmap(paper);
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
            binding.sbCharLength.setVisibility(View.GONE);
            charLength = -1.0f;
            preview();
        }
    };

    private final CompoundButton.OnCheckedChangeListener onCharLengthCustomRadButtCheckedChangeListener = (buttonView, isChecked) -> {
        if (isChecked) {
            charLength = binding.sbCharLength.getProgress();
            binding.sbCharLength.setVisibility(View.VISIBLE);
            preview();
        }
    };

    private final CompoundButton.OnCheckedChangeListener onRotateSwitchCheckedChangeListener = (buttonView, isChecked) -> {
//        brush = (brushColor == Color.BLACK
//                ? isChecked ? brushBlackRotated : brushBlack
//                : isChecked ? brushRedRotated : brushRed)
//                .copy(Bitmap.Config.ARGB_8888, true);
//        setBrushConcentration((double) sbConcentration.getProgress() / 10.0);
        drawGuide();
    };

    private final View.OnClickListener onColorButtonClickListener = v -> {
        isNotErasing = !isNotErasing;
        binding.bColor.setTextColor(isNotErasing ? brushColor : Color.WHITE);
    };

    private final View.OnLongClickListener onColorButtonLongClickListener = v -> {
//        brush.recycle();
        if (brushColor == Color.BLACK) {
//            brush = (binding.sRotated.isChecked() ? brushRedRotated : brushRed).copy(Bitmap.Config.ARGB_8888, true);
            brushColor = Color.RED;
            if (isNotErasing) {
                binding.bColor.setTextColor(Color.RED);
            }
        } else {
//            brush = (binding.sRotated.isChecked() ? brushBlackRotated : brushBlack).copy(Bitmap.Config.ARGB_8888, true);
            brushColor = Color.BLACK;
            if (isNotErasing) {
                binding.bColor.setTextColor(Color.BLACK);
            }
        }
        paint.setColor(brushColor);
//        setBrushConcentration((double) sbConcentration.getProgress() / 10.0);
        return true;
    };

    private final View.OnClickListener onNewButtonClickListener = v ->
            Toast.makeText(this, "長按以確定作廢當前紙張並使用新紙張", Toast.LENGTH_LONG).show();

    private final View.OnClickListener onNextButtonClickListener = v -> {
        if (isWriting) {
            next();
        }
    };

    private final View.OnClickListener onPaperButtonClickListener = v ->
            pickMedia.launch(pickVisualMediaRequest);

    private final View.OnLongClickListener onNewButtonLongClickListener = v -> {
        v.setEnabled(false);
        eraseBitmap(textBitmap);
        binding.iv.invalidate();
        drawCursor();
        return true;
    };

    private final View.OnClickListener onReturnButtonClickListener = v -> {
        if (isWriting) {
            next();
        } else {
            if (binding.rbHorizontalWriting.isChecked()) {
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
    private final View.OnTouchListener onBackspaceButtonTouchListener = new View.OnTouchListener() {
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
                    if (binding.rbHorizontalWriting.isChecked()) {
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
    private final View.OnTouchListener onIVsTouchListener = new View.OnTouchListener() {
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
            binding.iv.invalidate();
            return true;
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    private final View.OnTouchListener onIVsTouchListenerX = new View.OnTouchListener() {
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
                    binding.iv.invalidate();
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
    private final View.OnTouchListener onNextButtonTouchListener = (v, event) -> {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            int[] canvasLocation = new int[2];
            binding.iv.getLocationOnScreen(canvasLocation);
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
    private final View.OnTouchListener onSpaceButtonTouchListener = new View.OnTouchListener() {
        private float spacing = 0.0f, end = 0.0f;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    if (isWriting) {
                        if (binding.rbHorizontalWriting.isChecked() ^ binding.sRotated.isChecked()) {
                            spacing = event.getX();
                            end = width;
                        } else {
                            spacing = event.getY();
                            end = height;
                        }
                    } else {
                        spacing = binding.rbHorizontalWriting.isChecked() ? event.getX() : event.getY();
                    }
                }
                case MotionEvent.ACTION_MOVE -> {
                    space = true;
                    if (isWriting) {
                        eraseBitmap(cuttingBitmap);
                        if (binding.rbHorizontalWriting.isChecked() ^ binding.sRotated.isChecked()) {
                            float x = event.getX();
                            cuttingCanvas.drawLine(end += x - spacing, 0.0f, end, height, cutter);
                            spacing = x;
                        } else {
                            float y = event.getY();
                            cuttingCanvas.drawLine(0.0f, end += y - spacing, width, end, cutter);
                            spacing = y;
                        }
                        binding.ivCutting.invalidate();
                    } else {
                        if (binding.rbHorizontalWriting.isChecked()) {
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
                            if (binding.rbHorizontalWriting.isChecked()) {
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
    private final View.OnTouchListener onPreviewTouchListener = (v, event) -> {
        previewX = event.getX();
        previewY = event.getY();
        preview();
        return true;
    };

    @SuppressLint("ClickableViewAccessibility")
    private final View.OnClickListener onOptionsButtonClickListener = v -> {
        if (binding.llOptions.getVisibility() == View.VISIBLE) {
            binding.llOptions.setVisibility(View.GONE);
            binding.llTools.setVisibility(View.VISIBLE);
            binding.ivPreview.setVisibility(View.INVISIBLE);
        } else {
            binding.ivPreview.setVisibility(View.VISIBLE);
            binding.llTools.setVisibility(View.INVISIBLE);
            binding.llOptions.setVisibility(View.VISIBLE);
            binding.bNew.setEnabled(true);
        }
    };

    private void backspace(int size) {
        if (isWriting) {
            isWriting = false;
            eraseBitmap(bitmap);
            binding.iv.invalidate();
            binding.ivGuide.setVisibility(View.INVISIBLE);
            binding.vTranslucent.setVisibility(View.INVISIBLE);
        } else {
            if (binding.rbHorizontalWriting.isChecked()) {

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

            binding.ivText.invalidate();
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
        binding.ivCursor.invalidate();
    }

    private void drawGuide() {
        eraseBitmap(guideBitmap);
        if (!binding.sRotated.isChecked()) {
            guideCanvas.drawLine(0.0f, charTop, width, charTop, guide);
            guideCanvas.drawLine(0.0f, charBottom, width, charBottom, guide);
        } else {
            guideCanvas.drawLine(width / 2.0f, 0.0f, width / 2.0f, height, guide);
        }
        binding.ivGuide.invalidate();
    }

    private void eraseBitmap(Bitmap bitmap) {
        bitmap.eraseColor(Color.TRANSPARENT);
    }

    private void load() {
        width = binding.iv.getWidth();
        height = binding.iv.getHeight();
        rect = new Rect(0, 0, width, height);
        rotatedRect = new Rect(0, 0, height, width);
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        rotation.postRotate(-90.0f);
        rotation.postTranslate(0.0f, width);
        binding.iv.setImageBitmap(bitmap);

        rotatedBitmap = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);
        rotatedCanvas = new Canvas(rotatedBitmap);

        textBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        textCanvas = new Canvas(textBitmap);
        binding.ivText.setImageBitmap(textBitmap);

        cursorBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        cursorCanvas = new Canvas(cursorBitmap);
        binding.ivCursor.setImageBitmap(cursorBitmap);

        previewBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        previewCanvas = new Canvas(previewBitmap);
        binding.ivPreview.setImageBitmap(previewBitmap);
        previewX = width >> 1;
        previewY = 0;

        guideBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        guideCanvas = new Canvas(guideBitmap);
        binding.ivGuide.setImageBitmap(guideBitmap);

        cuttingBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        cuttingCanvas = new Canvas(cuttingBitmap);
        binding.ivCutting.setImageBitmap(cuttingBitmap);

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
        if (end >= (binding.rbHorizontalWriting.isChecked() ^ binding.sRotated.isChecked() ? width : height)) {
            end = 0;
        }

        Bitmap bm = !binding.sRotated.isChecked() ? bitmap : rotatedBitmap;
        Rect src;
        RectF dst = new RectF();

        if (binding.rbHorizontalWriting.isChecked()) {

            if (!binding.sRotated.isChecked()) {

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

            if (!binding.sRotated.isChecked()) {

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

        binding.ivText.invalidate();
        binding.ivGuide.setVisibility(View.INVISIBLE);
        eraseBitmap(bitmap);
        binding.iv.invalidate();
        binding.vTranslucent.setVisibility(View.INVISIBLE);
        drawCursor();
        left = width;
        right = 0.0f;
        top = height;
        bottom = 0.0f;

        if (nextBeginning != null) {
            eraseBitmap(cuttingBitmap);
            binding.ivCutting.invalidate();
            canvas.drawBitmap(nextBeginning, 0.0f, 0.0f, PAINT_SRC);
            nextBeginning.recycle();
            if (binding.rbHorizontalWriting.isChecked()) {
                if (!binding.sRotated.isChecked()) {
                    left = 0.0f;
                } else {
                    top = 0.0f;
                }
            } else {
                if (!binding.sRotated.isChecked()) {
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
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bBackspace.setOnTouchListener(onBackspaceButtonTouchListener);
        binding.bColor.setOnClickListener(onColorButtonClickListener);
        binding.bColor.setOnLongClickListener(onColorButtonLongClickListener);
        binding.bNew.setOnClickListener(onNewButtonClickListener);
        binding.bNew.setOnLongClickListener(onNewButtonLongClickListener);
        binding.bNext.setOnClickListener(onNextButtonClickListener);
        binding.bNext.setOnTouchListener(onNextButtonTouchListener);
        binding.bOptions.setOnClickListener(onOptionsButtonClickListener);
        binding.bPaper.setOnClickListener(onPaperButtonClickListener);
        binding.bReturn.setOnClickListener(onReturnButtonClickListener);
        binding.bSpace.setOnTouchListener(onSpaceButtonTouchListener);
        binding.flIv.setOnTouchListener(onIVsTouchListenerX);
        binding.ivPreview.setOnTouchListener(onPreviewTouchListener);
        binding.rbCharLengthAuto.setOnCheckedChangeListener(onCharLengthAutoRadButtCheckedChangeListener);
        binding.rbCharLengthCustom.setOnCheckedChangeListener(onCharLengthCustomRadButtCheckedChangeListener);
        binding.rbHorizontalWriting.setOnCheckedChangeListener((compoundButton, isChecked) -> preview());
//        ((SeekBar) findViewById(R.id.sb_alias)).setOnSeekBarChangeListener((OnSeekBarProgressChangedListener) progress -> alias = progress);
//        ((SeekBar) findViewById(R.id.sb_alpha)).setOnSeekBarChangeListener(onAlphaSeekBarChangeListener);
        binding.sbCharLength.setOnSeekBarChangeListener(onCharLengthSeekBarProgressChangedListener);
        binding.sbCharWidth.setOnSeekBarChangeListener(onCharWidthSeekBarProgressChangedListener);
        binding.sbColumnSpacing.setOnSeekBarChangeListener(onColSpacingSeekBarProgressChangedListener);
//        ((SeekBar) findViewById(R.id.sb_concentration)).setOnSeekBarChangeListener(onConcentrationSeekBarChangeListener);
//        ((SeekBar) findViewById(R.id.sb_curvature)).setOnSeekBarChangeListener((OnSeekBarProgressChangedListener) progress -> curvature = progress / 10.0f);
//        ((SeekBar) findViewById(R.id.sb_handwriting)).setOnSeekBarChangeListener((OnSeekBarProgressChangedListener) progress -> handwriting = progress);
        binding.sbLineSpacing.setOnSeekBarChangeListener(onLineSpacingSeekBarProgressChangedListener);
        binding.sbSoftness.setOnSeekBarChangeListener((OnSeekBarProgressChangedListener) progress -> softness = progress / 10.0f);
        binding.sbStrokeWidth.setOnSeekBarChangeListener((OnSeekBarProgressChangedListener) progress -> strokeWidth = progress);
        binding.sNewline.setOnCheckedChangeListener((compoundButton, b) -> autoNewline = b);
        binding.sRotated.setOnCheckedChangeListener(onRotateSwitchCheckedChangeListener);

//        Resources res = getResources();
//        brushBlack = BitmapFactory.decodeResource(res, R.mipmap.brush);
//        brushBlackRotated = BitmapFactory.decodeResource(res, R.mipmap.brush_rotated);
//        brushRed = BitmapFactory.decodeResource(res, R.mipmap.brush_red);
//        brushRedRotated = BitmapFactory.decodeResource(res, R.mipmap.brush_red_rotated);
//        brush = brushBlack.copy(Bitmap.Config.ARGB_8888, true); // Cannot use Bitmap.createBitmap(brushBlack); as it is immutable.
    }

    @Override
    protected void onDestroy() {
        canvas = null;
        bitmap.recycle();
        bitmap = null;
//        brush.recycle();
//        brush = null;
//        brushBlack.recycle();
//        brushBlack = null;
//        brushRed.recycle();
//        brushRed = null;
        guideCanvas = null;
        guideBitmap.recycle();
        guideBitmap = null;
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
        if (binding.rbHorizontalWriting.isChecked()) {
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
        binding.ivPreview.invalidate();
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
        Bitmap concentratedBrush = binding.sRotated.isChecked() ? brushBlackRotated : brushBlack;
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
        binding.bColor.setTextColor(color);
    }

    private void startWriting() {
        isWriting = true;
        binding.vTranslucent.setVisibility(View.VISIBLE);
        binding.ivGuide.setVisibility(View.VISIBLE);
    }

    private float toCharSize(float size) {
        return charWidth / width * size;
    }
}