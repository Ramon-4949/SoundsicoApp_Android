package edu.ucne.soundsicoappandroid.presentation.admincreation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import edu.ucne.soundsicoappandroid.domain.validation.BulletinValidation

@Composable
fun AdminMessageComposer(
    subject: String,
    message: String,
    saving: Boolean,
    onSubjectChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onSubmit: () -> Unit,
    editing: Boolean = false
) {
    val subjectError = BulletinValidation.subjectError(subject)
    val messageError = BulletinValidation.messageError(message)
    val showSubjectError = subject.isNotEmpty() && subjectError != null
    val showMessageError = message.isNotEmpty() && messageError != null
    val messageFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("ASUNTO DEL COMUNICADO", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = subject,
            onValueChange = onSubjectChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving,
            label = { Text("Asunto") },
            leadingIcon = { Icon(Icons.Outlined.Campaign, null, tint = MaterialTheme.colorScheme.primary) },
            singleLine = true,
            isError = showSubjectError,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { messageFocus.requestFocus() }),
            supportingText = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (showSubjectError) subjectError.orEmpty() else "", Modifier.weight(1f))
                    Text("${subject.length} / ${BulletinValidation.SUBJECT_LIMIT}")
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
        Text("MENSAJE / INSTRUCCIONES", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 190.dp).focusRequester(messageFocus),
            enabled = !saving,
            label = { Text("Mensaje") },
            placeholder = { Text("Escribe el comunicado para los colaboradores") },
            minLines = 8,
            maxLines = 20,
            isError = showMessageError,
            supportingText = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (showMessageError) messageError.orEmpty() else "", Modifier.weight(1f))
                    Text("${message.length} / ${BulletinValidation.MESSAGE_LIMIT}")
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
        Text("El comunicado será visible para toda la plantilla y no admite respuestas.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = { keyboard?.hide(); onSubmit() },
            enabled = !saving && subjectError == null && messageError == null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (saving) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Guardando…")
            } else {
                Icon(Icons.AutoMirrored.Outlined.Send, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (editing) "Guardar cambios" else "Publicar comunicado")
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
