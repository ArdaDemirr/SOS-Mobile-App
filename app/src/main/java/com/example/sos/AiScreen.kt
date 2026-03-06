package com.example.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

// Data Model for the Search Result
data class SurvivalChunk(
    val topic: String,
    val source: String,
    val content: String
)

@Composable
fun AiScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // --- STATE ---
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<SurvivalChunk>()) }
    var isSearching by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("AWAITING QUERY...") }

    // --- THE SEARCH ENGINE ---
    fun performSearch(searchText: String) {
        if (searchText.length < 3) {
            statusMessage = "QUERY TOO SHORT"
            return
        }

        isSearching = true
        statusMessage = "ACCESSING DATABASE..."
        focusManager.clearFocus() // Hide keyboard

        scope.launch(Dispatchers.IO) { // Run on background thread
            val foundItems = mutableListOf<SurvivalChunk>()
            try {
                // 1. Read the JSON file from Assets
                val inputStream = context.assets.open("survival_data.json")
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonString = reader.readText()
                reader.close()

                // 2. Parse JSON
                val jsonArray = JSONArray(jsonString)
                val totalChunks = jsonArray.length()

                // 3. Search Loop (Brute Force - Simple & Reliable)
                val lowerQuery = searchText.lowercase()

                for (i in 0 until totalChunks) {
                    val obj = jsonArray.getJSONObject(i)
                    val content = obj.getString("content")
                    val keywords = obj.getJSONArray("keywords").toString()
                    val topic = obj.getString("topic")

                    // Search Condition: Content OR Keywords match
                    if (content.lowercase().contains(lowerQuery) || keywords.lowercase().contains(lowerQuery) || topic.lowercase().contains(lowerQuery)) {
                        foundItems.add(
                            SurvivalChunk(
                                topic = topic,
                                source = "Page ${obj.getInt("page")} (${obj.getString("source")})",
                                content = content
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 4. Update UI
            withContext(Dispatchers.Main) {
                results = foundItems
                isSearching = false
                statusMessage = if (foundItems.isEmpty()) "NO MATCHES FOUND." else "FOUND ${foundItems.size} ENTRIES."
            }
        }
    }

    // --- UI LAYOUT ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PipBlack)
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(50.dp).background(PipAmber, RoundedCornerShape(8.dp)).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PipBlack)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("PROJECT ORACLE", color = PipAmber, fontSize = 24.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text("OFFLINE KNOWLEDGE BASE", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
        HorizontalDivider(color = PipAmber, thickness = 2.dp, modifier = Modifier.padding(vertical = 12.dp))

        // SEARCH BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .border(2.dp, PipGreen, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PipGreen, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Search, null, tint = PipGreen)
            }
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = TextStyle(color = PipGreen, fontFamily = FontFamily.Monospace, fontSize = 16.sp),
                modifier = Modifier.weight(1f),
                singleLine = true,
                cursorBrush = SolidColor(PipGreen),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { performSearch(query) }),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) Text("ENTER KEYWORDS (e.g. WATER)...", color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                    innerTextField()
                },
                enabled = !isSearching
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("STATUS: $statusMessage", color = if(statusMessage.contains("NO")) PipRed else Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(10.dp))

        // RESULTS LIST
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(results) { chunk ->
                SearchResultCard(chunk)
            }
        }
    }
}

@Composable
fun SearchResultCard(chunk: SurvivalChunk) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PipAmber.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .background(PipAmber.copy(alpha = 0.05f))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(chunk.topic.uppercase(), color = PipAmber, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(chunk.source, color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = chunk.content,
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 20.sp
            )
        }
    }
}