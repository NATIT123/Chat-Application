package com.example.chatapplication.models

import java.io.Serializable


data class User(
    var id: String = "",
    var name: String = "",
    var email: String = "",
    var image: String = "",
    var token: String = ""
):Serializable
