package com.example.navipilot.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import com.example.navipilot.R
import com.example.navipilot.core.SecurePrefs
import com.example.navipilot.ui.utils.localized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 用户数据模型
 */
data class UserData(
    val deviceId: String,
    val sponsorAmount: Float = 0f,
    val userType: Int = 4,  // 默认为铁粉权限（已移除赞助检查）
    val email: String = "",
    val contactName: String = ""  // 微信名/Discord用户名
)

/** 登录状态持久化 Key */
private const val AUTH_PREFS = "auth_config"
private const val KEY_LOGIN_EMAIL = "login_email"
private const val KEY_AUTH_TOKEN = "auth_token"
private const val LEGACY_KEY_LOGIN_METHOD = "login_method"
private const val LEGACY_KEY_GITHUB_NAME = "github_name"
private const val MAP_ADDRESS_PREFS = "map_addresses"
private const val KEY_PLATE_NUMBER = "plate_number"

/**
 * 我的页面组件
 */
@Composable
fun ProfilePage(deviceId: String) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // 登录状态
    val authPrefs = remember { context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE) }
    val securePrefs = remember { SecurePrefs.get(context) }
    val addressPrefs = remember { context.getSharedPreferences(MAP_ADDRESS_PREFS, Context.MODE_PRIVATE) }
    var loginEmail by remember { mutableStateOf(authPrefs.getString(KEY_LOGIN_EMAIL, "") ?: "") }
    val isLoggedIn = loginEmail.isNotBlank()
    
    // 用户数据状态
    var userData by remember { mutableStateOf<UserData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showUserForm by remember { mutableStateOf(false) }
    
    // 二维码弹窗状态（独立于编辑表单）
    var showQrCodeDialog by remember { mutableStateOf(false) }
    
    // 表单状态
    var sponsorAmount by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var plateNumber by remember { mutableStateOf(addressPrefs.getString(KEY_PLATE_NUMBER, "") ?: "") }
    var plateSaved by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    
    // 登录成功回调
    val onLoginSuccess: (String) -> Unit = { email ->
        loginEmail = email
        authPrefs.edit()
            .putString(KEY_LOGIN_EMAIL, email)
            .remove(LEGACY_KEY_LOGIN_METHOD)
            .remove(LEGACY_KEY_GITHUB_NAME)
            .apply()
    }
    
    val onLogout: () -> Unit = {
        loginEmail = ""
        authPrefs.edit()
            .remove(KEY_LOGIN_EMAIL)
            .remove(LEGACY_KEY_LOGIN_METHOD)
            .remove(LEGACY_KEY_GITHUB_NAME)
            .apply()
        // 清除安全存储中的 auth_token
        securePrefs.edit().remove(KEY_AUTH_TOKEN).apply()
    }
    
    // 重试触发器
    var retryTrigger by remember { mutableIntStateOf(0) }
    
    // 获取用户数据 - 已移除赞助检查，直接本地返回数据
    LaunchedEffect(deviceId, retryTrigger) {
        android.util.Log.i("ProfilePage", "✅ 赞助功能已禁用 - 所有用户获得完全访问权限")
        userData = UserData(
            deviceId = deviceId,
            userType = 4,  // 直接设为铁粉权限
            sponsorAmount = 0f
        )
        isLoading = false
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 重要提醒卡片 (已禁用)
        // ImportantNoticeCard()
        
        // 登录卡片
        LoginCard(
            deviceId = deviceId,
            isLoggedIn = isLoggedIn,
            loginEmail = loginEmail,
            onLoginSuccess = onLoginSuccess,
            onLogout = onLogout
        )
        
        // 用户信息卡片
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = localized("用户信息", "User Info"),
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = localized("用户信息", "User Info"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
                
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF3B82F6))
                        }
                    }
                    errorMessage != null -> {
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(
                                text = errorMessage!!,
                                color = if (errorMessage!!.contains("⚠️")) Color(0xFFD97706) else Color.Red,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    errorMessage = null
                                    isLoading = true
                                    retryTrigger++
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(localized("重试", "Retry"), fontSize = 13.sp)
                            }
                        }
                    }
                    userData != null -> {
                        UserInfoDisplay(
                            userData = userData!!,
                            plateNumber = plateNumber,
                            isLoggedIn = isLoggedIn,
                            onEditClick = { 
                                showUserForm = true
                                showQrCodeDialog = false  // 不再显示赞助二维码
                            }
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = Color(0xFFE2E8F0)
                )

                PlateNumberSection(
                    plateNumber = plateNumber,
                    onPlateNumberChange = {
                        plateNumber = it
                        plateSaved = false
                    },
                    onSave = {
                        addressPrefs.edit()
                            .putString(KEY_PLATE_NUMBER, plateNumber.trim())
                            .apply()
                        plateSaved = true
                    },
                    saved = plateSaved
                )
            }
        }
        
        // 用户信息编辑表单（已移除赞助字段）
        if (showUserForm && userData != null) {
            SimpleUserFormCard(
                contactName = contactName,
                onContactNameChange = { contactName = it },
                isUpdating = isUpdating,
                onSave = { isUpdating = true },
                onCancel = { 
                    showUserForm = false
                }
            )
        }
    }
}

/**
 * 用户信息显示组件
 */
@Composable
private fun UserInfoDisplay(
    userData: UserData,
    plateNumber: String,
    isLoggedIn: Boolean,
    onEditClick: () -> Unit
) {
    Column {
        InfoRow(localized("设备ID", "Device ID"), userData.deviceId)
        
        val userTypeText = localized("铁粉", "Super Fan")  // 所有用户都是铁粉
        InfoRow(localized("用户类型", "User Type"), userTypeText)
        
        InfoRow(localized("邮箱", "Email"), userData.email.ifEmpty { localized("未绑定", "Not bound") })
        
        InfoRow(
            localized("微信/Discord", "WeChat/Discord"),
            userData.contactName.ifEmpty { localized("未填写", "Not set") }
        )

        InfoRow(
            localized("车牌号", "License Plate"),
            plateNumber.ifEmpty { localized("未填写", "Not set") }
        )
        
        // 移除赞助金额显示
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onEditClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = isLoggedIn,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6),
                disabledContainerColor = Color(0xFFCBD5E1)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = localized("编辑", "Edit"),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(localized("编辑用户信息", "Edit User Info"))
        }
        
        if (!isLoggedIn) {
            Text(
                text = localized("请先登录绑定邮箱后才能编辑", "Please login and bind email first"),
                fontSize = 11.sp,
                color = Color(0xFFEF4444),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * 信息行组件
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF64748B)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E293B)
        )
    }
}

/**
 * 简化的用户信息编辑表单卡片（已移除赞助）
 */
@Composable
private fun SimpleUserFormCard(
    contactName: String,
    onContactNameChange: (String) -> Unit,
    isUpdating: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = localized("编辑用户信息", "Edit User Info"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            OutlinedTextField(
                value = contactName,
                onValueChange = { onContactNameChange(it.take(30)) },
                label = { Text(localized("微信名称", "Discord Username")) },
                placeholder = { Text(localized("请输入微信昵称", "Enter Discord username")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    Text(
                        localized("用于标识用户信息", "User identification info"),
                        color = Color(0xFF64748B)
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF94A3B8))
                ) {
                    Text(localized("取消", "Cancel"))
                }
                
                Button(
                    onClick = { onSave() },
                    modifier = Modifier.weight(1f),
                    enabled = !isUpdating,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White
                        )
                    } else {
                        Text(localized("保存", "Save"))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlateNumberSection(
    plateNumber: String,
    onPlateNumberChange: (String) -> Unit,
    onSave: () -> Unit,
    saved: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = localized("车牌号设置", "License Plate"),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B)
        )

        OutlinedTextField(
            value = plateNumber,
            onValueChange = { onPlateNumberChange(it.take(10).uppercase()) },
            label = { Text(localized("车牌号（限行避开）", "License Plate")) },
            placeholder = { Text(localized("如 京A12345", "e.g. 京A12345")) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
            supportingText = {
                Text(
                    localized("填写后，腾讯导航算路将自动避开限行区域", "Tencent Nav will avoid traffic restriction zones when set"),
                    color = Color(0xFF64748B)
                )
            }
        )

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(localized("保存车牌号", "Save License Plate"), fontSize = 13.sp)
        }

        if (saved) {
            Text(
                text = localized("已保存", "Saved"),
                fontSize = 11.sp,
                color = Color(0xFF10B981)
            )
        }
    }
}

/**
 * 检查是否有互联网连接（非局域网）
 */
private fun hasInternetConnection(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    // 必须有 INTERNET 能力，且验证过可达（排除仅局域网的WiFi热点）
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
           caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
