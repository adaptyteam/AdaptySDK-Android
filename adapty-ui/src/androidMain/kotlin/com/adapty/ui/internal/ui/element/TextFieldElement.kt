@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.adapty.ui.internal.ui.element

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType as ComposeContentType
import androidx.compose.ui.text.input.KeyboardType as ComposeKeyboardType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.ui.internal.script.get
import com.adapty.ui.internal.text.ComposeTextAttrs
import com.adapty.ui.internal.text.StringId
import com.adapty.ui.internal.text.toPlainString
import com.adapty.ui.internal.ui.LocalCurrentFocusId
import com.adapty.ui.internal.ui.LocalFocusCommand
import com.adapty.ui.internal.ui.LocalScreenInstance
import com.adapty.ui.internal.ui.LocalUiEnabled
import com.adapty.ui.internal.ui.attributes.TextAlign
import com.adapty.ui.internal.ui.attributes.toComposeTextAlign
import com.adapty.ui.internal.ui.resolveAssets
import com.adapty.ui.internal.ui.resolveState
import com.adapty.ui.internal.ui.resolveText
import com.adapty.ui.internal.ui.OverflowResult
import com.adapty.ui.internal.ui.ScaleDownToFit
import com.adapty.ui.internal.ui.ScaleDownToFitTextAutoSize
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import com.adapty.ui.internal.store.Message
import com.adapty.ui.internal.utils.LOG_PREFIX
import com.adapty.ui.internal.utils.OneWayBinding
import com.adapty.ui.internal.utils.TwoWayBinding
import com.adapty.ui.internal.utils.log
import com.adapty.utils.AdaptyLogLevel.Companion.WARN

@InternalAdaptyApi
public class TextFieldElement internal constructor(
    internal val value: TwoWayBinding,
    internal val textAlign: TextAlign,
    internal val maxLines: Int?,
    internal val minLines: Int?,
    internal val attributes: BaseTextElement.Attributes,
    internal val placeholder: Placeholder?,
    internal val secureEntry: Boolean,
    internal val keyboardOptions: TextFieldKeyboardOptions?,
    internal val inputConstraints: InputConstraints?,
    internal val validation: OneWayBinding?,
    internal val invalidAttributes: BaseTextElement.Attributes?,
    internal val submitActions: List<Action>,
    internal val isEditor: Boolean,
    override val baseProps: BaseProps,
) : UIElement {

    @InternalAdaptyApi
    public class Placeholder internal constructor(
        internal val stringId: StringId,
        internal val attributes: BaseTextElement.Attributes,
        internal val onOverflow: BaseTextElement.OnOverflowMode?,
    )

    @InternalAdaptyApi
    public class TextFieldKeyboardOptions internal constructor(
        internal val keyboardType: KeyboardType?,
        internal val contentType: ContentType?,
        internal val autoCapitalization: AutoCapitalization?,
        internal val submitButton: SubmitButton?,
    )

    @InternalAdaptyApi
    public enum class KeyboardType {
        DEFAULT, EMAIL, NUMBER, DECIMAL, PHONE, URL, ASCII;

        public fun toCompose(): ComposeKeyboardType = when (this) {
            DEFAULT -> ComposeKeyboardType.Text
            EMAIL -> ComposeKeyboardType.Email
            NUMBER -> ComposeKeyboardType.Number
            DECIMAL -> ComposeKeyboardType.Decimal
            PHONE -> ComposeKeyboardType.Phone
            URL -> ComposeKeyboardType.Uri
            ASCII -> ComposeKeyboardType.Ascii
        }
    }

    @InternalAdaptyApi
    public enum class ContentType {
        EMAIL, PHONE, USERNAME, PASSWORD, NEW_PASSWORD,
        NAME, GIVEN_NAME, FAMILY_NAME, MIDDLE_NAME, NAME_PREFIX, NAME_SUFFIX,
        ONE_TIME_CODE,
        POSTAL_CODE, STREET_ADDRESS, CITY, STATE, COUNTRY,
        CREDIT_CARD_NUMBER, CREDIT_CARD_SECURITY_CODE, CREDIT_CARD_EXPIRATION,
        CREDIT_CARD_EXPIRATION_MONTH, CREDIT_CARD_EXPIRATION_YEAR,
        BIRTHDATE, BIRTHDATE_DAY, BIRTHDATE_MONTH, BIRTHDATE_YEAR;

        internal fun toCompose(): ComposeContentType = when (this) {
            EMAIL -> ComposeContentType.EmailAddress
            PHONE -> ComposeContentType.PhoneNumber
            USERNAME -> ComposeContentType.Username
            PASSWORD -> ComposeContentType.Password
            NEW_PASSWORD -> ComposeContentType.NewPassword
            NAME -> ComposeContentType.PersonFullName
            GIVEN_NAME -> ComposeContentType.PersonFirstName
            FAMILY_NAME -> ComposeContentType.PersonLastName
            MIDDLE_NAME -> ComposeContentType.PersonMiddleName
            NAME_PREFIX -> ComposeContentType.PersonNamePrefix
            NAME_SUFFIX -> ComposeContentType.PersonNameSuffix
            ONE_TIME_CODE -> ComposeContentType.SmsOtpCode
            POSTAL_CODE -> ComposeContentType.PostalCode
            STREET_ADDRESS -> ComposeContentType.AddressStreet
            CITY -> ComposeContentType.AddressLocality
            STATE -> ComposeContentType.AddressRegion
            COUNTRY -> ComposeContentType.AddressCountry
            CREDIT_CARD_NUMBER -> ComposeContentType.CreditCardNumber
            CREDIT_CARD_SECURITY_CODE -> ComposeContentType.CreditCardSecurityCode
            CREDIT_CARD_EXPIRATION -> ComposeContentType.CreditCardExpirationDate
            CREDIT_CARD_EXPIRATION_MONTH -> ComposeContentType.CreditCardExpirationMonth
            CREDIT_CARD_EXPIRATION_YEAR -> ComposeContentType.CreditCardExpirationYear
            BIRTHDATE -> ComposeContentType.BirthDateFull
            BIRTHDATE_DAY -> ComposeContentType.BirthDateDay
            BIRTHDATE_MONTH -> ComposeContentType.BirthDateMonth
            BIRTHDATE_YEAR -> ComposeContentType.BirthDateYear
        }
    }

    @InternalAdaptyApi
    public enum class AutoCapitalization {
        NEVER, SENTENCES, WORDS, CHARACTERS;

        public fun toCompose(): KeyboardCapitalization = when (this) {
            NEVER -> KeyboardCapitalization.None
            SENTENCES -> KeyboardCapitalization.Sentences
            WORDS -> KeyboardCapitalization.Words
            CHARACTERS -> KeyboardCapitalization.Characters
        }
    }

    @InternalAdaptyApi
    public enum class SubmitButton {
        DONE, GO, SEARCH, SEND, NEXT, RETURN;

        public fun toCompose(): ImeAction = when (this) {
            DONE -> ImeAction.Done
            GO -> ImeAction.Go
            SEARCH -> ImeAction.Search
            SEND -> ImeAction.Send
            NEXT -> ImeAction.Next
            RETURN -> ImeAction.Default
        }
    }

    @InternalAdaptyApi
    public class InputConstraints internal constructor(
        internal val regex: Regex?,
        internal val maxLength: Int?,
    )

    override fun toComposable(
        dispatch: (Message) -> Unit,
        modifier: Modifier,
    ): @Composable () -> Unit = {
        val state = resolveState()
        val screen = LocalScreenInstance.current
        val isEnabled = LocalUiEnabled.current
        val currentText = (state[value] as? String) ?: ""

        var fieldValue by remember { mutableStateOf(TextFieldValue(currentText)) }
        val pendingTexts = remember { ArrayDeque<String>() }
        LaunchedEffect(currentText) {
            when {
                currentText in pendingTexts -> {
                    while (pendingTexts.isNotEmpty() && pendingTexts.removeFirst() != currentText) Unit
                }
                currentText == fieldValue.text -> Unit
                else -> {
                    pendingTexts.clear()
                    fieldValue = TextFieldValue(currentText, TextRange(currentText.length))
                }
            }
        }

        val focusId = baseProps.focusId
        val focusRequester = if (focusId != null) remember(focusId) { FocusRequester() } else null
        val focusCommand = LocalFocusCommand.current
        var hadFocus by remember { mutableStateOf(false) }
        if (focusId != null && focusRequester != null && focusCommand?.focusId == focusId) {
            LaunchedEffect(focusCommand) {
                runCatching { focusRequester.requestFocus() }
                    .onFailure { log(WARN) { "$LOG_PREFIX TextField: focus request for '$focusId' dropped (${it.message})" } }
                dispatch(Message.FocusCommandConsumed)
            }
        }

        val currentFocusId = LocalCurrentFocusId.current
        if (focusId != null && focusRequester != null) {
            val reclaimed = remember { mutableStateOf(false) }
            SideEffect {
                if (!reclaimed.value && currentFocusId == focusId) {
                    reclaimed.value = true
                    runCatching { focusRequester.requestFocus() }
                        .onFailure { log(WARN) { "$LOG_PREFIX TextField: focus restore for '$focusId' dropped (${it.message})" } }
                }
            }
        }

        val isValid = validation?.let { binding ->
            val result = state[binding]
            result as? Boolean ?: true
        } ?: true

        val effectiveAttributes = if (!isValid && invalidAttributes != null) invalidAttributes else attributes
        val assets = resolveAssets()
        val effectiveTextAttrs = ComposeTextAttrs.from(effectiveAttributes, assets)
        val fontSize = effectiveTextAttrs.fontSize ?: BaseTextElement.DEFAULT_FONT_SIZE

        val textStyle = LocalTextStyle.current.merge(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            color = effectiveTextAttrs.textColor ?: BaseTextElement.DEFAULT_TEXT_COLOR,
            fontFamily = effectiveTextAttrs.fontFamily,
            fontSize = fontSize.sp,
            lineHeight = effectiveTextAttrs.lineHeight?.sp ?: TextUnit.Unspecified,
            textDecoration = effectiveTextAttrs.textDecoration,
            textAlign = textAlign.toComposeTextAlign(),
            letterSpacing = effectiveTextAttrs.letterSpacing?.sp ?: TextUnit.Unspecified,
        )

        val resolveTextFn = resolveText
        val placeholderTextStyle = placeholder?.let { ph ->
            val phAttrs = ComposeTextAttrs.from(ph.attributes, assets)
            val phFontSize = phAttrs.fontSize ?: fontSize
            LocalTextStyle.current.merge(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                color = phAttrs.textColor ?: BaseTextElement.DEFAULT_TEXT_COLOR,
                fontFamily = phAttrs.fontFamily,
                fontSize = phFontSize.sp,
                lineHeight = phAttrs.lineHeight?.sp ?: TextUnit.Unspecified,
                textDecoration = phAttrs.textDecoration,
                textAlign = textAlign.toComposeTextAlign(),
                letterSpacing = phAttrs.letterSpacing?.sp ?: TextUnit.Unspecified,
            )
        }

        val phOverflowAtMinState = remember { mutableStateOf<OverflowResult?>(null) }
        val phAutoSize = remember(
            placeholder?.onOverflow,
            placeholderTextStyle?.fontSize,
            maxLines,
            LocalDensity.current.fontScale,
        ) {
            phOverflowAtMinState.value = null
            val phFs = placeholderTextStyle?.fontSize?.value
            if (placeholder?.onOverflow == BaseTextElement.OnOverflowMode.SCALE && phFs != null)
                TextAutoSize.ScaleDownToFit(
                    minFontSize = (phFs * BaseTextElement.MIN_FONT_SIZE_RATIO).sp,
                    maxFontSize = phFs.sp,
                    onOverflowAtMin = { overflow -> phOverflowAtMinState.value = overflow },
                )
            else null
        }

        val kbOpts = this@TextFieldElement.keyboardOptions

        val bringIntoViewRequester = remember { BringIntoViewRequester() }
        @OptIn(ExperimentalLayoutApi::class)
        val imeVisible = WindowInsets.isImeVisible
        var isFieldFocused by remember { mutableStateOf(false) }
        LaunchedEffect(isFieldFocused, imeVisible) {
            if (isFieldFocused) {
                runCatching { bringIntoViewRequester.bringIntoView() }
            }
        }

        val resolvedMinLines: Int
        val resolvedMaxLines: Int
        if (!isEditor) {
            resolvedMinLines = 1
            resolvedMaxLines = 1
        } else if (minLines != null && maxLines != null && minLines > maxLines) {
            LaunchedEffect(Unit) {
                log(WARN) { "$LOG_PREFIX TextField: clamping min_rows=$minLines to max_rows=$maxLines" }
            }
            resolvedMinLines = maxLines
            resolvedMaxLines = maxLines
        } else {
            resolvedMinLines = minLines ?: 1
            resolvedMaxLines = maxLines ?: Int.MAX_VALUE
        }

        BasicTextField(
            value = fieldValue,
            enabled = isEnabled,
            onValueChange = handler@{ newValue ->
                val constrained = inputConstraints?.let { c ->
                    var v = newValue.text
                    c.maxLength?.let { max ->
                        if (v.length > max) v = v.take(max)
                    }
                    if (v.isNotEmpty()) {
                        c.regex?.let { r ->
                            if (!r.matches(v)) return@handler
                        }
                    }
                    v
                } ?: newValue.text
                val textChanged = constrained != fieldValue.text
                fieldValue =
                    if (constrained == newValue.text) newValue
                    else TextFieldValue(constrained, TextRange(constrained.length))
                if (textChanged) {
                    pendingTexts.addLast(constrained)
                    while (pendingTexts.size > 32) pendingTexts.removeFirst()
                    dispatch(Message.ValueChanged(value, constrained, screen))
                }
            },
            textStyle = textStyle,
            singleLine = !isEditor,
            minLines = resolvedMinLines,
            maxLines = resolvedMaxLines,
            cursorBrush = SolidColor(effectiveTextAttrs.textColor ?: Color.Black),
            visualTransformation = if (secureEntry) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = kbOpts?.keyboardType?.toCompose() ?: ComposeKeyboardType.Text,
                imeAction = kbOpts?.submitButton?.toCompose()
                    ?: if (isEditor) ImeAction.Default else ImeAction.Done,
                capitalization = kbOpts?.autoCapitalization?.toCompose() ?: KeyboardCapitalization.None,
            ),
            keyboardActions = KeyboardActions(
                onAny = {
                    if (submitActions.isNotEmpty()) {
                        dispatch(Message.ActionsRequested(submitActions, screen))
                    } else {
                        dispatch(Message.JSCallback.ChangeFocus(null))
                    }
                },
            ),
            decorationBox = { innerTextField ->
                if (fieldValue.text.isEmpty() && placeholder != null && placeholderTextStyle != null) {
                    val placeholderText = resolveTextFn(placeholder.stringId, placeholder.attributes)
                        ?.toPlainString() ?: ""
                    if (placeholderText.isNotEmpty()) {
                        val phOverflowAtMin = phOverflowAtMinState.value
                        val effectivePhStyle = if (phOverflowAtMin != null && phAutoSize != null)
                            placeholderTextStyle.copy(fontSize = phAutoSize.minFontSize)
                        else
                            placeholderTextStyle
                        BasicText(
                            text = placeholderText,
                            style = effectivePhStyle,
                            maxLines = resolvedMaxLines,
                            overflow = TextOverflow.Ellipsis,
                            autoSize = if (phOverflowAtMin != null) null else phAutoSize,
                        )
                    }
                }
                innerTextField()
            },
            modifier = modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .then(kbOpts?.contentType?.toCompose()?.let { ct -> Modifier.semantics { contentType = ct } } ?: Modifier)
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                .onFocusChanged { focusState ->
                    isFieldFocused = focusState.isFocused
                    val elFocusId = baseProps.focusId
                    if (focusState.isFocused && elFocusId != null) {
                        dispatch(Message.FocusChanged(elFocusId))
                    } else if (!focusState.isFocused && hadFocus && elFocusId != null) {
                        dispatch(Message.FocusLost(elFocusId))
                    }
                    hadFocus = focusState.isFocused
                },
        )
    }
}
