package com.adapty.ui.onboardings.actions

public sealed class AdaptyOnboardingStateUpdatedParams {
    public class Select(public val params: AdaptyOnboardingSelectParams) :
        AdaptyOnboardingStateUpdatedParams() {

        override fun toString(): String {
            return "Select(params=$params)"
        }
    }

    public class MultiSelect(public val params: Collection<AdaptyOnboardingSelectParams>) :
        AdaptyOnboardingStateUpdatedParams() {

        override fun toString(): String {
            return "MultiSelect(params=$params)"
        }
    }

    public class Input(public val params: AdaptyOnboardingInputParams) :
        AdaptyOnboardingStateUpdatedParams() {

        override fun toString(): String {
            return "Input(params=$params)"
        }
    }

    public class DatePicker(public val params: AdaptyOnboardingDatePickerParams) :
        AdaptyOnboardingStateUpdatedParams() {

        override fun toString(): String {
            return "DatePicker(params=$params)"
        }
    }
}

public class AdaptyOnboardingSelectParams(
    public val id: String,
    public val value: String,
    public val label: String,
) {

    override fun toString(): String {
        return "AdaptyOnboardingSelectParams(id='$id', value='$value', label='$label')"
    }
}

public sealed class AdaptyOnboardingInputParams {
    public class Text(public val value: String) : AdaptyOnboardingInputParams() {

        override fun toString(): String {
            return "Text(value='$value')"
        }
    }

    public class Email(public val value: String) : AdaptyOnboardingInputParams() {

        override fun toString(): String {
            return "Email(value='$value')"
        }
    }

    public class Number(public val value: Double) : AdaptyOnboardingInputParams() {

        override fun toString(): String {
            return "Number(value=$value)"
        }
    }
}

public class AdaptyOnboardingDatePickerParams(
    public val day: Int?,
    public val month: Int?,
    public val year: Int?,
) {

    override fun toString(): String {
        return "AdaptyOnboardingDatePickerParams(day=$day, month=$month, year=$year)"
    }
}
