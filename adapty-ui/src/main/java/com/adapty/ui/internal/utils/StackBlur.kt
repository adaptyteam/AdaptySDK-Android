package com.adapty.ui.internal.utils

import android.graphics.Bitmap

internal class StackBlurWorker(private var maxW: Int, private var maxH: Int) {

    private val maxWh = maxW * maxH
    private var pixels = IntArray(maxWh)
    private var r = IntArray(maxWh)
    private var g = IntArray(maxWh)
    private var b = IntArray(maxWh)
    private var a = IntArray(maxWh)
    private var vmin = IntArray(maxOf(maxW, maxH))

    fun fits(w: Int, h: Int) = w <= maxW && h <= maxH && w * h <= maxWh

    fun blur(bitmap: Bitmap, radius: Int) {
        if (radius < 1) return

        val w = bitmap.width
        val h = bitmap.height
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h

        for (i in 0 until wh) {
            val p = pixels[i]
            val alpha = (p ushr 24) and 0xff
            if (alpha == 0) {
                pixels[i] = 0
            } else if (alpha < 255) {
                val rCh = (((p shr 16) and 0xff) * alpha) / 255
                val gCh = (((p shr 8) and 0xff) * alpha) / 255
                val bCh = ((p and 0xff) * alpha) / 255
                pixels[i] = (alpha shl 24) or (rCh shl 16) or (gCh shl 8) or bCh
            }
        }

        val div = radius + radius + 1

        var rsum: Int; var gsum: Int; var bsum: Int; var asum: Int
        var routsum: Int; var goutsum: Int; var boutsum: Int; var aoutsum: Int
        var rinsum: Int; var ginsum: Int; var binsum: Int; var ainsum: Int

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (i in dv.indices) {
            dv[i] = i / divsum
        }

        var yi = 0
        var yw = 0

        val stack = Array(div) { IntArray(4) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1

        for (y in 0 until h) {
            asum = 0; rsum = 0; gsum = 0; bsum = 0
            aoutsum = 0; routsum = 0; goutsum = 0; boutsum = 0
            ainsum = 0; rinsum = 0; ginsum = 0; binsum = 0
            for (i in -radius..radius) {
                val p = pixels[yi + minOf(wm, maxOf(i, 0))]
                sir = stack[i + radius]
                sir[0] = (p shr 16) and 0xff
                sir[1] = (p shr 8) and 0xff
                sir[2] = p and 0xff
                sir[3] = (p ushr 24) and 0xff
                rbs = r1 - kotlin.math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                asum += sir[3] * rbs
                if (i > 0) {
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]; ainsum += sir[3]
                } else {
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]; aoutsum += sir[3]
                }
            }
            stackpointer = radius

            for (x in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                a[yi] = dv[asum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                asum -= aoutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                aoutsum -= sir[3]

                if (y == 0) {
                    vmin[x] = minOf(x + radius + 1, wm)
                }
                val p = pixels[yw + vmin[x]]
                sir[0] = (p shr 16) and 0xff
                sir[1] = (p shr 8) and 0xff
                sir[2] = p and 0xff
                sir[3] = (p ushr 24) and 0xff

                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]; ainsum += sir[3]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                asum += ainsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]; aoutsum += sir[3]
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]; ainsum -= sir[3]

                yi++
            }
            yw += w
        }

        for (x in 0 until w) {
            asum = 0; rsum = 0; gsum = 0; bsum = 0
            aoutsum = 0; routsum = 0; goutsum = 0; boutsum = 0
            ainsum = 0; rinsum = 0; ginsum = 0; binsum = 0

            var yp = -radius * w
            for (i in -radius..radius) {
                yi = maxOf(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]; sir[1] = g[yi]; sir[2] = b[yi]; sir[3] = a[yi]
                rbs = r1 - kotlin.math.abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                asum += a[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]; ainsum += sir[3]
                } else {
                    routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]; aoutsum += sir[3]
                }
                if (i < hm) yp += w
            }
            yi = x
            stackpointer = radius
            for (y in 0 until h) {
                pixels[yi] = (dv[asum] shl 24) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                asum -= aoutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2]; aoutsum -= sir[3]

                if (x == 0) {
                    vmin[y] = minOf(y + r1, hm) * w
                }
                val p = x + vmin[y]
                sir[0] = r[p]; sir[1] = g[p]; sir[2] = b[p]; sir[3] = a[p]

                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]; ainsum += sir[3]
                rsum += rinsum; gsum += ginsum; bsum += binsum; asum += ainsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]; aoutsum += sir[3]
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2]; ainsum -= sir[3]

                yi += w
            }
        }

        for (i in 0 until wh) {
            val p = pixels[i]
            val alpha = (p ushr 24) and 0xff
            if (alpha == 0) {
                pixels[i] = 0
            } else if (alpha < 255) {
                val rCh = minOf(255, (((p shr 16) and 0xff) * 255) / alpha)
                val gCh = minOf(255, (((p shr 8) and 0xff) * 255) / alpha)
                val bCh = minOf(255, ((p and 0xff) * 255) / alpha)
                pixels[i] = (alpha shl 24) or (rCh shl 16) or (gCh shl 8) or bCh
            }
        }

        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }
}
