@file:OptIn(InternalAdaptyApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.adapty.ui.internal.ui.element

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.FlowConfiguration.Asset
import com.adapty.ui.internal.ui.attributes.AspectRatio
import com.adapty.ui.internal.ui.attributes.LocalContentAlignment
import com.adapty.ui.internal.ui.fillWithBaseParams
import com.adapty.ui.internal.ui.resolveAssets
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.VisualValue
import com.adapty.ui.internal.utils.asMediaItem
import com.adapty.ui.internal.utils.createPlayer
import com.adapty.ui.internal.utils.getAsset
import com.adapty.ui.internal.utils.log
import com.adapty.ui.internal.utils.resolve
import com.adapty.ui.video.R
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE
import kotlin.math.roundToInt

public class VideoElement internal constructor(
    internal val assetId: VisualValue,
    internal val aspectRatio: AspectRatio,
    internal val loop: Boolean,
    internal val actions: List<Action>,
    override val baseProps: BaseProps,
) : UIElement, SizeDrivenElement {

    override fun ColumnScope.toComposableInColumn(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit {
        return toComposable(
            dispatch,
            baseProps.weight?.let { modifier.weight(it) } ?: modifier,
        )
    }

    override fun RowScope.toComposableInRow(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit {
        return toComposable(
            dispatch,
            baseProps.weight?.let { modifier.weight(it) } ?: modifier,
        )
    }

    @UnstableApi
    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = renderVideo@{
        val assetIdValue = assetId.source.resolve()
            ?: return@renderVideo
        val video = resolveAssets().getAsset<Asset.Video>(assetIdValue)?.main
            ?: return@renderVideo
        var firstFrameRendered by remember {
            mutableStateOf(false)
        }
        val context = LocalContext.current

        val manualLoop = loop && actions.isNotEmpty()
        val player = remember {
            createPlayer(context)?.apply {
                volume = 0f
                repeatMode = if (loop && !manualLoop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                addListener(object: Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)
                        log(ERROR) { "$LOG_PREFIX_ERROR playback error: (${error.errorCode} / ${error.errorCodeName} / ${error.message})" }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        super.onPlaybackStateChanged(playbackState)
                        log(VERBOSE) { "$LOG_PREFIX onPlaybackStateChanged: ${playbackState}" }
                    }

                    override fun onRenderedFirstFrame() {
                        firstFrameRendered = true
                        super.onRenderedFirstFrame()
                    }
                })
            }
        }
        remember(video.source) {
            when (val source = video.source) {
                is Asset.Video.Source.Uri -> source.uri
                is Asset.Video.Source.AndroidAsset -> android.net.Uri.parse("asset:///${source.path}")
            }.also { uri ->
                player?.setMediaItem(uri.asMediaItem())
                player?.prepare()
            }
        }

        LifecycleStartEffect(Unit) {
            player?.playWhenReady = true
            onStopOrDispose { player?.playWhenReady = false }
        }

        DisposableEffect(Unit) {
            onDispose {
                player?.release()
            }
        }

        if (actions.isNotEmpty() && player != null) {
            val screen = LocalScreenInstance.current
            DisposableEffect(player) {
                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            dispatch(Message.ActionsRequested(actions, screen))
                            if (manualLoop) {
                                player.seekTo(0)
                                player.play()
                            }
                        }
                    }
                }
                player.addListener(listener)
                onDispose {
                    player.removeListener(listener)
                }
            }
        }

        val ratio = video.ratio
        val containerModifier = if (ratio != null) {
            modifier.videoAspectLayout(
                ratio = ratio,
                cover = aspectRatio != AspectRatio.FIT,
                alignment = LocalContentAlignment.current,
                layoutDirection = LocalLayoutDirection.current,
            )
        } else {
            modifier.fillMaxWidth().fillMaxHeight()
        }

        Box(modifier = containerModifier, contentAlignment = Alignment.Center) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                factory = { context ->
                    createPlayerView(context)?.apply {
                        this.player = player
                        useController = false
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        when (aspectRatio) {
                            AspectRatio.FILL -> {
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                player?.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                            }
                            AspectRatio.STRETCH -> resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                            else -> resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    } ?: return@AndroidView View(context)
                },
            )

            if (!firstFrameRendered) {
                val preview = remember(assetIdValue) {
                    ImageElement(
                        "${assetIdValue}${VIDEO_PREVIEW_ASSET_SUFFIX}",
                        aspectRatio,
                        baseProps,
                    )
                }
                preview.toComposable(
                    dispatch,
                    Modifier.fillWithBaseParams(preview)
                ).invoke()
            }
        }
    }

    private fun createPlayerView(context: Context): PlayerView? {
        return runCatching { LayoutInflater.from(context).inflate(R.layout.adapty_player, null) as PlayerView }
            .getOrElse { e ->
                log(ERROR) { "$LOG_PREFIX_ERROR couldn't create player view: (${e.localizedMessage})" }
                null
            }
    }

    private companion object {
        const val VIDEO_PREVIEW_ASSET_SUFFIX = "\$\$preview"
    }
}

private fun Modifier.videoAspectLayout(
    ratio: Float,
    cover: Boolean,
    alignment: Alignment,
    layoutDirection: LayoutDirection,
): Modifier =
    this.layout { measurable, constraints ->
        val boundedWidth = constraints.hasBoundedWidth
        val boundedHeight = constraints.hasBoundedHeight
        val maxW = constraints.maxWidth
        val maxH = constraints.maxHeight

        val width: Int
        val height: Int
        when {
            boundedWidth && boundedHeight -> {
                val heightForWidth = maxW / ratio
                if (cover == (heightForWidth >= maxH)) {
                    width = maxW
                    height = heightForWidth.roundToInt()
                } else {
                    width = (maxH * ratio).roundToInt()
                    height = maxH
                }
            }
            boundedWidth -> { width = maxW; height = (maxW / ratio).roundToInt() }
            boundedHeight -> { width = (maxH * ratio).roundToInt(); height = maxH }
            else -> { width = 0; height = 0 }
        }

        val placeable = measurable.measure(
            Constraints.fixed(
                width.coerceIn(0, MAX_VIDEO_DIMENSION_PX),
                height.coerceIn(0, MAX_VIDEO_DIMENSION_PX),
            )
        )
        val outWidth = if (boundedWidth) maxW else placeable.width
        val outHeight = if (boundedHeight) maxH else placeable.height
        layout(outWidth, outHeight) {
            placeable.place(
                alignment.align(
                    IntSize(placeable.width, placeable.height),
                    IntSize(outWidth, outHeight),
                    layoutDirection,
                )
            )
        }
    }

private const val MAX_VIDEO_DIMENSION_PX = 32_766