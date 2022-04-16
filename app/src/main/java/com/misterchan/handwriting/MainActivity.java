package com.misterchan.handwriting;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
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
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int TRANSLUCENT = Color.argb(0x7F, 0xFF, 0xFF, 0xFF);
    private static final Rect SQUARE_WITH_SIDE_LENGTH_192 = new Rect(0, 0, 126, 126);

    private Bitmap bitmap;
    private Bitmap blankBitmap;
    private Bitmap brush;
    private Bitmap brushBlack;
    private Bitmap brushBlackRotated;
    private Bitmap brushRed;
    private Bitmap brushRedRotated;
    private Bitmap charBitmap;
    private Bitmap displayBitmap;
    private Bitmap paper;
    private Bitmap previewBitmap;
    private Bitmap previewPaperBitmap;
    private Bitmap textBitmap;
    private Bitmap textBitmapTranslucent;
    private boolean autoNewline = false;
    private boolean backspace = false;
    private boolean hasNotLoaded = true;
    private boolean isNotErasing = true;
    private boolean isWriting = false;
    private boolean space = false;
    private Button bColor;
    private Button bNew;
    private Canvas blankCanvas;
    private Canvas canvas;
    private Canvas charCanvas;
    private Canvas displayCanvas;
    private Canvas previewCanvas;
    private Canvas previewPaperCanvas;
    private Canvas textCanvas;
    private float alias = 8.0f;
    private float backspaceX = 0.0f, backspaceY = 0.0f;
    private float bottom;
    private float brushWidth;
    private float charBottom;
    private float charLength = -1.0f;
    private float charTop;
    private float charWidth = 64.0f;
    private float columnSpacing = 4.0f;
    private float cursorX = 0.0f;
    private float curvature = 2.0f;
    private float end = 0.0f;
    private float left;
    private float lineSpacing = 0.0f;
    private float cursorY = 0.0f;
    private float prevX = 0.0f, prevY = 0.0f;
    private float previewX = 0f, previewY = 0.0f;
    private float right;
    private float size;
    private float spacing = 0.0f;
    private float strokeWidth = 64.0f;
    private float top;
    private ImageView ivCanvas;
    private ImageView ivPreview;
    private int brushColor = Color.BLACK;
    private int handwriting = 16;
    private int width, height;
    private LinearLayout llOptions;
    private LinearLayout llTools;
    private final Matrix matrix = new Matrix();
    private RadioButton rbLtr, rbUtd;
    private Rect rect;
    private Rect rotatedRect;
    private SeekBar sbCharLength;
    private SwitchCompat sRotate;

    private final Paint cursor = new Paint() {

        {
            setStrokeWidth(8.0f);
            setStyle(Style.STROKE);
        }
    };

    private final Paint eraser = new Paint() {

        {
            setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }
    };

    private final Paint paint = new Paint() {

        {
            setStrokeWidth(2.0f);
        }
    };

    private final Paint previewer = new Paint() {

        {
            setStrokeWidth(2.0f);
            setStyle(Style.STROKE);
        }
    };

    ActivityResultCallback<Uri> imageActivityResultCallback = result -> {
        if (result == null) {
            return;
        }
        try (InputStream inputStream = getContentResolver().openInputStream(result)) {
            paper = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(inputStream), width, height, true);
            textCanvas.drawBitmap(paper, 0.0f, 0.0f, paint);
            if (isWriting) {
                isWriting = false;
                clearCanvas(canvas);
            }
            ivCanvas.setImageBitmap(textBitmap);
            previewPaperCanvas.drawBitmap(paper, 0.0f, 0.0f, paint);
            preview();
            setCursor();
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    private final ActivityResultLauncher<String> imageActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), imageActivityResultCallback);

    private final CompoundButton.OnCheckedChangeListener onCharLengthAutoRadButtCheckedChangeListener = (compoundButton, isChecked) -> {
        if (isChecked) {
            sbCharLength.setVisibility(View.GONE);
            charLength = -1.0f;
            preview();
        }
    };

    private final CompoundButton.OnCheckedChangeListener onCharLengthCustomRadButtCheckedChangeListener = (compoundButton, isChecked) -> {
        if (isChecked) {
            charLength = sbCharLength.getProgress();
            sbCharLength.setVisibility(View.VISIBLE);
            preview();
        }
    };

    private final CompoundButton.OnCheckedChangeListener onRotateSwitchCheckedChangeListener = (buttonView, isChecked) ->
            brush = (brushColor == Color.BLACK
                    ? isChecked ? brushBlackRotated : brushBlack
                    : isChecked ? brushRedRotated : brushRed)
                    .copy(Bitmap.Config.ARGB_8888, true);

    private final View.OnClickListener onColorButtonClickListener = view -> {
        isNotErasing = !isNotErasing;
        bColor.setTextColor(isNotErasing ? brushColor : Color.WHITE);
    };

    private final View.OnLongClickListener onColorButtonLongClickListener = view -> {
        if (brushColor == Color.BLACK) {
            brush = (sRotate.isChecked() ? brushRedRotated : brushRed).copy(Bitmap.Config.ARGB_8888, true);
            brushColor = Color.RED;
            if (isNotErasing) {
                bColor.setTextColor(Color.RED);
            }
        } else {
            brush = (sRotate.isChecked() ? brushBlackRotated : brushBlack).copy(Bitmap.Config.ARGB_8888, true);
            brushColor = Color.BLACK;
            if (isNotErasing) {
                bColor.setTextColor(Color.BLACK);
            }
        }
        return true;
    };

    private final View.OnClickListener onNewButtonClickListener = view ->
            Toast.makeText(this, "長按以確定作廢當前紙張並使用新紙張", Toast.LENGTH_LONG).show();

    private final View.OnClickListener onNextButtonClickListener = view -> {
        if (isWriting) {
            next();
        }
    };

    private final View.OnClickListener onOptionsButtonClickListener = view -> {
        if (llOptions.getVisibility() == View.VISIBLE) {
            llOptions.setVisibility(View.GONE);
            llTools.setVisibility(View.VISIBLE);
            ivCanvas.setVisibility(View.VISIBLE);
        } else {
            ivCanvas.setVisibility(View.GONE);
            llTools.setVisibility(View.INVISIBLE);
            llOptions.setVisibility(View.VISIBLE);
            if (previewPaperBitmap == null) {
                previewPaperBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                previewPaperCanvas = new Canvas(previewPaperBitmap);
            }
            bNew.setEnabled(true);
        }
    };

    private final View.OnClickListener onPaperButtonClickListener = view ->
            imageActivityResultLauncher.launch("image/*");

    private final View.OnLongClickListener onNewButtonLongClickListener = view -> {
        view.setEnabled(false);
        if (paper == null) {
            clearCanvas(textCanvas);
        } else {
            textCanvas.drawBitmap(paper, 0.0f, 0.0f, paint);
        }
        ivCanvas.setImageBitmap(textBitmap);
        setCursor();
        return true;
    };

    private final View.OnClickListener onReturnButtonClickListener = view -> {
        if (isWriting) {
            next();
        } else {
            if (rbLtr.isChecked()) {
                cursorX = 0.0f;
                cursorY += charWidth + lineSpacing;
            } else {
                cursorY = 0.0f;
                cursorX -= charWidth + lineSpacing;
            }
            setCursor();
        }
    };

    private final OnProgressChangeListener onCharLengthSeekBarProgressChangeListener = progress -> {
        charLength = progress;
        preview();
    };

    private final OnProgressChangeListener onCharWidthSeekBarProgressChangeListener = progress -> {
        charWidth = progress;
        preview();
    };

    private final OnProgressChangeListener onColSpacingSeekBarProgressChangeListener = progress -> {
        columnSpacing = progress;
        preview();
    };

    private final OnProgressChangeListener onLineSpacingSeekBarProgressChangeListener = progress -> {
        lineSpacing = progress;
        preview();
    };

    private final SeekBar.OnSeekBarChangeListener onConcentrationSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
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
    private final View.OnTouchListener onBackspaceButtonTouchListener = (view, motionEvent) -> {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                backspaceX = motionEvent.getX();
                backspaceY = motionEvent.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (rbLtr.isChecked()) {
                    float x = motionEvent.getX();
                    int deltaX = (int) (backspaceX - x);
                    if (deltaX != 0) {
                        backspace(deltaX);
                        backspaceX = x;
                    }
                } else {
                    float y = motionEvent.getY();
                    int deltaY = (int) (backspaceY - y);
                    if (deltaY != 0) {
                        backspace(deltaY);
                        backspaceY = y;
                    }
                }
                backspace = true;
                break;
            case MotionEvent.ACTION_UP:
                if (backspace) {
                    backspace = false;
                } else {
                    backspace((int) (charWidth / 4.0f));
                }
                break;
        }
        return true;
    };

    @SuppressLint("ClickableViewAccessibility")
    private final View.OnTouchListener onCanvasTouchListener = (view, motionEvent) -> {
        final float x = motionEvent.getX(), y = motionEvent.getY();
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                prevX = x;
                prevY = y;
                brushWidth = 0.0f;
                if (!isWriting) {
                    isWriting = true;
                    clearCanvas(blankCanvas);
                    blankCanvas.drawBitmap(displayBitmap, 0.0f, 0.0f, paint);
                    blankCanvas.drawColor(TRANSLUCENT);
                    if (!sRotate.isChecked()) {
                        blankCanvas.drawLine(0.0f, charTop, width, charTop, paint);
                        blankCanvas.drawLine(0.0f, charBottom, width, charBottom, paint);
                    } else {
                        blankCanvas.drawLine(width / 2.0f, 0.0f, width / 2.0f, height, paint);
                    }
                    textBitmapTranslucent = Bitmap.createBitmap(blankBitmap);
                    blankCanvas.drawBitmap(bitmap, 0.0f, 0.0f, paint);
                }
                break;
            case MotionEvent.ACTION_MOVE:

                if (isNotErasing) {

                    if (x < left) left = x;
                    if (x > right) right = x;
                    if (y < top) top = y;
                    if (y > bottom) bottom = y;

                    float d = (float) Math.sqrt(Math.pow(x - prevX, 2) + Math.pow(y - prevY, 2)),
                            a = d / (float) Math.pow(d, curvature),
                            w = 0.0f,
                            width = (float) Math.pow(1 - d / size, handwriting) * strokeWidth,
                            dBwPD = (width - brushWidth) / d, // Delta brushWidth per d
                            dxPD = (x - prevX) / d, dyPD = (y - prevY) / d; // Delta x per d and delta y per d
                    if (width >= brushWidth) {
                        for (float f = 0; f < d; f += alias) {
                            w = a * (float) Math.pow(f, curvature) * dBwPD + brushWidth;
                            RectF dst = new RectF(dxPD * f + prevX - w, dyPD * f + prevY - w,
                                    dxPD * f + prevX + w, dyPD * f + prevY + w);
                            blankCanvas.drawBitmap(brush, SQUARE_WITH_SIDE_LENGTH_192, dst, paint);
                            canvas.drawBitmap(brush, SQUARE_WITH_SIDE_LENGTH_192, dst, paint);
                        }
                    } else {
                        for (float f = 0; f < d; f += alias) {
                            w = (float) Math.pow(f / a, 1 / curvature) * dBwPD + brushWidth;
                            RectF dst = new RectF(dxPD * f + prevX - w, dyPD * f + prevY - w,
                                    dxPD * f + prevX + w, dyPD * f + prevY + w);
                            canvas.drawBitmap(brush, SQUARE_WITH_SIDE_LENGTH_192, dst, paint);
                            blankCanvas.drawBitmap(brush, SQUARE_WITH_SIDE_LENGTH_192, dst, paint);
                        }
                    }
                    brushWidth = w;
                    prevX = x;
                    prevY = y;

                } else {
                    int strokeWidth = (int) this.strokeWidth,
                            halfStrokeWidth = strokeWidth >> 1,
                            strokeLeft = (int) (x - halfStrokeWidth),
                            strokeTop = (int) (y - halfStrokeWidth);
                    canvas.drawRect(strokeLeft, strokeTop, strokeLeft + strokeWidth, strokeTop + strokeWidth, eraser);
                    if (paper == null) {
                        blankCanvas.drawRect(strokeLeft, strokeTop, strokeLeft + strokeWidth, strokeTop + strokeWidth, eraser);
                    } else {
                        Bitmap bm = Bitmap.createBitmap(textBitmapTranslucent, strokeLeft, strokeTop, strokeWidth, strokeWidth);
                        blankCanvas.drawBitmap(bm, strokeLeft, strokeTop, paint);
                        bm.recycle();
                    }

                }

                break;
            case MotionEvent.ACTION_UP:
                return true;
        }
        ivCanvas.setImageBitmap(blankBitmap);
        return true;
    };

    @SuppressLint("ClickableViewAccessibility")
    private final View.OnTouchListener onNextButtonTouchListener = (view, motionEvent) -> {
        if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
            int[] canvasLocation = new int[2];
            ivCanvas.getLocationOnScreen(canvasLocation);
            if (motionEvent.getRawY() < canvasLocation[1] + height) {
                if (isWriting) {
                    next();
                }
                int[] buttonLocation = new int[2];
                view.getLocationOnScreen(buttonLocation);
                cursorX = (buttonLocation[0] + motionEvent.getX() - canvasLocation[0]) - charWidth / 8.0f;
                cursorY = (buttonLocation[1] + motionEvent.getY() - canvasLocation[1]) - charWidth / 2.0f;
                setCursor(true);
            }
        }
        return false;
    };

    @SuppressLint("ClickableViewAccessibility")
    private final View.OnTouchListener onPreviewTouchListener = (view, motionEvent) -> {
        previewX = motionEvent.getX();
        previewY = motionEvent.getY();
        preview();
        return true;
    };

    @SuppressLint("ClickableViewAccessibility")
    private final View.OnTouchListener onSpaceButtonTouchListener = (view, motionEvent) -> {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isWriting) {
                    spacing = rbLtr.isChecked() ^ sRotate.isChecked() ? motionEvent.getX() : motionEvent.getY();
                    end = rbLtr.isChecked() ^ sRotate.isChecked() ? width : height;
                } else {
                    spacing = rbLtr.isChecked() ? motionEvent.getX() : motionEvent.getY();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                space = true;
                if (isWriting) {
                    if (rbLtr.isChecked() ^ sRotate.isChecked()) {
                        float x = motionEvent.getX();
                        clearCanvas(blankCanvas);
                        blankCanvas.drawBitmap(bitmap, 0.0f, 0.0f, paint);
                        blankCanvas.drawLine(end += x - spacing, 0.0f, end, height, paint);
                        ivCanvas.setImageBitmap(blankBitmap);
                        spacing = x;
                    } else {
                        float y = motionEvent.getY();
                        clearCanvas(blankCanvas);
                        blankCanvas.drawBitmap(bitmap, 0.0f, 0.0f, paint);
                        blankCanvas.drawLine(0.0f, end += y - spacing, width, end, paint);
                        ivCanvas.setImageBitmap(blankBitmap);
                        spacing = y;
                    }
                } else {
                    if (rbLtr.isChecked()) {
                        float x = motionEvent.getX();
                        cursorX += x - spacing;
                        spacing = x;
                    } else {
                        float y = motionEvent.getY();
                        cursorY += y - spacing;
                        spacing = y;
                    }
                    setCursor();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (space) {
                    space = false;
                    if (isWriting) {
                        next((int) end);
                    }
                } else {
                    if (isWriting) {
                        next((int) ((right - left) / 4.0f * 3.0f));
                    } else {
                        if (rbLtr.isChecked()) {
                            cursorX += charWidth / 4.0f;
                        } else {
                            cursorY += charWidth / 4.0f;
                        }
                    }
                    setCursor();
                }
                break;
        }
        return true;
    };

    private void backspace(int size) {
        if (isWriting) {
            isWriting = false;
            clearCanvas(canvas);
            ivCanvas.setImageBitmap(displayBitmap);
        } else {
            if (rbLtr.isChecked()) {

                if (cursorX > 0) {
                    cursorX -= size;
                } else {
                    cursorY -= charWidth;
                    cursorX = width - size;
                }

                if (paper == null) {
                    textCanvas.drawRect(cursorX, cursorY, cursorX + size, cursorY + charWidth, eraser);
                } else if (0 <= cursorX && cursorX < width) {
                    Bitmap bm = Bitmap.createBitmap(paper, (int) cursorX, (int) cursorY, Math.abs(size), (int) charWidth);
                    textCanvas.drawBitmap(bm, cursorX, cursorY, paint);
                    bm.recycle();
                }

            } else {

                if (cursorY > 0) {
                    cursorY -= size;
                } else {
                    cursorX -= charWidth;
                    cursorY = height - size;
                }

                if (paper == null) {
                    textCanvas.drawRect(cursorX, cursorY, cursorX + charWidth, cursorY + size, eraser);
                } else if (0 <= cursorY && cursorY < height) {
                    Bitmap bm = Bitmap.createBitmap(paper, (int) cursorX, (int) cursorY, (int) charWidth, Math.abs(size));
                    textCanvas.drawBitmap(bm, cursorX, cursorY, paint);
                    bm.recycle();
                }

            }

            ivCanvas.setImageBitmap(textBitmap);
            setCursor();
        }
    }

    private void clearCanvas(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    }

    private void load() {
        width = ivCanvas.getWidth();
        height = ivCanvas.getHeight();
        rect = new Rect(0, 0, width, height);
        rotatedRect = new Rect(0, 0, height, width);
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        matrix.setRotate(-90.0f);

        blankBitmap = Bitmap.createBitmap(bitmap);
        blankCanvas = new Canvas(blankBitmap);

        charBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        charCanvas = new Canvas(charBitmap);

        textBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        textCanvas = new Canvas(textBitmap);

        textBitmapTranslucent = Bitmap.createBitmap(textBitmap);

        displayBitmap = Bitmap.createBitmap(textBitmap);
        displayCanvas = new Canvas(displayBitmap);

        size = (float) Math.sqrt(Math.pow(width, 2) + Math.pow(height, 2));
        left = width;
        right = 0.0f;
        top = height;
        bottom = 0.0f;
        charTop = (height - width) / 2.0f;
        charBottom = charTop + width;
        setCursor();
    }

    private void next() {
        next(0);
    }

    private void next(int end) {
        isWriting = false;
        clearCanvas(charCanvas);

        float charLength;

        Bitmap nextBeginning = null;
        if (end >= (rbLtr.isChecked() ^ sRotate.isChecked() ? width : height)) {
            end = 0;
        }

        if (rbLtr.isChecked()) {

            if (!sRotate.isChecked()) {

                if (end == 0) {
                    charLength = toCharSize(right - left);
                    charCanvas.drawBitmap(bitmap,
                            rect,
                            new RectF(0.0f, 0.0f, charWidth, toCharSize(height)),
                            paint);
                } else {
                    nextBeginning = Bitmap.createBitmap(bitmap, end, 0, width - end, height);
                    charLength = toCharSize(end - left);
                    charCanvas.drawBitmap(bitmap,
                            new Rect(0, 0, end, height),
                            new RectF(0.0f, 0.0f, toCharSize(end), toCharSize(height)),
                            paint);
                }

                if (left > 0) {
                    cursorX += columnSpacing;
                }
                if (autoNewline && cursorX + charLength > width) {
                    cursorY += charWidth + lineSpacing;
                    cursorX = 0.0f;
                }

                textCanvas.drawBitmap(charBitmap, cursorX - toCharSize(left), cursorY - toCharSize(charTop), paint);

            } else {

                Bitmap rotatedBitmap;
                if (end == 0) {
                    charLength = toCharSize(bottom - top);
                    rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
                    charCanvas.drawBitmap(rotatedBitmap,
                            rotatedRect,
                            new RectF(0.0f, 0.0f, toCharSize(height), charWidth),
                            paint);
                } else {
                    nextBeginning = Bitmap.createBitmap(bitmap, 0, end, width, height - end);
                    charLength = toCharSize(end - top);
                    rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, end, matrix, true);
                    charCanvas.drawBitmap(rotatedBitmap,
                            new Rect(0, 0, end, width),
                            new RectF(0.0f, 0.0f, toCharSize(end), charWidth),
                            paint);
                }
                rotatedBitmap.recycle();

                if (top > 0) {
                    cursorX += columnSpacing;
                }
                if (autoNewline && cursorX + charLength > width) {
                    cursorY += charWidth + lineSpacing;
                    cursorX = 0.0f;
                }

                textCanvas.drawBitmap(charBitmap, cursorX - toCharSize(top), cursorY, paint);
            }

            cursorX += this.charLength == -1.0f ? charLength : this.charLength;
            if (!autoNewline && cursorX > width) {
                cursorY += charWidth + lineSpacing;
                cursorX = 0.0f;
            }

        } else {

            if (!sRotate.isChecked()) {

                if (end == 0) {
                    charLength = toCharSize(bottom - top);
                    charCanvas.drawBitmap(bitmap,
                            rect,
                            new RectF(0.0f, 0.0f, charWidth, toCharSize(height)),
                            paint);
                } else {
                    nextBeginning = Bitmap.createBitmap(bitmap, 0, end, width, height - end);
                    charLength = toCharSize(end - top);
                    charCanvas.drawBitmap(bitmap,
                            new Rect(0, 0, width, end),
                            new RectF(0.0f, 0.0f, charWidth, toCharSize(end)),
                            paint);
                }

                if (top > 0) {
                    cursorY += columnSpacing;
                }
                if (autoNewline && cursorY + charLength > height) {
                    cursorX -= charWidth + lineSpacing;
                    cursorY = 0;
                }

                textCanvas.drawBitmap(charBitmap, cursorX, cursorY - toCharSize(top), paint);

            } else {

                Bitmap rotatedBitmap;
                if (end == 0) {
                    charLength = toCharSize(right - left);
                    rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
                    charCanvas.drawBitmap(rotatedBitmap,
                            rotatedRect,
                            new RectF(0.0f, 0.0f, toCharSize(height), charWidth),
                            paint);
                } else {
                    nextBeginning = Bitmap.createBitmap(bitmap, 0, 0, end, height);
                    charLength = toCharSize(right - end);
                    rotatedBitmap = Bitmap.createBitmap(bitmap, end, 0, width - end, height, matrix, true);
                    charCanvas.drawBitmap(rotatedBitmap,
                            new Rect(0, 0, height, end),
                            new RectF(0.0f, 0.0f, toCharSize(height), toCharSize(end)),
                            paint);
                }
                rotatedBitmap.recycle();

                if (right < width) {
                    cursorY += columnSpacing;
                }
                if (autoNewline && cursorY + charLength > height) {
                    cursorX -= charWidth + lineSpacing;
                    cursorY = 0;
                }

                textCanvas.drawBitmap(charBitmap, cursorX - toCharSize(top), cursorY, paint);

            }

            cursorY += this.charLength == -1.0f ? charLength : this.charLength;
            if (!autoNewline && cursorY > height) {
                cursorX -= charWidth + lineSpacing;
                cursorY = 0;
            }

        }
        clearCanvas(canvas);
        setCursor();
        left = width;
        right = 0.0f;
        top = height;
        bottom = 0.0f;

        if (nextBeginning != null) {
            canvas.drawBitmap(nextBeginning, 0.0f, 0.0f, paint);
            if (rbLtr.isChecked()) {
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
        ivCanvas = findViewById(R.id.iv_canvas);
        ivPreview = findViewById(R.id.iv_preview);
        llOptions = findViewById(R.id.ll_options);
        llTools = findViewById(R.id.ll_tools);
        rbLtr = findViewById(R.id.rb_ltr);
        rbUtd = findViewById(R.id.rb_utd);
        sbCharLength = findViewById(R.id.sb_char_length);
        sRotate = findViewById(R.id.s_rotate);

        findViewById(R.id.b_backspace).setOnTouchListener(onBackspaceButtonTouchListener);
        bColor.setOnClickListener(onColorButtonClickListener);
        bNew.setOnClickListener(onNewButtonClickListener);
        bNew.setOnLongClickListener(onNewButtonLongClickListener);
        findViewById(R.id.b_next).setOnClickListener(onNextButtonClickListener);
        findViewById(R.id.b_next).setOnTouchListener(onNextButtonTouchListener);
        findViewById(R.id.b_options).setOnClickListener(onOptionsButtonClickListener);
        findViewById(R.id.b_paper).setOnClickListener(onPaperButtonClickListener);
        findViewById(R.id.b_return).setOnClickListener(onReturnButtonClickListener);
        findViewById(R.id.b_space).setOnTouchListener(onSpaceButtonTouchListener);
        ivCanvas.setOnTouchListener(onCanvasTouchListener);
        ivPreview.setOnTouchListener(onPreviewTouchListener);
        ((RadioButton) findViewById(R.id.rb_char_length_auto)).setOnCheckedChangeListener(onCharLengthAutoRadButtCheckedChangeListener);
        ((RadioButton) findViewById(R.id.rb_char_length_custom)).setOnCheckedChangeListener(onCharLengthCustomRadButtCheckedChangeListener);
        ((SeekBar) findViewById(R.id.sb_alias)).setOnSeekBarChangeListener((OnProgressChangeListener) progress -> alias = progress);
        sbCharLength.setOnSeekBarChangeListener(onCharLengthSeekBarProgressChangeListener);
        ((SeekBar) findViewById(R.id.sb_char_width)).setOnSeekBarChangeListener(onCharWidthSeekBarProgressChangeListener);
        ((SeekBar) findViewById(R.id.sb_column_spacing)).setOnSeekBarChangeListener(onColSpacingSeekBarProgressChangeListener);
        ((SeekBar) findViewById(R.id.sb_concentration)).setOnSeekBarChangeListener(onConcentrationSeekBarChangeListener);
        ((SeekBar) findViewById(R.id.sb_curvature)).setOnSeekBarChangeListener((OnProgressChangeListener) progress -> curvature = progress / 10.0f);
        ((SeekBar) findViewById(R.id.sb_handwriting)).setOnSeekBarChangeListener((OnProgressChangeListener) progress -> handwriting = progress);
        ((SeekBar) findViewById(R.id.sb_line_spacing)).setOnSeekBarChangeListener(onLineSpacingSeekBarProgressChangeListener);
        ((SeekBar) findViewById(R.id.sb_stroke_width)).setOnSeekBarChangeListener((OnProgressChangeListener) progress -> strokeWidth = progress);
        ((SwitchCompat) findViewById(R.id.s_newline)).setOnCheckedChangeListener((compoundButton, b) -> autoNewline = b);
        sRotate.setOnCheckedChangeListener(onRotateSwitchCheckedChangeListener);

        Resources res = getResources();
        brushBlack = BitmapFactory.decodeResource(res, R.mipmap.brush);
        brushBlackRotated = BitmapFactory.decodeResource(res, R.mipmap.brush_rotated);
        brushRed = BitmapFactory.decodeResource(res, R.mipmap.brush_red);
        brushRedRotated = BitmapFactory.decodeResource(res, R.mipmap.brush_red_rotated);
        brush = brushBlack.copy(Bitmap.Config.ARGB_8888, true);
    }

    @Override
    protected void onDestroy() {
        canvas = null;
        bitmap.recycle();
        bitmap = null;
        brushBlack.recycle();
        brushBlack = null;
        blankCanvas = null;
        blankBitmap.recycle();
        blankBitmap = null;
        brush.recycle();
        brush = null;
        charCanvas = null;
        charBitmap.recycle();
        charBitmap = null;
        displayCanvas = null;
        displayBitmap.recycle();
        displayBitmap = null;
        paper.recycle();
        paper = null;
        previewCanvas = null;
        previewBitmap.recycle();
        previewBitmap = null;
        previewPaperCanvas = null;
        previewPaperBitmap.recycle();
        previewPaperBitmap = null;
        brushRed.recycle();
        brushRed = null;
        textCanvas = null;
        textBitmap.recycle();
        textBitmap = null;
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasNotLoaded && hasFocus) {
            hasNotLoaded = false;
            load();
        }
    }

    private void preview() {
        if (previewBitmap == null) {
            int previewWidth = ivPreview.getWidth(), previewHeight = ivPreview.getHeight();
            previewBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
            previewCanvas = new Canvas(previewBitmap);
            previewX = previewWidth >> 1;
            previewY = previewHeight >> 1;
        }
        clearCanvas(previewCanvas);
        previewCanvas.drawBitmap(previewPaperBitmap, 0.0f, 0.0f, previewer);
        float charLength = (this.charLength == -1.0f ? charWidth : this.charLength);
        if (rbLtr.isChecked()) {
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
        ivPreview.setImageBitmap(previewBitmap);
    }

    private void setBrushConcentration(double concentration) {
        int brushWidth = brushBlack.getWidth(), brushHeight = brushBlack.getHeight();
        for (int y = 0; y < brushHeight; ++y) {
            for (int x = 0; x < brushWidth; ++x) {
                if (brushBlack.getPixel(x, y) != Color.TRANSPARENT) {
                    brush.setPixel(x, y, Math.random() < concentration ? brushColor : Color.TRANSPARENT);
                }
            }
        }
    }

    private void setBrushColor(int color) {
        if (color == brushColor) {
            return;
        }
        int brushWidth = brushBlack.getWidth(), brushHeight = brushBlack.getHeight();
        for (int y = 0; y < brushHeight; ++y) {
            for (int x = 0; x < brushWidth; ++x) {
                if (brushBlack.getPixel(x, y) != Color.TRANSPARENT) {
                    brush.setPixel(x, y, color);
                }
            }
        }
        brushColor = color;
        bColor.setTextColor(color);
    }

    private void setCursor() {
        setCursor(false);
    }

    private void setCursor(boolean style) {
        clearCanvas(displayCanvas);
        displayCanvas.drawBitmap(textBitmap, 0.0f, 0.0f, paint);
        displayCanvas.drawRect(cursorX, style ? cursorY : cursorY + charWidth,
                cursorX + charWidth, cursorY + charWidth,
                cursor);
        ivCanvas.setImageBitmap(displayBitmap);
    }

    private float toCharSize(float size) {
        return charWidth / width * size;
    }
}