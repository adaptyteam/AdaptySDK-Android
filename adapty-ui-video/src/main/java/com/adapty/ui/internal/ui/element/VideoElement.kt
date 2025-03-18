@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.ui.element

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.AdaptyUI.LocalizedViewConfiguration.Asset
import com.adapty.ui.internal.ui.attributes.AspectRatio
import com.adapty.ui.internal.ui.fillWithBaseParams
import com.adapty.ui.internal.utils.EventCallback
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.LOG_PREFIX_ERROR
import com.adapty.ui.internal.utils.asMediaItem
import com.adapty.ui.internal.utils.createPlayer
import com.adapty.ui.internal.utils.getForCurrentSystemTheme
import com.adapty.ui.internal.utils.log
import com.adapty.ui.video.R
import com.adapty.utils.AdaptyLogLevel.Companion.ERROR
import com.adapty.utils.AdaptyLogLevel.Companion.VERBOSE

public class VideoElement internal constructor(
    internal val assetId: String,
    internal val aspectRatio: AspectRatio,
    internal val loop: Boolean,
    override val baseProps: BaseProps,
    internal val preview: ImageElement,
) : UIElement {

    @UnstableApi
    override fun toComposable(
        resolveAssets: ResolveAssets,
        resolveText: ResolveText,
        resolveState: ResolveState,
        eventCallback: EventCallback,
        modifier: Modifier,
    ): @Composable () -> Unit = renderVideo@{
        val video = resolveAssets().getForCurrentSystemTheme(assetId) as? Asset.Video
            ?: return@renderVideo
        var firstFrameRendered by remember {
            mutableStateOf(false)
        }
        val context = LocalContext.current

        val player = remember {
            createPlayer(context)?.apply {
                volume = 0f
                repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
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
        remember(video.url) {
            Uri.parse(video.url).also { uri ->
                player?.setMediaItem(uri.asMediaItem())
                player?.prepare()
            }
        }

        val lifecycleObserver = remember {
            object: DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    player?.playWhenReady = false
                    super.onStop(owner)
                }

                override fun onStart(owner: LifecycleOwner) {
                    player?.playWhenReady = true
                    super.onStart(owner)
                }
            }.also { lifecycleObserver ->
                ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
                player?.release()
            }
        }

        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            factory = { context ->
                createPlayerView(context)?.apply {
                    this.player = player
                    useController = false
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
            preview.toComposable(
                resolveAssets,
                resolveText,
                resolveState,
                eventCallback,
                Modifier.fillWithBaseParams(preview, resolveAssets)
            ).invoke()
        }
    }

    private fun createPlayerView(context: Context): PlayerView? {
        return runCatching { LayoutInflater.from(context).inflate(R.layout.adapty_player, null) as PlayerView }
            .getOrElse { e ->
                log(ERROR) { "$LOG_PREFIX_ERROR couldn't create player view: (${e.localizedMessage})" }
                null
            }
    }
}