package com.wenyan.app.feature.aiassistant

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.net.URISyntaxException

/**
 * 南师大文学院官网导师信息页面 URL。
 */
private const val MENTOR_INFO_URL = "https://wxy.njnu.edu.cn/szdw/jsfc.htm"

/**
 * 导师信息界面。
 *
 * 不内置导师数据，改为外链南师大文学院官网师资风采页面。
 * 点击"前往官网"按钮通过系统浏览器打开页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentorInfoScreen() {
    val context = LocalContext.current

    ExpressiveScaffold(
        topBar = {
            // 固定内容页，仅享受 Large 标题样式
            WenyanLargeTopAppBar(title = "导师信息")
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Spacing.xl),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    text = "导师信息请前往南京师范大学文学院官网查看",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                FilledTonalButton(
                    onClick = { openOfficialWebsite(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "前往官网")
                }
            }
        }
    }
}

/**
 * 通过系统浏览器打开南师大文学院官网导师信息页面。
 *
 * 错误处理：
 * - 无可用浏览器：捕获 ActivityNotFoundException，提示用户安装浏览器。
 * - URL 格式错误：捕获 URISyntaxException，提示链接异常。
 */
private fun openOfficialWebsite(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(MENTOR_INFO_URL)).apply {
            // 不与当前Activity栈关联，避免按返回键回到App内部
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // 设备上没有可处理 http 链接的浏览器应用
        Toast.makeText(
            context,
            "未找到可用的浏览器应用，请先安装浏览器",
            Toast.LENGTH_LONG,
        ).show()
    } catch (e: URISyntaxException) {
        // URL 格式异常（理论上不会发生，已硬编码合法链接）
        Toast.makeText(
            context,
            "链接格式异常，无法打开",
            Toast.LENGTH_LONG,
        ).show()
    }
}
