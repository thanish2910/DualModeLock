package com.duallock

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Green = Color(0xFF25D366)
private val DarkBg = Color(0xFF0B141A)
private val DarkPanel = Color(0xFF202C33)
private val TextMain = Color(0xFFE9EDEF)
private val TextMuted = Color(0xFF8696A0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pinStore = PinStore(this)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Green,
                    background = DarkBg,
                    surface = DarkPanel,
                    onBackground = TextMain,
                    onSurface = TextMain
                )
            ) {
                DualLockApp(
                    pinStore = pinStore,
                    openWhatsApp = { openOfficialWhatsApp() }
                )
            }
        }
    }

    private fun openOfficialWhatsApp() {
        val packages = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (packageName in packages) {
            val launch = packageManager.getLaunchIntentForPackage(packageName)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launch)
                return
            }
        }
        // If neither app is installed, open the official WhatsApp website.
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.whatsapp.com/download")))
        } catch (_: ActivityNotFoundException) {
            // No browser is available; the UI remains usable.
        }
    }
}

private enum class Screen { LOCK, SETUP, DECOY, CHAT }

@Composable
private fun DualLockApp(pinStore: PinStore, openWhatsApp: () -> Unit) {
    var screen by remember { mutableStateOf(if (pinStore.isConfigured()) Screen.LOCK else Screen.SETUP) }
    var pin by remember { mutableStateOf("") }
    var selectedChat by remember { mutableStateOf<DemoChat?>(null) }
    var notice by remember { mutableStateOf("") }

    when (screen) {
        Screen.SETUP -> SetupScreen(
            onSave = { mainPin, decoyPin ->
                pinStore.savePins(mainPin, decoyPin)
                pin = ""
                notice = "PINs saved on this device."
                screen = Screen.LOCK
            }
        )
        Screen.LOCK -> LockScreen(
            pin = pin,
            notice = notice,
            onPinChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
            onUnlock = {
                when (pinStore.verify(pin)) {
                    PinStore.Result.MAIN -> {
                        pin = ""
                        notice = ""
                        pinStore.authorizeMainLaunch()
                        openWhatsApp()
                    }
                    PinStore.Result.DECOY -> {
                        pin = ""
                        notice = ""
                        screen = Screen.DECOY
                    }
                    PinStore.Result.INVALID -> {
                        pin = ""
                        notice = "Incorrect PIN. Try again."
                    }
                    PinStore.Result.NOT_CONFIGURED -> screen = Screen.SETUP
                }
            },
            onReset = { screen = Screen.SETUP; pin = ""; notice = "" }
        )
        Screen.DECOY -> DecoyScreen(
            onBack = { screen = Screen.LOCK },
            onOpenChat = { selectedChat = it; screen = Screen.CHAT }
        )
        Screen.CHAT -> selectedChat?.let { chat ->
            ConversationScreen(chat = chat, onBack = { screen = Screen.DECOY })
        } ?: run { screen = Screen.DECOY }
    }
}

@Composable
private fun SetupScreen(onSave: (String, String) -> Unit) {
    var mainPin by remember { mutableStateOf("") }
    var confirmMain by remember { mutableStateOf("") }
    var decoyPin by remember { mutableStateOf("") }
    var confirmDecoy by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Surface(Modifier.fillMaxSize(), color = DarkBg) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Green, modifier = Modifier.size(54.dp))
            Spacer(Modifier.height(12.dp))
            Text("Set up DualMode Lock", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Spacer(Modifier.height(8.dp))
            Text(
                "Create two different PINs. The main PIN launches the official WhatsApp app; the decoy PIN opens fictional local chats.",
                color = TextMuted
            )
            Spacer(Modifier.height(20.dp))
            PinField("Main PIN (4–8 digits)", mainPin) { mainPin = it.filter(Char::isDigit).take(8) }
            PinField("Confirm main PIN", confirmMain) { confirmMain = it.filter(Char::isDigit).take(8) }
            PinField("Decoy PIN (4–8 digits)", decoyPin) { decoyPin = it.filter(Char::isDigit).take(8) }
            PinField("Confirm decoy PIN", confirmDecoy) { confirmDecoy = it.filter(Char::isDigit).take(8) }
            if (error.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = Color(0xFFFF8A80))
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    error = when {
                        mainPin.length !in 4..8 || decoyPin.length !in 4..8 -> "Each PIN must contain 4–8 digits."
                        mainPin != confirmMain || decoyPin != confirmDecoy -> "PIN confirmations do not match."
                        mainPin == decoyPin -> "Main and decoy PINs must be different."
                        else -> ""
                    }
                    if (error.isEmpty()) onSave(mainPin, decoyPin)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black)
            ) { Text("Save PINs") }
            Spacer(Modifier.height(12.dp))
            Text("Prototype note: this protects entry into this app only, not WhatsApp's other launch paths.", color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PinField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Green,
            focusedLabelColor = Green,
            cursorColor = Green
        )
    )
}

@Composable
private fun LockScreen(
    pin: String,
    notice: String,
    onPinChange: (String) -> Unit,
    onUnlock: () -> Unit,
    onReset: () -> Unit
) {
    Surface(Modifier.fillMaxSize(), color = DarkBg) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(88.dp).clip(RoundedCornerShape(26.dp)).background(Green),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Message, contentDescription = null, tint = Color.Black, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(22.dp))
            Text("WhatsApp Lock", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Text("Enter your PIN to continue", color = TextMuted)
            Spacer(Modifier.height(22.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = onPinChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Passcode") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green, cursorColor = Green)
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onUnlock,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black)
            ) { Text("Unlock") }
            if (notice.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(notice, color = Color(0xFFFF8A80))
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onReset) { Text("Reset / change PINs", color = TextMuted) }
        }
    }
}

private data class DemoChat(
    val name: String,
    val message: String,
    val time: String,
    val initials: String,
    val color: Color,
    val messages: List<Pair<String, Boolean>>
)

private val demoChats = listOf(
    DemoChat("Aarav", "Are we meeting tomorrow?", "9:41 PM", "A", Color(0xFF456A8A),
        listOf("Hey, are we meeting tomorrow?" to false, "Yep, around 10?" to true, "Sounds good 👍" to false)),
    DemoChat("Priya", "Okay, see you there!", "8:15 PM", "P", Color(0xFF84678A),
        listOf("Did you finish the notes?" to true, "Almost done" to false, "Okay, see you there!" to true)),
    DemoChat("College Group", "Assignment submitted", "Yesterday", "C", Color(0xFF657D51),
        listOf("Reminder: submit the assignment" to false, "Submitted just now" to true)),
    DemoChat("Rahul", "Thanks!", "Yesterday", "R", Color(0xFFA17B54),
        listOf("Can you share the link?" to false, "Here it is" to true, "Thanks!" to false)),
    DemoChat("Maya", "Sent you the notes", "Yesterday", "M", Color(0xFF9A6A7A),
        listOf("Do you have the notes?" to false, "Sent you the notes" to true)),
    DemoChat("ECE Study Group", "Lab record due Friday", "Tue", "E", Color(0xFF547D78),
        listOf("Lab record due Friday" to false, "Got it" to true)),
    DemoChat("Nikhil", "Cool, thanks!", "Tue", "N", Color(0xFF6C79A8),
        listOf("Can you check this?" to false, "Cool, thanks!" to true)),
    DemoChat("Ananya", "😂😂", "Mon", "A", Color(0xFFAA8055),
        listOf("That was hilarious" to false, "😂😂" to true)),
    DemoChat("Project Team", "I uploaded the file", "Mon", "P", Color(0xFF648B62),
        listOf("Who has the latest file?" to false, "I uploaded the file" to true)),
    DemoChat("Kiran", "See you tomorrow", "Sun", "K", Color(0xFF8B6D9F),
        listOf("Are you coming?" to false, "See you tomorrow" to true)),
    DemoChat("Family Group", "Good morning ☀️", "Sun", "F", Color(0xFF6C8B9B),
        listOf("Good morning ☀️" to false, "Morning!" to true)),
    DemoChat("Meera", "Thank you!", "Sat", "M", Color(0xFFB07875),
        listOf("I sent the document" to false, "Thank you!" to true))
)

@Composable
private fun DecoyScreen(onBack: () -> Unit, onOpenChat: (DemoChat) -> Unit) {
    var search by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val filtered = demoChats.filter {
        it.name.contains(search, ignoreCase = true) ||
            it.message.contains(search, ignoreCase = true)
    }

    Surface(Modifier.fillMaxSize(), color = Color(0xFF0B141A)) {
        Column {
            // Familiar messenger structure, using original styling and no WhatsApp logo.
            Row(
                Modifier.fillMaxWidth().background(Color(0xFF101B22)).padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Chats", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE9EDEF), modifier = Modifier.weight(1f))
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Lock, contentDescription = "Return to lock", tint = Color(0xFFE9EDEF))
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Settings, contentDescription = "Demo settings", tint = Color(0xFFE9EDEF))
                }
            }

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text("Search conversations", color = Color(0xFFB4C0C8)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFB4C0C8)) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3B7770),
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color(0xFF202C33),
                    unfocusedContainerColor = Color(0xFF202C33),
                    focusedTextColor = Color(0xFFE9EDEF),
                    unfocusedTextColor = Color(0xFFE9EDEF)
                )
            )

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Unread", "Groups").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF174F48),
                            selectedLabelColor = Color(0xFFE9EDEF),
                            labelColor = Color(0xFFD4DDE2)
                        )
                    )
                }
            }

            LazyColumn(Modifier.weight(1f)) {
                items(filtered, key = { it.name }) { chat ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { onOpenChat(chat) }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(52.dp).clip(CircleShape).background(chat.color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(chat.initials, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    chat.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE9EDEF),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(chat.time, fontSize = 11.sp, color = Color(0xFF9BAAB3))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                chat.message,
                                fontSize = 13.sp,
                                color = Color(0xFFB0BEC5),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFF26343C), thickness = 0.7.dp)
                }
            }

            Row(
                Modifier.fillMaxWidth().background(Color(0xFF101B22)).navigationBarsPadding().padding(top = 8.dp, bottom = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Chats", "Updates", "People", "Calls").forEachIndexed { index, label ->
                    Column(
                        Modifier.weight(1f).clickable { }.padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when (index) {
                                0 -> Icons.Default.Message
                                1 -> Icons.Default.Search
                                2 -> Icons.Default.Lock
                                else -> Icons.Default.Message
                            },
                            contentDescription = label,
                            tint = if (index == 0) Color(0xFF6ED9B0) else Color(0xFF9BAAB3),
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(label, color = if (index == 0) Color(0xFF6ED9B0) else Color(0xFF9BAAB3), fontSize = 10.sp)
                    }
                }
            }
            Text(
                "DEMO MODE · Fictional local messages",
                color = Color(0xFF7F929D),
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth().background(Color(0xFF101B22)).padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ConversationScreen(chat: DemoChat, onBack: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = Color(0xFF0B141A)) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(DarkPanel).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextMain) }
                Box(Modifier.size(38.dp).clip(CircleShape).background(chat.color), contentAlignment = Alignment.Center) {
                    Text(chat.initials, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(chat.name, color = TextMain, fontWeight = FontWeight.SemiBold)
                    Text("demo conversation", color = TextMuted, fontSize = 11.sp)
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.Bottom) {
                chat.messages.forEach { (message, fromMe) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (fromMe) Arrangement.End else Arrangement.Start) {
                        Surface(
                            color = if (fromMe) Color(0xFF005C4B) else DarkPanel,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.widthIn(max = 300.dp).padding(vertical = 4.dp)
                        ) {
                            Text(message, color = TextMain, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
                        }
                    }
                }
            }
            Text(
                "Read-only demo. Messages are not sent.",
                color = TextMuted, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }
}
