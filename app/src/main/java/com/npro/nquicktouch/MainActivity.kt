package com.npro.nquicktouch

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import DataItem
import android.app.Activity.RESULT_OK
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.npro.nquicktouch.ui.theme.NQuickTouchTheme
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    companion object {
        var ipAddress: String = "http://127.0.0.1:41234" // 设置默认 IP 地址
    }

    private lateinit var qrcodeScanner: ActivityResultLauncher<ScanOptions>
    private val dataItemsState = mutableStateOf(listOf<DataItem>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置默认横屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        qrcodeScanner = registerForActivityResult(ScanContract()) { result ->
            result?.contents?.let { contents ->
                try {
                    val jsonObject = Json.parseToJsonElement(contents).jsonObject
                    ipAddress = jsonObject["ip"]?.jsonPrimitive?.content ?: "http://127.0.0.1:41234"
                    Toast.makeText(this, "连接: $ipAddress", Toast.LENGTH_LONG).show()
                    if (ipAddress.isNotEmpty()) {
                        val client = OkHttpClient()
                        val request = Request.Builder()
                            .url("$ipAddress/config")
                            .build()
                        thread {
                            try {
                                val response: Response = client.newCall(request).execute()
                                if (response.isSuccessful) {
                                    val responseData = response.body?.string()
                                    if (responseData != null){
                                        val rJson = Json.parseToJsonElement(responseData).jsonObject
                                        val data = rJson["data"]
                                        //将data赋值给dataItems
                                        if (data != null) {
                                            val dataArray = data.jsonArray
                                            var dataItemsx: List<DataItem> = listOf()
                                            dataItemsx = dataArray.map { item ->
                                                val path = item.jsonObject["path"]?.jsonPrimitive?.content ?: ""
                                                val icon = item.jsonObject["icon"]?.jsonPrimitive?.content ?: ""
                                                val name = item.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                                                DataItem(path, icon, name)
                                           }
                                            if (dataItemsx.isNotEmpty()) {
                                                dataItemsState.value = dataItemsx
                                            }
                                       }
                                    }
                                } else {
                                    Toast.makeText(this, "同步失败!", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "连接无效", Toast.LENGTH_LONG).show()
                }


            }
        }

        enableEdgeToEdge()
        setContent {
            NQuickTouchTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainContent(
                        modifier = Modifier.padding(innerPadding),
                        onQRCodeIconClick = { launchQRCodeScanner() },
                        dataItems = dataItemsState.value
                    )
                }
            }
        }
    }

    private fun launchQRCodeScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan a QR code")
        options.setCameraId(0) // Use a specific camera of the device
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        qrcodeScanner.launch(options)
    }
}

@Composable
fun MainContent(modifier: Modifier = Modifier, onQRCodeIconClick: () -> Unit, dataItems: List<DataItem>) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_gear), // 请确保你有一个 ic_gear 的图标资源
                contentDescription = "Settings",
                tint = Color.Black,
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.TopEnd)
                    .clickable {
                        onQRCodeIconClick()
                    }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(8f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val (counter, setCounter) = remember { mutableStateOf(0) }
            // 当数据更改时，手动增加计数器来触发重新渲染
            setCounter(counter + 1)
            GridContent(dataItems)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Bottom Area (10%)")
        }
    }
}

@Composable
fun GridContent(items: List<DataItem>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(items.size) { index ->
            val item = items[index]
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(4.dp)
                    .clickable {
                        sendHttpRequest(index)
                    },
                contentAlignment = Alignment.Center
            ) {
                val imageBitmap = base64ToImageBitmap(item.icon)
                imageBitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "Item ${index}",
                        modifier = Modifier.size(50.dp) // 调整图片大小
                    )
                }
            }
        }
    }
}

fun sendHttpRequest(index: Int) {
    val client = OkHttpClient()
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val jsonBody = """{"data": $index}"""
    val requestBody = jsonBody.toRequestBody(mediaType)
    val request = Request.Builder()
        .url("${MainActivity.ipAddress}/shell") // 替换为你的服务器地址
        .post(requestBody)
        .build()
    thread {
        try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                println("Request successful: ${response.body?.string()}")
            } else {
                println("Request failed: ${response.code}")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

fun base64ToImageBitmap(base64String: String): ImageBitmap? {
    return try {
        var fstr=""
        if(base64String!=""&&base64String!=null){
            fstr = base64String.split(",")[1]
        }else{
            fstr = base64String
        }
        val imageBytes = Base64.decode(fstr, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Preview(showBackground = true)
@Composable
fun MainContentPreview() {
    NQuickTouchTheme {
        MainContent(onQRCodeIconClick = {}, dataItems = listOf())
    }
}
