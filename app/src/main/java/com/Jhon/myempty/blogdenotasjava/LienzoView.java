package com.Jhon.myempty.blogdenotasjava;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;

public class LienzoView extends View {
    private Bitmap mBitmap;
    private Canvas mCanvas;

    private Path mCurrentPath;
    private Paint mCurrentPaint;

    private ArrayList<DibujoObjeto> mObjetos = new ArrayList<>();
    private DibujoObjeto objetoSeleccionado = null;

    private ArrayList<DibujoObjeto> mUndoneObjetos = new ArrayList<>();

    private int mColorActual = Color.BLACK;
    private float mGrosorActual = 10f;
    private String herramientaActual = "PEN";

    private static final int HANDLE_RADIUS = 25;
    private int handleTocado = -1;
    private float lastTouchX;
    private float lastTouchY;
    private boolean mMostrarCuadricula = false;

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
        if (mBitmap == null) {
            mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            mCanvas.drawColor(Color.WHITE);
        }
    }

    // --- MÉTODOS REQUERIDOS POR DIBUJOACTIVITY (Añadidos/Adaptados) ---

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        // Escala el bitmap para que se ajuste al tamaño del lienzo y lo dibuja.
        if (mCanvas != null) {
            Bitmap escalado = Bitmap.createScaledBitmap(bitmap, getWidth(), getHeight(), false);
            mCanvas.drawBitmap(escalado, 0, 0, null);
            invalidate();
        }
    }

    public void modoBorrador() {
        activarBorrador();
    }

    public void modoLapiz() {
        activarPluma();
    }

    public void deshacer() {
        if (!mObjetos.isEmpty()) {
            mUndoneObjetos.add(mObjetos.remove(mObjetos.size() - 1));
            objetoSeleccionado = null;
            redrawAllPaths();
        }
    }

    public void rehacer() {
        if (!mUndoneObjetos.isEmpty()) {
            mObjetos.add(mUndoneObjetos.remove(mUndoneObjetos.size() - 1));
            redrawAllPaths();
        }
    }

    // --- Lógica interna (existente) ---

    public void setColor(int nuevoColor) {
        this.mColorActual = nuevoColor;
    }

    public void setGrosor(float nuevoGrosor) {
        this.mGrosorActual = nuevoGrosor;
    }

    public float getGrosorActual() {
        return mGrosorActual;
    }

    public void nuevoDibujo() {
        mObjetos.clear();
        mUndoneObjetos.clear();
        objetoSeleccionado = null;
        if (mCanvas != null) {
            mCanvas.drawColor(Color.WHITE);
        }
        invalidate();
    }

    public void cargarFondo(Bitmap bitmap) {
        this.post(() -> {
            if (mBitmap != null && mCanvas != null) {
                Bitmap escalado = Bitmap.createScaledBitmap(bitmap, getWidth(), getHeight(), true);
                mCanvas.drawBitmap(escalado, 0, 0, null);
                invalidate();
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBitmap != null) canvas.drawBitmap(mBitmap, 0, 0, null);

        if (mMostrarCuadricula) {
            Paint pGrid = new Paint();
            pGrid.setColor(Color.LTGRAY);
            pGrid.setStrokeWidth(2f);
            pGrid.setAlpha(100);
            int paso = 100;
            for (int i = 0; i < getWidth(); i += paso) canvas.drawLine(i, 0, i, getHeight(), pGrid);
            for (int j = 0; j < getHeight(); j += paso) canvas.drawLine(0, j, getWidth(), j, pGrid);
        }
        if (mCurrentPath != null && mCurrentPaint != null) canvas.drawPath(mCurrentPath, mCurrentPaint);

        if (objetoSeleccionado != null) {
            objetoSeleccionado.actualizarBounds();
            RectF r = objetoSeleccionado.bounds;
            Paint pCuadro = new Paint();
            pCuadro.setColor(Color.parseColor("#4285F4"));
            pCuadro.setStyle(Paint.Style.STROKE);
            pCuadro.setStrokeWidth(4f);
            canvas.drawRect(r, pCuadro);
            
            pCuadro.setStyle(Paint.Style.FILL);
            canvas.drawCircle(r.left, r.top, HANDLE_RADIUS, pCuadro);
            canvas.drawCircle(r.right, r.top, HANDLE_RADIUS, pCuadro);
            canvas.drawCircle(r.left, r.bottom, HANDLE_RADIUS, pCuadro);
            canvas.drawCircle(r.right, r.bottom, HANDLE_RADIUS, pCuadro);

            pCuadro.setStyle(Paint.Style.STROKE);
            canvas.drawLine(r.centerX(), r.top, r.centerX(), r.top - 60, pCuadro);
            pCuadro.setStyle(Paint.Style.FILL);
            canvas.drawCircle(r.centerX(), r.top - 60, HANDLE_RADIUS, pCuadro);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (herramientaActual.equals("SELECTION")) {
            handleSelectionMode(event, x, y);
        } else {
            handleDrawingMode(event, x, y);
        }

        invalidate();
        return true;
    }

    private void handleDrawingMode(MotionEvent event, float x, float y) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mCurrentPath = new Path();
                mCurrentPaint = new Paint(setupCurrentPaint());
                mCurrentPath.moveTo(x, y);
                mUndoneObjetos.clear();
                break;
            case MotionEvent.ACTION_MOVE:
                mCurrentPath.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                DibujoObjeto nuevoObj = new DibujoObjeto(mCurrentPath, mCurrentPaint);
                mObjetos.add(nuevoObj);
                mCanvas.drawPath(nuevoObj.pathTransformado, nuevoObj.paint);
                mCurrentPath = null;
                break;
        }
    }

    private void handleSelectionMode(MotionEvent event, float x, float y) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                handleTocado = detectarHandle(x, y);
                if (handleTocado == -1) {
                    detectarSeleccion(x, y);
                    if (objetoSeleccionado != null) handleTocado = 9;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (objetoSeleccionado != null && handleTocado != -1) {
                    aplicarTransformacion(x, y);
                    redrawAllPaths(); 
                }
                break;
            case MotionEvent.ACTION_UP:
                handleTocado = -1;
                break;
        }
    }

    private int detectarHandle(float x, float y) {
        if (objetoSeleccionado == null) return -1;
        RectF r = objetoSeleccionado.bounds;
        float tol = 50;
        if (dist(x, y, r.centerX(), r.top - 60) < tol) return 8;
        if (dist(x, y, r.left, r.top) < tol) return 0;
        if (dist(x, y, r.right, r.top) < tol) return 1;
        if (dist(x, y, r.left, r.bottom) < tol) return 2;
        if (dist(x, y, r.right, r.bottom) < tol) return 3;
        if (r.contains(x, y)) return 9;
        return -1;
    }

    private void detectarSeleccion(float x, float y) {
        objetoSeleccionado = null;
        for (int i = mObjetos.size() - 1; i >= 0; i--) {
            DibujoObjeto obj = mObjetos.get(i);
            if (obj.bounds.contains(x, y)) {
                objetoSeleccionado = obj;
                break;
            }
        }
    }

    private void aplicarTransformacion(float x, float y) {
        float dx = x - lastTouchX;
        float dy = y - lastTouchY;
        RectF r = objetoSeleccionado.bounds;
        float cx = r.centerX();
        float cy = r.centerY();

        if (handleTocado == 9) {
            objetoSeleccionado.matrix.postTranslate(dx, dy);
        } else if (handleTocado == 8) {
            float anguloActual = (float) Math.toDegrees(Math.atan2(y - cy, x - cx));
            float anguloAnt = (float) Math.toDegrees(Math.atan2(lastTouchY - cy, lastTouchX - cx));
            objetoSeleccionado.matrix.postRotate(anguloActual - anguloAnt, cx, cy);
        } else if (handleTocado >= 0 && handleTocado <= 3) {
            float s = dist(x, y, cx, cy) / dist(lastTouchX, lastTouchY, cx, cy);
            if (dist(x, y, cx, cy) > 30) {
                objetoSeleccionado.matrix.postScale(s, s, cx, cy);
            }
        }
        lastTouchX = x;
        lastTouchY = y;
    }

    private void redrawAllPaths() {
        if (mBitmap != null && mCanvas != null) {
            mBitmap.eraseColor(Color.WHITE);
            for (DibujoObjeto obj : mObjetos) {
                obj.actualizarBounds();
                mCanvas.drawPath(obj.pathTransformado, obj.paint);
            }
        }
        invalidate();
    }

    private Paint setupCurrentPaint() {
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(mColorActual);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(mGrosorActual);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);

        if (herramientaActual.equals("RESALTADOR")) {
            p.setAlpha(80);
            p.setStrokeCap(Paint.Cap.SQUARE);
        } else if (herramientaActual.equals("ERASER")) {
            p.setColor(Color.WHITE);
        }
        return p;
    }

    public void setModo(String modo) {
        if (this.herramientaActual.equals("SELECTION") && !modo.equals("SELECTION")) {
            objetoSeleccionado = null;
            redrawAllPaths(); 
        }
        this.herramientaActual = modo;
        switch (modo) {
            case "PEN": mGrosorActual = 10f; break;
            case "MARKER": mGrosorActual = 25f; break;
            case "RESALTADOR": mGrosorActual = 50f; break;
            case "ERASER": mGrosorActual = 60f; break;
        }
    }

    private float dist(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    private class DibujoObjeto {
        Path pathOriginal, pathTransformado;
        Paint paint;
        Matrix matrix = new Matrix();
        RectF bounds = new RectF();

        DibujoObjeto(Path p, Paint pt) {
            this.pathOriginal = new Path(p);
            this.pathTransformado = new Path(p);
            this.paint = new Paint(pt);
            actualizarBounds();
        }

        void actualizarBounds() {
            pathTransformado.set(pathOriginal);
            pathTransformado.transform(matrix);
            pathTransformado.computeBounds(bounds, true);
        }
    }
    public boolean toggleCuadricula() {
        mMostrarCuadricula = !mMostrarCuadricula;
        invalidate();
        return mMostrarCuadricula;
    }
    
    public void activarPluma() {
        setModo("PEN");
        if (mColorActual == Color.WHITE) mColorActual = Color.BLACK; 
    }

    public void activarMarcador() {
        setModo("MARKER");
    }

    public void activarResaltador() {
        setModo("RESALTADOR");
    }

    public void activarBorrador() {
        setModo("ERASER");
    }

    public void activarSeleccion() {
        setModo("SELECTION");
    }

    public void setNuevoColor(int color) {
        this.mColorActual = color;
        if (mCurrentPaint != null) {
            mCurrentPaint.setColor(color);
        }
        if (objetoSeleccionado != null) {
            objetoSeleccionado.paint.setColor(color);
            invalidate();
        }
    }

    public void deseleccionarTodo() {
        objetoSeleccionado = null;
        invalidate();
    }
}
