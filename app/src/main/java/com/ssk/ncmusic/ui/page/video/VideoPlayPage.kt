package com.ssk.ncmusic.ui.page.video

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.accompanist.insets.statusBarsPadding
import com.ssk.ncmusic.R
import com.ssk.ncmusic.core.AppConfig.APP_DESIGN_WIDTH
import com.ssk.ncmusic.model.VideoBean
import com.ssk.ncmusic.ui.common.CommonIcon
import com.ssk.ncmusic.ui.common.CommonNetworkImage
import com.ssk.ncmusic.ui.common.CommonTopAppBar
import com.ssk.ncmusic.ui.theme.AppColorsProvider
import com.ssk.ncmusic.utils.*
import com.ssk.ncmusic.viewmodel.video.VideoPlayViewModel
import kotlinx.coroutines.launch

private val cpnBottomSendCommentHeight = 100.cdp

/**
 * Created by ssk on 2022/5/15.
 */
@Composable
fun PlayVideoPage(videoBean: VideoBean, videoGroupId: Int, videoOffsetIndex: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VideoList(videoBean, videoGroupId, videoOffsetIndex)

        CommonTopAppBar(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            backgroundColor = Color.Transparent,
            contentColor = Color.White
        )
    }
}

@Composable
private fun VideoList(videoBean: VideoBean, videoGroupId: Int, videoOffsetIndex: Int) {
    val viewModel: VideoPlayViewModel = hiltViewModel()
    if (viewModel.videoFlows == null) {
        viewModel.buildVideoPager(videoGroupId, videoOffsetIndex)
    }
    val videoGroupItems = viewModel.videoFlows?.collectAsLazyPagingItems()
    val lazyListState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        state = lazyListState
    ) {
        item {
            CpnVideo(0, lazyListState, videoBean)
        }

        videoGroupItems?.let { items ->
            items(items.itemCount) { index ->
                items[index]?.data?.let { videoBean ->
                    CpnVideo(index + 1, lazyListState, videoBean)
                }
            }
        }
    }
}

@Composable
private fun CpnVideo(index: Int, lazyListState: LazyListState, videoBean: VideoBean) {
    val scope = rememberCoroutineScope()
    // 预加载
    val itemHeight = ScreenUtil.getScreenHeight().transformDp - 1.cdp
    val maxVideoHeight = ScreenUtil.getScreenHeight().transformDp - cpnBottomSendCommentHeight
    val videoWidth = videoBean.width
    val videoHeight = videoBean.height
//        if(videoWidth >= videoHeight) {  //横屏
//            cpnWidth = APP_DESIGN_WIDTH.cdp
//            cpnHeight = ((APP_DESIGN_WIDTH / videoWidth) * videoHeight).cdp
//        }else {  // 竖屏
//            cpnHeight = ScreenUtil.getScreenHeight().transformDp
//            cpnWidth =
//        }
    val cpnWidth = APP_DESIGN_WIDTH.cdp
    val cpnHeight = ((APP_DESIGN_WIDTH.toFloat() / videoWidth) * videoHeight).cdp
    var totalDragAmount = remember { 0f }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        totalDragAmount = 0f
                    },
                    onDragEnd = {
                        var newIndex = index
                        if (totalDragAmount < 0) {  // 向上滑动
                            if (totalDragAmount < -ScreenUtil.getScreenHeight() / 6) {
                                newIndex = index + 1
                            }
                        } else {  //向下滑动
                            if (totalDragAmount > ScreenUtil.getScreenHeight() / 6) {
                                newIndex = index - 1
                            }
                        }

                        scope.launch {
                            lazyListState.animateScrollToItem(newIndex)
                        }
                    }
                ) { _, dragAmount ->
                    // dragAmount 向上滑动为负
                    totalDragAmount += dragAmount
                    lazyListState.dispatchRawDelta(-dragAmount)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxVideoHeight),
            contentAlignment = Alignment.Center
        ) {
            // TODO
            CommonNetworkImage(
                url = videoBean.coverUrl,
                modifier = Modifier
                    .width(cpnWidth)
                    .height(cpnHeight),
                placeholder = -1,
                error = -1
            )
        }

        CpnVideoInfo(videoBean)
        CpnBottomSendComment()
    }
}

@Composable
private fun BoxScope.CpnVideoInfo(videoBean: VideoBean) {
    Row(
        modifier = Modifier
            .padding(bottom = cpnBottomSendCommentHeight)
            .padding(32.cdp)
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CommonNetworkImage(
                    url = videoBean.creator.avatarUrl,
                    placeholder = R.drawable.ic_default_avator,
                    error = R.drawable.ic_default_avator,
                    modifier = Modifier
                        .size(55.cdp)
                        .clip(
                            RoundedCornerShape(50)
                        )
                )
                Text(
                    text = videoBean.creator.nickname,
                    fontSize = 32.csp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(start = 16.cdp)
                        .weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = videoBean.title,
                maxLines = 4,
                modifier = Modifier.padding(top = 24.cdp),
                fontSize = 28.csp,
                color = Color.White,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CpnVideoActionButton(R.drawable.ic_video_fabulous, StringUtil.friendlyNumber(videoBean.praisedCount))
            CpnVideoActionButton(R.drawable.ic_video_comment, StringUtil.friendlyNumber(videoBean.commentCount))
            CpnVideoActionButton(R.drawable.ic_video_share, StringUtil.friendlyNumber(videoBean.shareCount))
            CpnVideoActionButton(R.drawable.ic_video_collect, "收藏")
        }
    }
}

@Composable
private fun CpnVideoActionButton(iconResId: Int, text: String) {
    Column(
        modifier = Modifier.padding(bottom = 54.cdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CommonIcon(
            resId = iconResId,
            tint = Color.White,
            modifier = Modifier
                .padding(bottom = 12.cdp)
                .size(48.cdp)
        )

        Text(
            text = text,
            color = Color.White,
            fontSize = 28.csp
        )
    }
}

@Composable
private fun BoxScope.CpnBottomSendComment() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cpnBottomSendCommentHeight)
            .align(Alignment.BottomCenter)
    ) {
        Text(
            text = "千言万语,汇成评论一句话", fontSize = 28.csp, color = AppColorsProvider.current.thirdText,
            modifier = Modifier
                .padding(horizontal = 32.cdp)
                .align(Alignment.CenterStart)
        )
    }
}