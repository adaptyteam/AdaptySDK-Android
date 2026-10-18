@file:OptIn(InternalAdaptyApi::class)

package com.adapty.ui.internal.mapping.element

import com.adapty.errors.AdaptyErrorCode
import com.adapty.internal.utils.InternalAdaptyApi
import com.adapty.internal.utils.adaptyError
import com.adapty.ui.internal.mapping.attributes.toAction
import com.adapty.ui.internal.mapping.attributes.toTextAlign
import com.adapty.ui.internal.mapping.attributes.toTwoWayBinding
import com.adapty.ui.internal.mapping.attributes.toOneWayBinding
import com.adapty.ui.internal.text.toStringId
import com.adapty.ui.internal.ui.element.TextFieldElement
import com.adapty.ui.internal.ui.element.UIElement

internal fun Map<*, *>.toTextFieldElement(assets: Assets): UIElement {
    val isEditor = this["type"] == "text_editor"
    return TextFieldElement(
        this["value"]?.toTwoWayBinding() ?: throw adaptyError(
            message = "value in TextField must not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        ),
        this["align"].toTextAlign(),
        (this["max_rows"] as? Number)?.toInt()?.takeIf { isEditor && it > 0 },
        (this["min_rows"] as? Number)?.toInt()?.takeIf { isEditor && it > 0 },
        toTextAttributes(),
        (this["placeholder"] as? Map<*, *>)?.toPlaceholder(),
        (this["secure_entry"] as? Boolean) ?: false,
        (this["keyboard_options"] as? Map<*, *>)?.toKeyboardOptions(),
        (this["input_constraints"] as? Map<*, *>)?.toInputConstraints(),
        this["validation"]?.toOneWayBinding(),
        (this["invalid_attributes"] as? Map<*, *>)?.toTextAttributes(),
        (this["submit_action"] as? Iterable<*>)?.mapNotNull { item -> (item as? Map<*, *>)?.toAction() }
            ?: (this["submit_action"] as? Map<*, *>)?.toAction()?.let { action -> listOf(action) }.orEmpty(),
        isEditor,
        this.extractBaseProps(),
    )
}

private fun Map<*, *>.toInputConstraints(): TextFieldElement.InputConstraints {
    return TextFieldElement.InputConstraints(
        (this["regex"] as? String)?.let { Regex(it) },
        (this["max_length"] as? Number)?.toInt()?.takeIf { it > 0 },
    )
}

private fun Map<*, *>.toPlaceholder(): TextFieldElement.Placeholder {
    val stringId = this["string_id"]?.toStringId()
        ?: throw adaptyError(
            message = "string_id in placeholder must not be null",
            adaptyErrorCode = AdaptyErrorCode.DECODING_FAILED
        )
    return TextFieldElement.Placeholder(
        stringId,
        toTextAttributes(),
        this.toOnOverflowMode(),
    )
}

private fun Map<*, *>.toKeyboardOptions(): TextFieldElement.TextFieldKeyboardOptions {
    return TextFieldElement.TextFieldKeyboardOptions(
        keyboardType = (this["keyboard"] as? String)?.toKeyboardType(),
        contentType = (this["content_type"] as? String)?.toContentType(),
        autoCapitalization = (this["auto_capitalization"] as? String)?.toAutoCapitalization(),
        submitButton = (this["submit_button"] as? String)?.toSubmitButton(),
    )
}

private fun String.toKeyboardType(): TextFieldElement.KeyboardType? = when (this) {
    "default" -> TextFieldElement.KeyboardType.DEFAULT
    "email" -> TextFieldElement.KeyboardType.EMAIL
    "number" -> TextFieldElement.KeyboardType.NUMBER
    "decimal" -> TextFieldElement.KeyboardType.DECIMAL
    "phone" -> TextFieldElement.KeyboardType.PHONE
    "url" -> TextFieldElement.KeyboardType.URL
    "ascii" -> TextFieldElement.KeyboardType.ASCII
    else -> null
}

private fun String.toContentType(): TextFieldElement.ContentType? = when (this) {
    "email" -> TextFieldElement.ContentType.EMAIL
    "phone" -> TextFieldElement.ContentType.PHONE
    "username" -> TextFieldElement.ContentType.USERNAME
    "password" -> TextFieldElement.ContentType.PASSWORD
    "new_password" -> TextFieldElement.ContentType.NEW_PASSWORD
    "name" -> TextFieldElement.ContentType.NAME
    "given_name" -> TextFieldElement.ContentType.GIVEN_NAME
    "family_name" -> TextFieldElement.ContentType.FAMILY_NAME
    "middle_name" -> TextFieldElement.ContentType.MIDDLE_NAME
    "name_prefix" -> TextFieldElement.ContentType.NAME_PREFIX
    "name_suffix" -> TextFieldElement.ContentType.NAME_SUFFIX
    "one_time_code" -> TextFieldElement.ContentType.ONE_TIME_CODE
    "postal_code" -> TextFieldElement.ContentType.POSTAL_CODE
    "street_address" -> TextFieldElement.ContentType.STREET_ADDRESS
    "city" -> TextFieldElement.ContentType.CITY
    "state" -> TextFieldElement.ContentType.STATE
    "country" -> TextFieldElement.ContentType.COUNTRY
    "credit_card_number" -> TextFieldElement.ContentType.CREDIT_CARD_NUMBER
    "credit_card_security_code" -> TextFieldElement.ContentType.CREDIT_CARD_SECURITY_CODE
    "credit_card_expiration" -> TextFieldElement.ContentType.CREDIT_CARD_EXPIRATION
    "credit_card_expiration_month" -> TextFieldElement.ContentType.CREDIT_CARD_EXPIRATION_MONTH
    "credit_card_expiration_year" -> TextFieldElement.ContentType.CREDIT_CARD_EXPIRATION_YEAR
    "birthdate" -> TextFieldElement.ContentType.BIRTHDATE
    "birthdate_day" -> TextFieldElement.ContentType.BIRTHDATE_DAY
    "birthdate_month" -> TextFieldElement.ContentType.BIRTHDATE_MONTH
    "birthdate_year" -> TextFieldElement.ContentType.BIRTHDATE_YEAR
    else -> null
}

private fun String.toAutoCapitalization(): TextFieldElement.AutoCapitalization? = when (this) {
    "never" -> TextFieldElement.AutoCapitalization.NEVER
    "sentences" -> TextFieldElement.AutoCapitalization.SENTENCES
    "words" -> TextFieldElement.AutoCapitalization.WORDS
    "characters" -> TextFieldElement.AutoCapitalization.CHARACTERS
    else -> null
}

private fun String.toSubmitButton(): TextFieldElement.SubmitButton? = when (this) {
    "done" -> TextFieldElement.SubmitButton.DONE
    "go" -> TextFieldElement.SubmitButton.GO
    "search" -> TextFieldElement.SubmitButton.SEARCH
    "send" -> TextFieldElement.SubmitButton.SEND
    "next" -> TextFieldElement.SubmitButton.NEXT
    "return" -> TextFieldElement.SubmitButton.RETURN
    else -> null
}
