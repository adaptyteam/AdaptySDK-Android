package com.adapty.internal.crossplatform.ui

class PresentViewArgs(val id: String)

class DismissViewArgs(val id: String, val destroy: Boolean)

class ShowDialogArgs(val id: String, val configuration: AdaptyUiDialogConfig)
