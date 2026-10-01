package edu.ucne.soundsicoappandroid.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import edu.ucne.soundsicoappandroid.R

@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(22.dp), modifier = modifier.size(102.dp)) {
        Image(painterResource(R.drawable.brand_logo), "SounDisco", Modifier.padding(11.dp))
    }
}

@Composable
fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    error: String? = null,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            enabled = enabled,
            singleLine = true,
            isError = error != null,
            shape = RoundedCornerShape(8.dp),
            visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (password) KeyboardType.Password else keyboardType,
                imeAction = imeAction,
                autoCorrectEnabled = !password && keyboardType == KeyboardType.Text
            ),
            keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
            trailingIcon = if (password) {
                {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            if (visible) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                }
            } else null
        )
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun PrimaryAction(label: String, busy: Boolean, onClick: () -> Unit) {
    Button(onClick, enabled = !busy, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
        }
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun FailureDialog(message: String?, onDismiss: () -> Unit) {
    if (message != null) AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("No se pudo completar") },
        text = { Text(message) },
        confirmButton = { TextButton(onDismiss) { Text("Entendido") } }
    )
}
