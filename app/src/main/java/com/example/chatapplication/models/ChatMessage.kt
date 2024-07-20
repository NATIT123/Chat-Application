package com.example.chatapplication.models

import java.util.Date

data class ChatMessage(
    var senderId: String = "",
    var receiverId: String = "",
    var message: String = "",
    var date: Date = Date(),
    var conversionId: String = "",
    var conversionName: String = "",
    var conversionImage: String = "",
)