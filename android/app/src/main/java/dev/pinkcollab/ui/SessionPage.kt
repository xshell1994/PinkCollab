package dev.pinkcollab.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.PlatformTextStyle
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import dev.pinkcollab.data.*
import dev.pinkcollab.ui.theme.*

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun SessionPage(
    state: SessionPageState,
    onAction: (SessionAction) -> Unit,
    onApplyModelSettings: (ModelSettingsChanges) -> Boolean,
    isActive: Boolean = true,
    composerHidden: Boolean = false,
    messageFontSize: Float = 12f,
    codeFontSize: Float = 12f,
    compactDensity: Boolean = false,
    composerMaxLines: Int = 5,
    immersiveButtonLeft: Boolean = false,
    onShowComposer: () -> Unit = {},
    loadToolDetails: suspend (String, String?) -> ToolDetailPage = { _, _ -> error("Tool details unavailable") },
) {
    val load = state.detail
    val host = state.host.takeIf { load is LoadState.Ready && load.value.snapshotToken != null }
    val draft = state.draft
    val selectingFiles = state.selectingFiles
    val activity = state.activity
    val modelState = state.model
    val onPrompt: () -> Unit = { onAction(SessionAction.Send) }
    val onDraftTextChange: (String) -> Unit = { onAction(SessionAction.DraftChanged(it)) }
    val onFileSelected: (Uri) -> Unit = { onAction(SessionAction.FileSelected(it)) }
    val onFileRemoved: (String) -> Unit = { onAction(SessionAction.FileRemoved(it)) }
    val onCommand: (SessionUserCommand) -> Unit = { onAction(SessionAction.Command(it)) }
    val onRespond: (AttentionResponse) -> Unit = { onAction(SessionAction.Respond(it)) }
    val onLoadModels: (Boolean) -> Unit = { onAction(SessionAction.LoadModels(it)) }
    // Keep the composer mounted while the selected session's details arrive.
    // The summary supplies identity only; it does not enable runtime actions.
    val detail = (load as? LoadState.Ready)?.value ?: SessionDetail(
        session = requireNotNull(state.summary),
        savedHistory = SavedHistory.Loading,
    )
    val context = LocalContext.current
    val renderer = remember(context, detail.session.hostId, detail.session.id, codeFontSize) {
        SessionMarkdownRenderer(createSessionMarkwon(context, codeFontSize))
    }
    val session = detail.session
    val currentDetailLoader by rememberUpdatedState(loadToolDetails)
    val toolDetails = remember(session.hostId, session.id, session.generation) {
        ToolDetailsController { callId, cursor -> currentDetailLoader(callId, cursor) }
    }
    val prompt = draft.text
    val selectedFiles = draft.files
    var fileError by remember(session.id) { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        fileError = null
        var count = selectedFiles.size
        val seen = selectedFiles.map { it.uri }.toMutableSet()
        for (uri in uris) {
            if (!seen.add(uri.toString())) continue
            if (count >= 5) {
                fileError = "Up to 5 files per message"
                break
            }
            count++
            onFileSelected(uri)
        }
    }
    var showModels by rememberSaveable(session.id) { mutableStateOf(false) }
    var showExitConfirmation by rememberSaveable(session.id) { mutableStateOf(false) }
    val controls = sessionControls(detail, host, draft, selectingFiles, activity)
    val attached = controls.attached
    val savedHistory = detail.savedHistory
    val historyItems = state.historyItems ?: savedHistory.items
    val liveItems = if (session.runtimeAttached) detail.liveItems else emptyList()
    val conversation = remember(historyItems, liveItems) { conversationTimeline(historyItems, liveItems) }
    val conversationDisplay = remember(conversation) { projectSessionTimeline(conversation) }
    val liveOperationIds = remember(liveItems) { liveItems.filter { it.tool?.completed == false }.map { it.id }.toSet() }
    val savedTodo = remember(historyItems) { projectSessionTodo(historyItems) }
    val liveTodo = remember(liveItems) { projectSessionTodo(liveItems) }
    val todo = latestSessionTodo(savedTodo, liveTodo)
    var todoExpanded by rememberSaveable(session.hostId, session.id) { mutableStateOf(false) }
    LaunchedEffect(todo == null) { if (todo == null) todoExpanded = false }
    BackHandler(todoExpanded && isActive) { todoExpanded = false }
    val historyError = sessionHistoryError(detail, state.host, state.refreshError)
    val awaitingHistory = historyError == null &&
        (savedHistory == SavedHistory.Loading || savedHistory == SavedHistory.Failed) && conversationDisplay.isEmpty() &&
        detail.streaming.isBlank() && session.attention == null
    val contentReady = load is LoadState.Ready && !awaitingHistory
    // Retained content starts visible; refreshes never reset a completed entrance.
    val contentOpacity = remember(session.hostId, session.id) {
        Animatable(if (contentReady) 1f else 0f)
    }
    LaunchedEffect(contentReady, contentOpacity) {
        if (contentReady && contentOpacity.value < 1f) contentOpacity.animateTo(1f, tween(300))
    }
    // Loading is presentation only; subscription readiness still gates runtime actions.
    val workStatus = when {
        load == LoadState.Loading -> SessionWorkStatus(WorkStatusKind.Loading, T("正在加载会话", "Loading conversation"))
        load !is LoadState.Ready -> null
        detail.snapshotToken == null && state.host?.connected == true ->
            SessionWorkStatus(WorkStatusKind.Loading, T("正在加载会话", "Loading conversation"))
        else -> {
            val current = remember(session, detail.liveItems, detail.streaming.isNotBlank(),
                state.host?.connection, state.sendProgress) {
                sessionWorkStatus(detail, state.host, state.sendProgress)
            }
            if (awaitingHistory && (current.kind == WorkStatusKind.Ready || current.kind == WorkStatusKind.OnHost)) {
                SessionWorkStatus(WorkStatusKind.Loading, T("正在加载会话", "Loading conversation"))
            } else current
        }
    }
    val timelineState = rememberLazyListState(initialFirstVisibleItemIndex = Int.MAX_VALUE)
    val historyPullState = rememberPullToRefreshState()
    var followTimeline by rememberSaveable(session.id) { mutableStateOf(true) }
    val canLoadEarlier = isActive && savedHistory.nextCursor != null &&
        host?.connected == true && !activity.history
    val onLoadEarlier: () -> Boolean = {
        if (canLoadEarlier) {
            followTimeline = false
            onAction(SessionAction.LoadEarlierHistory)
            true
        } else false
    }
    LaunchedEffect(isActive) {
        if (isActive) followTimeline = true
    }

    val inputEnabled = controls.inputEnabled
    var composerHeightPx by remember(session.id) { mutableIntStateOf(0) }
    LaunchedEffect(timelineState) {
        snapshotFlow {
            (if (timelineState.isScrollInProgress) 1 else 0) or
                (if (timelineState.lastScrolledBackward) 2 else 0) or
                (if (timelineState.canScrollForward) 4 else 0)
        }.collect { scrollFlags ->
            if (scrollFlags and 3 == 3) {
                followTimeline = false
            } else if (scrollFlags and 4 == 0) {
                followTimeline = true
            }
        }
    }
    LaunchedEffect(
        isActive,
        conversationDisplay,
        detail.streaming,
        session.attention,
        composerHeightPx,
        composerClearance,
        followTimeline,
    ) {
        if (!isActive || !followTimeline) return@LaunchedEffect
        withFrameNanos { }
        val lastItemIndex = timelineState.layoutInfo.totalItemsCount - 1
        if (lastItemIndex >= 0) timelineState.scrollToItem(lastItemIndex)
    }
    val density = LocalDensity.current
    // Plan 模式：切换后，下一条消息前缀 "/plan" 以进入 OMP 的只读规划模式（再次切换取消）。
    var planMode by rememberSaveable(session.id) { mutableStateOf(false) }
    // 「我发的消息」在时间轴中的索引（用于右侧快速定位控件）。
    val userMessageIndices = remember(conversationDisplay) {
        conversationDisplay.withIndex()
            .filter { (_, item) -> item is SessionDisplayItem.Message && item.role == "user" }
            .map { it.index }
    }
    val navScope = rememberCoroutineScope()
    // 回复完成：streaming 由非空转空 → 细微震动（Grok 风格），仅当前活跃页触发。
    val contextForHaptic = LocalContext.current
    var wasStreaming by remember { mutableStateOf(false) }
    LaunchedEffect(detail.streaming.isNotBlank()) {
        val streaming = detail.streaming.isNotBlank()
        if (wasStreaming && !streaming && isActive) {
            performCompletionHaptic(contextForHaptic)
        }
        wasStreaming = streaming
    }
    // 沉浸模式：滑动消息区时隐藏顶栏/输入框等，全屏只显示消息区（状态栏不隐藏）。
    var immersive by remember { mutableStateOf(false) }
    // 是否已处于列表底部（用于「拉到底自动恢复界面」的判断）。
    var atBottom by remember { mutableStateOf(true) }
    LaunchedEffect(timelineState) {
        snapshotFlow {
            Triple(
                timelineState.isScrollInProgress,
                timelineState.lastScrolledBackward,
                timelineState.canScrollForward,
            )
        }.collect { (scrolling, scrolledBackward, canScrollForward) ->
            if (scrolling) immersive = true
            // 列表已滚到底部（无法再向前滚动）→ 记录到达底部。
            // 但若用户正在向后（向上）翻看历史，则不视为「到底」。
            atBottom = !canScrollForward && !scrolledBackward
        }
    }
    // 拉到底后自动退出沉浸，并恢复顶栏/输入框。
    LaunchedEffect(immersive, atBottom) {
        if (immersive && atBottom) {
            kotlinx.coroutines.delay(320)
            if (immersive && atBottom) immersive = false
        }
    }
    // 同步到全局，供 TasksScreen 隐藏顶栏（仅当前活跃页写入，避免多页互相覆盖）。
    androidx.compose.runtime.SideEffect {
        if (isActive) AppImmersive.active = immersive
    }
    // 页面失活/销毁时复位，避免顶栏一直隐藏。
    androidx.compose.runtime.DisposableEffect(isActive) {
        onDispose { AppImmersive.active = false }
    }
    val composerClearanceTarget =
        if (composerHidden || immersive) 0.dp else with(density) { composerHeightPx.toDp() } + 12.dp
    val composerClearance by animateDpAsState(composerClearanceTarget, tween(280), label = "composerClearance")
    val imeInsets = WindowInsets.ime
    val navigationInsets = WindowInsets.navigationBars
    // IME insets animate every frame. Moving the composer during placement keeps that
    // animation from remeasuring its text field, buttons, and attachment chips.
    val keyboardOffset = Modifier.offset {
        val overlap = (imeInsets.getBottom(this) - navigationInsets.getBottom(this))
            .coerceAtLeast(0)
        IntOffset(0, -overlap)
    }
    val composerShape = RoundedCornerShape(14.dp)
    val composerFill = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
    fun Modifier.composerCard() = this
        .shadow(
            elevation = 18.dp,
            shape = composerShape,
            clip = false,
            ambientColor = ScrimBase.copy(alpha = 0.62f),
            spotColor = Purple400.copy(alpha = 0.18f),
        )
        .clip(composerShape)
        .background(composerFill)
    val placeholder = localizePlaceholder(controls.placeholder)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val stackComposerControls = maxWidth < 360.dp * density.fontScale
        val todoHeaderHeight = todoPanelHeaderHeight()
        val todoClearance = if (todo != null && !immersive) todoHeaderHeight + 12.dp else 0.dp
        val keyboardHeight = with(density) {
            (imeInsets.getBottom(this) - navigationInsets.getBottom(this)).coerceAtLeast(0).toDp()
        }
        val panelMaxHeight = (maxHeight - composerClearance - keyboardHeight - 12.dp).coerceAtLeast(todoHeaderHeight)
        if (load is LoadState.Ready) MaterialTheme(
            colorScheme = MaterialTheme.colorScheme,
            typography = sessionTypographyFor(messageFontSize),
        ) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = contentOpacity.value }
                .pullToRefresh(
                state = historyPullState,
                isRefreshing = activity.history,
                enabled = canLoadEarlier,
                onRefresh = { onLoadEarlier() },
            )) {
                LazyColumn(
                    Modifier.fillMaxSize().testTag("sessionTimeline").semantics {
                        if (canLoadEarlier) {
                            customActions = listOf(CustomAccessibilityAction("Load earlier messages", onLoadEarlier))
                        }
                    },
                    state = timelineState,
                    contentPadding = PaddingValues(
                        top = todoClearance + 8.dp,
                        // Keeps the last timeline item clear of the floating composer card.
                        bottom = composerClearance,
                    ),
                    verticalArrangement = Arrangement.spacedBy(if (compactDensity) 0.dp else 2.dp),
                ) {
                    items(conversationDisplay, key = { it.id }, contentType = { it::class }) { item ->
                        val running = item is SessionDisplayItem.ActivityGroup && item.operations.any { it.callId in liveOperationIds }
                        DisplayItem(item, renderer, liveActivity = running && host?.connected == true &&
                            session.status == SessionStatus.Running, toolDetails = toolDetails)
                    }
                    if (session.runtimeAttached && detail.streaming.isNotBlank()) item(key = "streaming") {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .timelineBand(tint = Purple400, tintAlpha = 0.055f)
                                .padding(12.dp),
                        ) {
                            AgentHeader(detail.model, replying = true)
                            Spacer(Modifier.height(4.dp))
                            TimelineMessageBody(detail.streaming) { _ ->
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(detail.streaming, style = MaterialTheme.typography.bodySmall, color = TextHigh)
                                    StreamingCursor()
                                }
                            }
                        }
                    }
                    detail.operations.lastOrNull()?.let { receipt ->
                        operationStatusText(receipt)?.let { text ->
                            item(key = "operation-${receipt.commandId}") {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ) {
                                    Text(text, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    session.attention?.let { attention -> item(key = "attention") { AttentionCard(attention, !activity.inputBusy && attached, onRespond) } }
                    if (historyError != null) item(key = "history-failed") {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                            Text(historyError, color = TextMid,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    // A separate end anchor reaches the bottom even when the final message is taller than the viewport.
                    item(key = "timeline-end") { Spacer(Modifier.height(1.dp)) }
                }
                if (savedHistory.nextCursor != null || activity.history) {
                    PullToRefreshDefaults.Indicator(
                        state = historyPullState,
                        isRefreshing = activity.history,
                        modifier = Modifier.align(Alignment.TopCenter)
                            .offset(y = todoClearance).testTag("historyPullIndicator"),
                        color = BrandPink,
                    )
                }
                if (isActive && userMessageIndices.isNotEmpty()) {
                    UserMessageNavigator(
                        userMessageIndices = userMessageIndices,
                        timelineState = timelineState,
                        onJump = { index -> navScope.launch { timelineState.animateScrollToItem(index) } },
                        onScrub = { index -> navScope.launch { timelineState.scrollToItem(index) } },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .heightIn(max = 420.dp)
                            .fillMaxHeight(0.6f)
                            .padding(end = 2.dp, top = todoClearance + 12.dp, bottom = composerClearance + 12.dp),
                    )
                }
            }
        }
        if (load is LoadState.Failed) {
            EmptyState("Could not load this session", load.message,
                modifier = Modifier.padding(bottom = composerClearance))
        } else if (load == LoadState.Loading || awaitingHistory) {
            TimelineLoadingState(Modifier.padding(bottom = composerClearance)
                .testTag(if (load == LoadState.Loading) "sessionLoading" else "historyLoading"))
        }
        AnimatedVisibility(
            visible = composerHidden || immersive,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(if (immersiveButtonLeft) Alignment.BottomStart else Alignment.BottomEnd)
                .then(keyboardOffset)
                .padding(
                    start = if (immersiveButtonLeft) 16.dp else 0.dp,
                    end = if (immersiveButtonLeft) 0.dp else 16.dp,
                    bottom = 20.dp,
                ),
        ) {
            // 输入框隐藏 / 沉浸模式：底部只留一个轻量浮标，点击唤出。
            Surface(
                onClick = {
                    immersive = false
                    if (composerHidden) onShowComposer()
                },
                modifier = Modifier.testTag("showComposerButton"),
                shape = RoundedCornerShape(24.dp),
                // 沉浸模式下浮标半透明，尽量不遮挡消息区。
                color = if (immersive) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = if (immersive) 0.dp else 6.dp,
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Outlined.KeyboardArrowUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(T("输入", "Input"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        AnimatedVisibility(
            visible = !(composerHidden || immersive),
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(280)) { it } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(240)) { it } + fadeOut(tween(180)),
        ) {
        Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .then(keyboardOffset)
                .fillMaxWidth()
                .height(composerClearance + 32.dp)
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        // Reach the work-status strip already dimmed, then fade to base below it.
                        (32.dp / (composerClearance + 32.dp)) to ScrimBase.copy(alpha = 0.60f),
                        1.00f to ScrimBase,
                    ),
                ),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .then(keyboardOffset)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .onSizeChanged { composerHeightPx = it.height },
        ) {
            workStatus?.let { SessionWorkStatusLine(it) }
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .composerCard()
                        .testTag("sessionComposer"),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    val composerTextStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(start = ComposerContentInset),
                        verticalAlignment = Alignment.Top,
                    ) {
                        BasicTextField(
                            value = prompt,
                            onValueChange = onDraftTextChange,
                            modifier = Modifier
                                .testTag("sessionInput")
                                .weight(1f)
                                .heightIn(min = 48.dp, max = 180.dp)
                                // Center the first 20sp line beside the 48dp attachment target.
                                .padding(top = 14.dp)
                                .onFocusChanged {
                                    if (it.isFocused) todoExpanded = false
                                },
                            enabled = inputEnabled,
                            textStyle = composerTextStyle.copy(
                                color = if (inputEnabled) TextHigh else Gray400.copy(alpha = 0.65f),
                            ),
                            cursorBrush = SolidColor(Purple400),
                            maxLines = composerMaxLines,
                            // 输入法回车键 = 发送（Inbox 保持换行需 Shift+Enter，移动端用发送更顺手）。
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (inputEnabled && controls.canSend) {
                                    fileError = null
                                    onPrompt()
                                }
                            }),
                            decorationBox = { innerTextField ->
                                Box(Modifier.fillMaxWidth()) {
                                    if (prompt.isEmpty()) {
                                        Text(
                                            placeholder,
                                            style = composerTextStyle,
                                            color = Gray400.copy(alpha = if (inputEnabled) 0.82f else 0.48f),
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )
                        IconButton(
                            onClick = rememberHapticOnClick { picker.launch(arrayOf("*/*")) },
                            enabled = controls.canAttach,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                Icons.Outlined.AttachFile,
                                contentDescription = T("添加附件", "Attach files"),
                                modifier = Modifier.size(20.dp),
                                tint = if (controls.canAttach) TextMid else Gray400.copy(alpha = 0.34f),
                            )
                        }
                    }
                    if (selectedFiles.isNotEmpty()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = ComposerContentInset)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            selectedFiles.forEach { file ->
                                InputChip(
                                    selected = true,
                                    enabled = inputEnabled,
                                    onClick = { onFileRemoved(file.id) },
                                    label = { Text(file.name, maxLines = 1) },
                                    trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = T("移除 ${file.name}", "Remove ${file.name}"), modifier = Modifier.size(14.dp)) },
                                )
                            }
                        }
                    }
                    fileError?.let {
                        Text(
                            it,
                            modifier = Modifier.padding(horizontal = ComposerContentInset),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Box(Modifier.fillMaxWidth()) {
                        // Use the page width: a subcomposed layout cannot participate in the rail's intrinsic-height pass.
                        if (stackComposerControls) {
                            Column(
                                Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.End,
                            ) {
                                ComposerModelButton(
                                    label = composerModelLabel(detail.model),
                                    thinkingLevel = composerThinkingLabel(detail.model),
                                    onClick = { showModels = true },
                                    enabled = controls.canChooseModel,
                                )
                                ComposerPlanSwitch(
                                    checked = planMode,
                                    enabled = inputEnabled,
                                    onToggle = {
                                        planMode = !planMode
                                        // 切换即把 /plan 或 /plan off 写入草稿，发送后由 OMP 处理。
                                        val cmd = if (planMode) "/plan" else "/plan off"
                                        onDraftTextChange(if (draft.text.isBlank()) cmd else "$cmd ${draft.text}")
                                    },
                                )
                            }
                        } else {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ComposerModelButton(
                                    label = composerModelLabel(detail.model),
                                    thinkingLevel = composerThinkingLabel(detail.model),
                                    onClick = { showModels = true },
                                    enabled = controls.canChooseModel,
                                    modifier = Modifier.weight(1f),
                                )
                                ComposerPlanSwitch(
                                    checked = planMode,
                                    enabled = inputEnabled,
                                    onToggle = {
                                        planMode = !planMode
                                        val cmd = if (planMode) "/plan" else "/plan off"
                                        onDraftTextChange(if (draft.text.isBlank()) cmd else "$cmd ${draft.text}")
                                    },
                                )
                            }
                        }
                    }
                }
                ComposerRail(
                    controls = controls,
                    onCommand = onCommand,
                    onExit = { showExitConfirmation = true },
                    onSend = { fileError = null; onPrompt() },
                    modifier = Modifier.align(Alignment.Bottom),
                )
            }
        }
        }
        }
        if (todo != null && !immersive) {
            AnimatedVisibility(todoExpanded, modifier = Modifier.align(Alignment.TopCenter),
                enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxWidth().height(panelMaxHeight).testTag("todoDismissArea")
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        todoExpanded = false
                    })
            }
            key(session.hostId, session.id) {
                FloatingTodoPanel(todo, live = host?.connected == true && session.status == SessionStatus.Running &&
                    liveTodo is SessionTodo.Snapshot,
                    expanded = todoExpanded, onExpandedChange = { todoExpanded = it },
                    maxHeight = panelMaxHeight.coerceAtMost(maxHeight * 0.65f),
                    modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
    }
    LaunchedEffect(showModels, attached, session.generation) {
        if (showModels && attached) onLoadModels(false)
    }
    if (showModels) {
        ModelPickerSheet(
            state = modelState,
            usageState = state.usage,
            loadUsage = { onAction(SessionAction.LoadUsage) },
            current = detail.model,
            enabled = attached && !activity.inputBusy,
            runtimeAttached = attached,
            runtimeStarting = session.status == SessionStatus.Starting,
            canStartRuntime = controls.canChooseModel,
            startRuntime = { onCommand(SessionUserCommand.Start) },
            runtimeStartAttempt = state.runtimeStart,
            dismiss = { showModels = false },
            retry = { onLoadModels(true) },
            refresh = { onLoadModels(true) },
            apply = { changes ->
                if (onApplyModelSettings(changes)) showModels = false
            },
        )
    }
    if (showExitConfirmation) {
        ExitConfirmationDialog(
            dismiss = { showExitConfirmation = false },
            confirm = {
                showExitConfirmation = false
                onCommand(SessionUserCommand.Stop)
            },
        )
    }
}

/**
 * 右侧「跳到我发的消息」定位条。
 *
 * 在一条竖直轨道上按比例标出每条「我发送的消息」，顶部圆点随滚动联动高亮；
 * 拖动或点击轨道即可平滑定位到最近的一条用户消息。
 */
@Composable
private fun UserMessageNavigator(
    userMessageIndices: List<Int>,
    timelineState: androidx.compose.foundation.lazy.LazyListState,
    onJump: (Int) -> Unit,
    onScrub: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (userMessageIndices.isEmpty()) return
    val accent = MaterialTheme.colorScheme.primary
    val track = TextMid.copy(alpha = 0.45f)
    // 当前可视首项对应的「我发的消息」序号（用于高亮）。
    val activeOrdinal by remember(userMessageIndices) {
        derivedStateOf {
            val first = timelineState.firstVisibleItemIndex
            var ordinal = 0
            for (i in userMessageIndices.indices) {
                if (userMessageIndices[i] <= first) ordinal = i else break
            }
            ordinal
        }
    }
    val lastIndex = userMessageIndices.last()
    val firstIndex = userMessageIndices.first()
    val span = (lastIndex - firstIndex).coerceAtLeast(1)

    Box(
        modifier
            .width(30.dp)
            .pointerInput(userMessageIndices) {
                fun locate(y: Float, scrub: Boolean) {
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    val fraction = (y / h).coerceIn(0f, 1f)
                    val target = firstIndex + (fraction * span).roundToInt()
                    val nearest = userMessageIndices.minByOrNull { abs(it - target) } ?: return
                    if (scrub) onScrub(nearest) else onJump(nearest)
                }
                detectDragGestures(
                    onDragStart = { offset -> locate(offset.y, scrub = true) },
                    onDrag = { change, _ -> locate(change.position.y, scrub = true); change.consume() },
                    onDragEnd = { },
                    onDragCancel = { },
                )
            }
            .pointerInput(userMessageIndices) {
                detectTapGestures { offset ->
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    val fraction = (offset.y / h).coerceIn(0f, 1f)
                    val target = firstIndex + (fraction * span).roundToInt()
                    userMessageIndices.minByOrNull { abs(it - target) }?.let(onJump)
                }
            }
            .testTag("userMessageNavigator"),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val h = size.height
            val top = 8.dp.toPx()
            val bottom = h - 8.dp.toPx()
            val usable = (bottom - top).coerceAtLeast(1f)
            // 半透明胶囊底，确保控件在任意底色上都能被看到。
            drawRoundRect(
                color = track.copy(alpha = 0.18f),
                topLeft = androidx.compose.ui.geometry.Offset(cx - 9.dp.toPx(), top),
                size = Size(18.dp.toPx(), usable),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),
            )
            userMessageIndices.forEachIndexed { ordinal, index ->
                val fraction = (index - firstIndex).toFloat() / span
                val y = top + fraction * usable
                val isActive = ordinal == activeOrdinal
                if (isActive) {
                    drawCircle(color = accent.copy(alpha = 0.25f), radius = 7.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(cx, y))
                }
                drawCircle(
                    color = if (isActive) accent else track,
                    radius = (if (isActive) 3.5.dp else 2.dp).toPx(),
                    center = androidx.compose.ui.geometry.Offset(cx, y),
                )
            }
        }
    }
}
