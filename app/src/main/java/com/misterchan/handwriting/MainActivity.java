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
    private Bitmap blackBrush;
    private Bitmap blankBitmap;
    private Bitmap brush;
    private Bitmap charBitmap;
    private Bitmap displayBitmap;
    private Bitmap paper;
    private Bitmap previewBitmap;
    private Bitmap previewPaperBitmap;
    private Bitmap redBrush;
    private Bitmap textBitmap;
    private boolean autoNewline = false;
    private boolean backspace = false;
    private boolean hasNotLoaded = true;
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
    private float alias = 8f;
    private float backspaceX = 0f, backspaceY = 0f;
    private float bottom;
    private float brushWidth;
    private float charBottom;
    private float charLength = -1f;
    private float charTop;
    private float charWidth = 64f;
    private float columnSpacing = 4f;
    private float cursorX = 0f;
    private float left;
    private float lineSpacing = 0f;
    private float cursorY = 0f;
    private float prevX = 0f, prevY = 0f;
    private float previewX = 0f, previewY = 0f;
    private float ratio = 2f;
    private float right;
    private float spaceX = 0f, spaceY = 0f;
    private float size;
    private float strokeWidth = 96f;
    private float top;
    private ImageView ivCanvas;
    private ImageView ivPreview;
    private int brushColor = Color.BLACK;
    private int handwriting = 16;
    private int width, height;
    private LinearLayout llOptions;
    private LinearLayout llTools;
    private Matrix matrix = new Matrix();
    private RadioButton rbLtr, rbUtd;
    private Rect rect;
    private Rect rotatedRect;
    private SeekBar sbCharLength;

    private final Paint cursor = new Paint() {
        {
            setStrokeWidth(8f);
            setStyle(Style.STROKE);
        }
    };

    private final Paint eraser = new Paint() {
        {
            setColor(Color.WHITE);
        }
    };

    private final Paint paint = new Paint() {
        {
            setStrokeWidth(2f);
        }
    };

    private final Paint previewer = new Paint() {
        {
            setStrokeWidth(2f);
            setStyle(Style.STROKE);
        }
    };

    ActivityResultCallback<Uri> imageActivityResultCallback = result -> {
        try (InputStream inputStream = getContentResolver().openInputStream(result)) {
            paper = BitmapFactory.decodeStream(inputStream);
            paper = Bitmap.createScaledBitmap(paper, width, height, true);
            textCanvas.drawBitmap(paper, 0f, 0f, paint);
            ivCanvas.setImageBitmap(textBitmap);
            previewPaperCanvas.drawBitmap(paper, 0f, 0f, paint);
            preview();
            setCursor();
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    private final ActivityResultLauncher<String> imageActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), imageActivityResultCallback);

    private final CompoundButton.OnCheckedChangeListener onCharLengthAutoCompButtCheckedChangeListener = (compoundButton, isChecked) -> {
        if (isChecked) {
            sbCharLength.setVisibility(View.GONE);
            charLength = -1f;
            preview();
        }
    };

    private final CompoundButton.OnCheckedChangeListener onCharLengthCustomCompButtCheckedChangeListener = (compoundButton, isChecked) -> {
        if (isChecked) {
            charLength = sbCharLength.getProgress();
            sbCharLength.setVisibility(View.VISIBLE);
            preview();
        }
    };

    private final View.OnClickListener onColorButtonClickListener = view -> {
        if (brushColor == Color.BLACK) {
            brush = redBrush.copy(Bitmap.Config.ARGB_8888, true);
            brushColor = Color.RED;
            bColor.setTextColor(Color.RED);
        } else {
            brush = blackBrush.copy(Bitmap.Config.ARGB_8888, true);
            brushColor = Color.BLACK;
            bColor.setTextColor(Color.BLACK);
        }
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
            textCanvas.drawBitmap(paper, 0f, 0f, paint);
        }
        ivCanvas.setImageBitmap(textBitmap);
        setCursor();
        return true;
    };

    private final View.OnClickListener onReturnButtonClickListener = view -> {
        if (isWriting) {
            next(true);
        } else {
            if (rbLtr.isChecked()) {
                cursorX = 0f;
                cursorY += charWidth + lineSpacing;
            } else {
                cursorY = 0f;
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
                    backspace((int) (charWidth / 4f));
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
                brushWidth = 0f;
                if (!isWriting) {
                    isWriting = true;
                    clearCanvas(blankCanvas);
                    blankCanvas.drawBitmap(displayBitmap, 0f, 0f, paint);
                    blankCanvas.drawColor(TRANSLUCENT);
                    blankCanvas.drawLine(0f, charTop, width, charTop, paint);
                    blankCanvas.drawLine(0f, charBottom, width, charBottom, paint);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (x < left) left = x;
                if (x > right) right = x;
                if (y < top) top = y;
                if (y > bottom) bottom = y;

                float d = (float) Math.sqrt(Math.pow(x - prevX, 2) + Math.pow(y - prevY, 2)),
                        a = d / (float) Math.pow(d, ratio),
                        w = 0f,
                        width = (float) Math.pow(1 - d / size, handwriting) * strokeWidth,
                        xpd = (x - prevX) / d, ypd = (y - prevY) / d;
                if (width >= brushWidth) {
                    for (float f = 0; f < d; f += alias) {
                        w = a * (float) Math.pow(f, ratio) / d * (width - brushWidth) + brushWidth;
                        Rect r = new Rect((int) (xpd * f + prevX - w), (int) (ypd * f + prevY - w), (int) (xpd * f + prevX + w), (int) (ypd * f + prevY + w));
                        blankCanvas.drawBitmap(brush, SQUARE_WITH_SIDE_LENGTH_192, r, paint);
                        canvas.drawBitmap(brush, SQUARE_WITH_SIDE_LENGTH_192, r, paint);
                    }
                } else {
                    for (float f = 0; f < d; f += alias) {
                        w = (float) Math.pow(f / a, 1 / ratio) / d * (width - brushWidth) + brushWidth;
                        Rect r = new Rect((int) (xpd * f + prevX - w), (int) (ypd * f + prevY - w), (int) (xpd * f + prevX + w), (int) (ypd * f + prevY + w));
                        blankCanvas.drawBitmap(brush, SQUARE_WITH_SIDE_LENGTH_192, r, paint);
                        canvas.drawBitmap(brush, SQUARE_WITH_SIDE_LENGTH_192, r, paint);
                    }
                }
                brushWidth = w;
                prevX = x;
                prevY = y;

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
                cursorX = (buttonLocation[0] + motionEvent.getX() - canvasLocation[0]) - charWidth / 8f;
                cursorY = (buttonLocation[1] + motionEvent.getY() - canvasLocation[1]) - charWidth / 2f;
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
                spaceX = motionEvent.getX();
                spaceY = motionEvent.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (rbLtr.isChecked()) {
                    float x = motionEvent.getX();
                    cursorX += x - spaceX;
                    spaceX = x;
                } else {
                    float y = motionEvent.getY();
                    cursorY += y - spaceY;
                    spaceY = y;
                }
                space = true;
                setCursor();
                break;
            case MotionEvent.ACTION_UP:
                if (space) {
                    space = false;
                } else {
                    if (rbLtr.isChecked()) {
                        cursorX += charWidth / 4f;
                    } else {
                        cursorY += charWidth / 4f;
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
        matrix.setRotate(-90f);
        blankBitmap = Bitmap.createBitmap(bitmap);
        blankCanvas = new Canvas(blankBitmap);
        charBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        charCanvas = new Canvas(charBitmap);
        textBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        textCanvas = new Canvas(textBitmap);
        displayBitmap = Bitmap.createBitmap(textBitmap);
        displayCanvas = new Canvas(displayBitmap);
        size = (float) Math.sqrt(Math.pow(width, 2) + Math.pow(height, 2));
        left = width;
        right = 0f;
        top = height;
        bottom = 0f;
        charTop = (height - width) / 2f;
        charBottom = charTop + width;
        setCursor();
    }

    private void next() {
        next(false);
    }

    private void next(boolean rotate) {
        isWriting = false;
        clearCanvas(charCanvas);

        float charLength;

        if (rbLtr.isChecked()) {

            if (!rotate) {
                charLength = toCharSize(right - left);
                charCanvas.drawBitmap(bitmap,
                        rect,
                        new RectF(0f, 0f, charWidth, toCharSize(height)),
                        paint);
            } else {
                charLength = toCharSize(bottom - top);
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
                charCanvas.drawBitmap(rotatedBitmap,
                        rotatedRect,
                        new RectF(0f, 0f, toCharSize(height), charWidth),
                        paint);
                rotatedBitmap.recycle();
            }

            cursorX += columnSpacing;
            if (autoNewline && cursorX + charLength > width) {
                cursorY += charWidth + lineSpacing;
                cursorX = 0f;
            }

            if (!rotate) {
                textCanvas.drawBitmap(charBitmap,
                        cursorX - toCharSize(left),
                        cursorY - toCharSize(charTop),
                        paint);
            } else {
                textCanvas.drawBitmap(charBitmap,
                        cursorX - toCharSize(top),
                        cursorY,
                        paint);
            }

            cursorX += this.charLength == -1f ? charLength : this.charLength;
            if (!autoNewline && cursorX > width) {
                cursorY += charWidth + lineSpacing;
                cursorX = 0f;
            }

        } else {

            if (!rotate) {
                charLength = toCharSize(bottom - top);
                charCanvas.drawBitmap(bitmap,
                        rect,
                        new RectF(0f, 0f, charWidth, toCharSize(height)),
                        paint);
            } else {
                charLength = toCharSize(right - left);
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
                charCanvas.drawBitmap(rotatedBitmap,
                        rotatedRect,
                        new RectF(0f, 0f, toCharSize(height), charWidth),
                        paint);
                rotatedBitmap.recycle();
            }

            cursorY += columnSpacing;
            if (autoNewline && cursorY + charLength > height) {
                cursorX -= charWidth + lineSpacing;
                cursorY = 0;
            }

            if (!rotate) {
                textCanvas.drawBitmap(charBitmap, cursorX, cursorY - toCharSize(top), paint);
            } else {
                textCanvas.drawBitmap(charBitmap, cursorX - toCharSize(top), cursorY, paint);
            }

            cursorY += this.charLength == -1f ? charLength : this.charLength;
            if (!autoNewline && cursorY > height) {
                cursorX -= charWidth + lineSpacing;
                cursorY = 0;
            }

        }
        clearCanvas(canvas);
        setCursor();
        left = ivCanvas.getWidth();
        right = 0f;
        top = ivCanvas.getHeight();
        bottom = 0f;
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
        ((RadioButton) findViewById(R.id.rb_char_length_auto)).setOnCheckedChangeListener(onCharLengthAutoCompButtCheckedChangeListener);
        ((RadioButton) findViewById(R.id.rb_char_length_custom)).setOnCheckedChangeListener(onCharLengthCustomCompButtCheckedChangeListener);
        ((SeekBar) findViewById(R.id.sb_alias)).setOnSeekBarChangeListener((OnProgressChangeListener) progress -> alias = progress);
        sbCharLength.setOnSeekBarChangeListener(onCharLengthSeekBarProgressChangeListener);
        ((SeekBar) findViewById(R.id.sb_char_width)).setOnSeekBarChangeListener(onCharWidthSeekBarProgressChangeListener);
        ((SeekBar) findViewById(R.id.sb_column_spacing)).setOnSeekBarChangeListener(onColSpacingSeekBarProgressChangeListener);
        ((SeekBar) findViewById(R.id.sb_handwriting)).setOnSeekBarChangeListener((OnProgressChangeListener) progress -> handwriting = progress);
        ((SeekBar) findViewById(R.id.sb_line_spacing)).setOnSeekBarChangeListener(onLineSpacingSeekBarProgressChangeListener);
        ((SeekBar) findViewById(R.id.sb_ratio)).setOnSeekBarChangeListener((OnProgressChangeListener) progress -> ratio = progress / 10f);
        ((SeekBar) findViewById(R.id.sb_stroke_width)).setOnSeekBarChangeListener((OnProgressChangeListener) progress -> strokeWidth = progress);
        ((SwitchCompat) findViewById(R.id.s_newline)).setOnCheckedChangeListener((compoundButton, b) -> autoNewline = b);

        Resources res = getResources();
        blackBrush = BitmapFactory.decodeResource(res, R.mipmap.brush);
        redBrush = BitmapFactory.decodeResource(res, R.mipmap.brush_red);
        brush = blackBrush.copy(Bitmap.Config.ARGB_8888, true);
    }

    @Override
    protected void onDestroy() {
        canvas = null;
        bitmap.recycle();
        bitmap = null;
        blackBrush.recycle();
        blackBrush = null;
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
        redBrush.recycle();
        redBrush = null;
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
        previewCanvas.drawBitmap(previewPaperBitmap, 0f, 0f, previewer);
        float charLength = (this.charLength == -1f ? charWidth : this.charLength);
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

    private void setBrushColor(int color) {
        if (color == brushColor) {
            return;
        }
        int brushWidth = blackBrush.getWidth(), brushHeight = blackBrush.getHeight();
        for (int y = 0; y < brushHeight; ++y) {
            for (int x = 0; x < brushWidth; ++x) {
                if (blackBrush.getPixel(x, y) != Color.TRANSPARENT) {
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
        displayCanvas.drawBitmap(textBitmap, 0f, 0f, paint);
        displayCanvas.drawRect(cursorX,
                style ? cursorY : cursorY + charWidth,
                cursorX + charWidth,
                cursorY + charWidth,
                cursor);
        ivCanvas.setImageBitmap(displayBitmap);
    }

    private float toCharSize(float size) {
        return charWidth / width * size;
    }
}