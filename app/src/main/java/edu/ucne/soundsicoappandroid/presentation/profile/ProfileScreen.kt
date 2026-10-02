package edu.ucne.soundsicoappandroid.presentation.profile

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucne.soundsicoappandroid.domain.model.EmployeeProfile

@Composable
fun ProfileScreen(
    profile: EmployeeProfile,
    email: String,
    signingOut: Boolean,
    onOpenDashboard: () -> Unit,
    onOpenPerformance: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Mi Perfil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Surface(Modifier.size(82.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AccountCircle, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outline)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(28.dp))
        ProfileSectionLabel("GESTIÓN E INFORMACIÓN")
        Spacer(Modifier.height(8.dp))
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column {
                ProfileRow(Icons.Outlined.MailOutline, "Correo corporativo", email)
                HorizontalDivider(Modifier.padding(start = 50.dp), color = MaterialTheme.colorScheme.outlineVariant)
                ProfileRow(Icons.Outlined.Phone, "Número de teléfono", profile.phone.formatPhone())
                HorizontalDivider(Modifier.padding(start = 50.dp), color = MaterialTheme.colorScheme.outlineVariant)
                ProfileRow(Icons.Outlined.Badge, "Cargo", profile.position.ifBlank { "Sin registrar" })
                if (profile.isAdministrator) {
                    HorizontalDivider(Modifier.padding(start = 50.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileActionRow(Icons.Outlined.Dashboard, "Panel de control", onOpenDashboard)
                    HorizontalDivider(Modifier.padding(start = 50.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileActionRow(Icons.Outlined.BarChart, "Rendimiento", onOpenPerformance)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        ProfileSectionLabel("PREFERENCIAS Y SEGURIDAD")
        Spacer(Modifier.height(8.dp))
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            ProfileRow(
                Icons.Outlined.Language,
                "Idioma de interfaz",
                languageLabel(),
                trailing = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(18.dp)) },
                onClick = {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.parse("package:${context.packageName}"))
                    } else {
                        Intent(Settings.ACTION_LOCALE_SETTINGS)
                    }
                    context.startActivity(intent)
                }
            )
        }
        Spacer(Modifier.height(28.dp))
        OutlinedButton(
            onSignOut,
            Modifier.fillMaxWidth().height(50.dp),
            enabled = !signingOut,
            border = BorderStroke(0.dp, MaterialTheme.colorScheme.surfaceVariant),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (signingOut) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp)
            else {
                Icon(Icons.AutoMirrored.Outlined.ExitToApp, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ProfileSectionLabel(value: String) {
    Text(
        value,
        Modifier.fillMaxWidth().padding(start = 4.dp),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.4.sp
    )
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    label: String,
    value: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    Row(modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        ProfileIcon(icon)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        trailing?.invoke()
    }
}

@Composable
private fun ProfileActionRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 54.dp).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileIcon(icon)
        Spacer(Modifier.width(10.dp))
        Text(title, Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(18.dp))
    }
}

@Composable
private fun ProfileIcon(icon: ImageVector) {
    Surface(Modifier.size(34.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun languageLabel() = "Español (DO)"

private fun String.formatPhone(): String {
    val digits = filter(Char::isDigit).takeLast(10)
    if (digits.length != 10) return ifBlank { "Sin registrar" }
    return "+1 (${digits.take(3)}) ${digits.substring(3, 6)}-${digits.takeLast(4)}"
}
