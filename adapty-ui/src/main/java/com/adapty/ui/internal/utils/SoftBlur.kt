package com.adapty.ui.internal.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal class SoftBlur {
    private var bmp: Bitmap? = null
    private var canvas: Canvas? = null
    private var px = ByteArray(0)
    private var tmp = ByteArray(0)
    private var buf: ByteBuffer = ByteBuffer.wrap(px)
    private var argb: Bitmap? = null
    private var argbColor = 0
    private var argbStale = true
    private val tint = Paint()
    private val scaled = Paint(Paint.FILTER_BITMAP_FLAG)
    private var lineA = IntArray(0)
    private var lineB = IntArray(0)
    private var scale = 1
    private var pad = 0

    fun render(w: Int, h: Int, sigma: Float, source: (Canvas) -> Unit) {
        val k = SoftBlurMath.scale(sigma)
        val s = SoftBlurMath.scaledSigma(sigma, k)
        val p = ceil(s * 3).toInt() + 1
        val bw = (w + k - 1) / k + 2 * p
        val bh = (h + k - 1) / k + 2 * p
        val old = bmp
        val b = if (old == null || old.width != bw || old.height != bh) {
            old?.recycle()
            Bitmap.createBitmap(bw, bh, Bitmap.Config.ALPHA_8).also {
                bmp = it
                canvas = Canvas(it)
            }
        } else {
            old.apply { eraseColor(0) }
        }
        scale = k
        pad = p
        argbStale = true
        val c = canvas ?: return
        c.save()
        c.translate(p.toFloat(), p.toFloat())
        c.scale(1f / k, 1f / k)
        c.clipRect(0, 0, w, h)
        source(c)
        c.restore()
        if (s > 0f) blur(b, s)
    }

    fun draw(canvas: Canvas, x: Float, y: Float, paint: Paint) {
        var b = bmp ?: return
        var p = paint
        if (scale > 1 && !canvas.isHardwareAccelerated) {
            val oldArgb = argb
            val c = if (oldArgb == null || oldArgb.width != b.width || oldArgb.height != b.height) {
                oldArgb?.recycle()
                argbStale = true
                Bitmap.createBitmap(b.width, b.height, Bitmap.Config.ARGB_8888).also { argb = it }
            } else {
                oldArgb
            }
            if (argbStale || argbColor != paint.color) {
                c.eraseColor(0)
                tint.color = paint.color
                Canvas(c).drawBitmap(b, 0f, 0f, tint)
                argbColor = paint.color
                argbStale = false
            }
            scaled.xfermode = paint.xfermode
            scaled.alpha = 255
            b = c
            p = scaled
        }
        canvas.save()
        canvas.translate(x - pad * scale, y - pad * scale)
        canvas.scale(scale.toFloat(), scale.toFloat())
        canvas.drawBitmap(b, 0f, 0f, p)
        canvas.restore()
    }

    fun release() {
        bmp?.recycle()
        bmp = null
        canvas = null
        argb?.recycle()
        argb = null
        px = ByteArray(0)
        tmp = ByteArray(0)
        buf = ByteBuffer.wrap(px)
    }

    private fun blur(b: Bitmap, sigma: Float) {
        val w = b.width
        val h = b.height
        val stride = b.rowBytes
        val n = stride * h
        if (px.size < n) {
            px = ByteArray(n)
            tmp = ByteArray(n)
            buf = ByteBuffer.wrap(px)
        }
        if (lineA.size < maxOf(w, h)) {
            lineA = IntArray(maxOf(w, h))
            lineB = IntArray(maxOf(w, h))
        }
        buf.rewind()
        b.copyPixelsToBuffer(buf)
        val r = SoftBlurMath.boxRadius(sigma)
        val edge = SoftBlurMath.boxEdge(sigma)
        val width = 2 * r + 1 + 2 * edge
        val mul = (65536f / width).roundToInt()
        val edgeMul = (65536f * edge / width).roundToInt()
        BoxBlur.lines(px, w, h, stride, tmp, h, r, mul, edgeMul, lineA, lineB)
        BoxBlur.lines(tmp, h, w, h, px, stride, r, mul, edgeMul, lineA, lineB)
        buf.rewind()
        b.copyPixelsFromBuffer(buf)
    }
}

internal object SoftBlurMath {

    fun sigmaOf(radius: Float): Float = if (radius > 0f) 0.57735f * radius + 0.5f else 0f

    fun maskFilterBlurs(hardware: Boolean, api: Int = Build.VERSION.SDK_INT): Boolean =
        if (hardware) api >= Build.VERSION_CODES.P else api >= Build.VERSION_CODES.Q

    fun scale(sigma: Float): Int = (sigma / 1.5f).toInt().coerceIn(1, 8)

    fun scaledSigma(sigma: Float, scale: Int): Float =
        sqrt(maxOf(sigma * sigma - scale * scale / 4f, 0f)) / scale

    fun boxRadius(sigma: Float): Int {
        val v = sigma * sigma / 3f
        var r = ((sqrt(1f + 12f * v) - 1f) / 2f).toInt()
        while ((r + 1) * (r + 2) / 3f <= v) r++
        while (r > 0 && r * (r + 1) / 3f > v) r--
        return r
    }

    fun boxEdge(sigma: Float): Float {
        val v = sigma * sigma / 3f
        val r = boxRadius(sigma)
        return (2 * r + 1) * (r * (r + 1) / 3f - v) / (2f * (v - (r + 1) * (r + 1)))
    }
}

internal object BoxBlur {

    fun lines(
        src: ByteArray, len: Int, count: Int, stride: Int, dst: ByteArray, dstStride: Int,
        r: Int, mul: Int, edgeMul: Int, a: IntArray, b: IntArray,
    ) {
        for (i in 0 until count) {
            val base = i * stride
            for (j in 0 until len) a[j] = src[base + j].toInt() and 0xff
            box(a, b, len, r, mul, edgeMul)
            box(b, a, len, r, mul, edgeMul)
            box(a, b, len, r, mul, edgeMul)
            var o = i
            for (j in 0 until len) {
                dst[o] = b[j].toByte()
                o += dstStride
            }
        }
    }

    private fun box(src: IntArray, dst: IntArray, len: Int, r: Int, mul: Int, edgeMul: Int) {
        var sum = 0
        for (j in 0..minOf(r, len - 1)) sum += src[j]
        for (j in 0 until len) {
            val lo = j - r - 1
            val hi = j + r + 1
            val edge = (if (lo >= 0) src[lo] else 0) + (if (hi < len) src[hi] else 0)
            dst[j] = minOf((sum * mul + edge * edgeMul + 0x8000) ushr 16, 255)
            if (hi < len) sum += src[hi]
            if (lo + 1 >= 0) sum -= src[lo + 1]
        }
    }
}
