package com.fitquest.app.util.messages

object HeroMessages {
    data class Message(val text: String, val emoji: String)

    private val messages = listOf(
        Message("Level up today, warrior", "\uD83C\uDFC6"),   // 🏆
        Message("Conquer your limits", "\u2694\uFE0F"),       // ⚔️
        Message("Every step counts", "\uD83D\uDC63"),        // 👣
        Message("Strength is earned, not given", "\uD83D\uDCAA"), // 💪
        Message("Your journey, your glory", "\u2728"),       // ✨
        Message("Keep pushing forward", "\uD83D\uDE80"),    // 🚀
        Message("Victory favors the bold", "\uD83C\uDFC6"), // 🏆
        Message("Forge your strength", "\uD83D\uDD28"),      // 🔨
        Message("Rise stronger than yesterday", "\uD83C\uDF1F"), // 🌟
        Message("Your next quest awaits", "\u2694\uFE0F"),   // ⚔️
        Message("The path to power continues", "\uD83D\uDC51"),   // 👑
        Message("Unbreakable spirit", "\uD83D\uDEE1\uFE0F"), // 🛡️
        Message("Sweat is your armor", "\uD83E\uDDBbe"),     // 🦾
        Message("Pain is temporary", "\u23F3"),              // ⏳
        Message("Become the champion", "\uD83E\uDD47"),      // 🥇
        Message("Defy the odds", "\uD83C\uDFB2"),            // 🎲
    )

    fun random(): Message = messages.random()
}
