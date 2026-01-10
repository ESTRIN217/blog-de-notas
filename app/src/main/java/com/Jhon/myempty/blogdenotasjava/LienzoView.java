package com.Jhon.myempty.blogdenotasjava;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class LienzoView extends View {
    private Bitmap mBitmap; // Bitmap donde se dibuja de forma persistente
    private Canvas mCanvas; // Canvas asociado al mBitmap

    private Path mCurrentPath; // El trazo actual que el usuario está haciendo
    private Paint mCurrentPaint; // El pincel para el trazo actual

    private ArrayList<Path> mPaths = new ArrayList<>(); // Lista de todos los trazos completados
    private ArrayList<Paint> mPaints = new ArrayList<>(); // Lista de pinceles correspondientes a mPaths

    private ArrayList<Path> mUndonePaths = new ArrayList<>(); // Trazos deshechos
    private ArrayList<Paint> mUndonePaints = new ArrayList<>(); // Pinceles de trazos deshechos

    private int mColorActual = Color.BLACK;
    private float mGrosorActual = 10f;

    public LienzoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupPaint();
    }

    private void setupPaint() {
        mCurrentPaint = new Paint();
        mCurrentPaint.setAntiAlias(true);
        mCurrentPaint.setDither(true);
        mCurrentPaint.setColor(mColorActual);
        mCurrentPaint.setStyle(Paint.Style.STROKE);
        mCurrentPaint.setStrokeJoin(Paint.Join.ROUND);
        mCurrentPaint.setStrokeCap(Paint.Cap.ROUND);
        mCurrentPaint.setStrokeWidth(mGrosorActual);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mBitmap == null) { // Solo crear el bitmap la primera vez
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            mCanvas.drawColor(Color.WHITE); // Fondo inicial blanco
        } else {
            // Si el tamaño cambia y ya hay un bitmap, escalarlo o recrearlo
            Bitmap newBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas newCanvas = new Canvas(newBitmap);
            newCanvas.drawColor(Color.WHITE);
            newCanvas.drawBitmap(mBitmap, 0, 0, null); // Dibuja el viejo bitmap en el nuevo
            mBitmap = newBitmap;
            mCanvas = newCanvas;
            redrawAllPaths(); // Redibujar todo si el bitmap se recrea
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, 0, 0, null); // Dibuja el bitmap persistente

        // Dibuja el trazo actual que el usuario está haciendo (si existe)
        if (mCurrentPath != null && mCurrentPaint != null) {
            canvas.drawPath(mCurrentPath, mCurrentPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mCurrentPath = new Path();
                mCurrentPaint = new Paint(setupCurrentPaint()); // Crear un nuevo Paint con la configuración actual
                mCurrentPath.moveTo(x, y);
                // Si se empieza un nuevo trazo, se borra el historial de "deshechos"
                mUndonePaths.clear();
                mUndonePaints.clear();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mCurrentPath != null) {
                    mCurrentPath.lineTo(x, y);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mCurrentPath != null && mCurrentPaint != null) {
                    mPaths.add(mCurrentPath);
                    mPaints.add(mCurrentPaint);
                    mCanvas.drawPath(mCurrentPath, mCurrentPaint); // Dibuja el trazo en el bitmap persistente
                    mCurrentPath = null; // Reiniciar el trazo actual
                    mCurrentPaint = null;
                }
                break;
            default:
                return false;
        }
        invalidate(); // Solicita un redibujo de la vista
        return true;
    }

    // Configura un nuevo Paint con los valores actuales (color y grosor)
    private Paint setupCurrentPaint() {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(mColorActual);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(mGrosorActual);
        return paint;
    }

    public Bitmap getDibujo() {
        return mBitmap;
    }

    public void deshacer() {
        if (mPaths.size() > 0) {
            mUndonePaths.add(mPaths.remove(mPaths.size() - 1));
            mUndonePaints.add(mPaints.remove(mPaints.size() - 1));
            redrawAllPaths();
        }
    }

    public void rehacer() {
        if (mUndonePaths.size() > 0) {
            mPaths.add(mUndonePaths.remove(mUndonePaths.size() - 1));
            mPaints.add(mUndonePaints.remove(mUndonePaints.size() - 1));
            redrawAllPaths();
        }
    }

    public void nuevoDibujo() {
        mPaths.clear();
        mPaints.clear();
        mUndonePaths.clear();
        mUndonePaints.clear();
        if (mCanvas != null) {
            mCanvas.drawColor(Color.WHITE); // Limpiar el bitmap a blanco
        }
        invalidate();
    }

    public void setColor(int nuevoColor) {
        this.mColorActual = nuevoColor;
        // No es necesario recrear mCurrentPaint aquí, se hará en ACTION_DOWN
    }

    public void setGrosor(float nuevoGrosor) {
        this.mGrosorActual = nuevoGrosor;
        // No es necesario recrear mCurrentPaint aquí, se hará en ACTION_DOWN
    }

    // Método para redibujar todos los trazos en el bitmap persistente
    private void redrawAllPaths() {
        if (mBitmap != null && mCanvas != null) {
            mBitmap.eraseColor(Color.WHITE); // Limpiar completamente el bitmap
            for (int i = 0; i < mPaths.size(); i++) {
                mCanvas.drawPath(mPaths.get(i), mPaints.get(i));
            }
        }
        invalidate(); // Solicita redibujar la vista con el nuevo bitmap
    }
    // En LienzoView.java
public float getGrosorActual() {
    return mGrosorActual; // Esta es la variable float que definimos antes
}
    // En LienzoView.java
public void setModo(String modo) {
    switch (modo) {
        case "PEN":
            mColorActual = Color.BLACK;
            mGrosorActual = 10f;
            break;
        case "MARKER":
            mColorActual = Color.parseColor("#40FFFF00"); // Amarillo transparente
            mGrosorActual = 40f;
            break;
        case "ERASER":
            mColorActual = Color.WHITE;
            mGrosorActual = 60f;
            break;
    }
    }

}