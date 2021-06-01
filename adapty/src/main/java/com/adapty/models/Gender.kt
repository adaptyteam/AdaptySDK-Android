package com.adapty.models

enum class Gender {
    MALE, FEMALE, OTHER;

    override fun toString(): String {
        return this.name[0].toLowerCase().toString()
    }
}