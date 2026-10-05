package de.wcinfo.app.ui.feedback

import android.app.Application
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import de.wcinfo.app.R
import de.wcinfo.app.data.model.Toilet
import de.wcinfo.app.data.repository.ToiletRepository
import de.wcinfo.app.ui.theme.PrimaryPurple
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedbackViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ToiletRepository()

    data class UiState(
        val toilet: Toilet? = null,
        val isLoadingToilet: Boolean = false,
        val isSubmitting: Boolean = false,
        val isSubmitted: Boolean = false,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun loadToilet(toiletId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingToilet = true) }
            repository.getToilet(toiletId)
                .onSuccess { toilet ->
                    _uiState.update { it.copy(toilet = toilet, isLoadingToilet = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingToilet = false) }
                }
        }
    }

    fun submitFeedback(toiletId: Int, subject: String, message: String?) {
        val trimmedSubject = subject.trim()
        if (trimmedSubject.isBlank()) return
        val trimmedMessage = message?.trim()?.takeIf { it.isNotEmpty() }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            repository.sendFeedback(toiletId, trimmedSubject, trimmedMessage)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false, isSubmitted = true) }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = err.localizedMessage ?: getApplication<Application>().getString(R.string.error_sending_feedback)
                        )
                    }
                }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackScreen(
    toiletId: Int,
    onBackClick: () -> Unit,
    viewModel: FeedbackViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val preselectableSubjects = listOf(
        stringResource(id = R.string.feedback_subject_not_exist),
        stringResource(id = R.string.feedback_subject_check),
        stringResource(id = R.string.feedback_subject_relocated),
        stringResource(id = R.string.feedback_subject_belongs_elsewhere)
    )

    LaunchedEffect(toiletId) {
        viewModel.loadToilet(toiletId)
    }

    val context = LocalContext.current
    val successMessage = stringResource(id = R.string.feedback_sent_success)

    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted) {
            Toast.makeText(context, successMessage, Toast.LENGTH_LONG).show()
            onBackClick()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Drag handle top indicator
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }

            // Top Header: Cancel pill button + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable { onBackClick() }
                ) {
                    Text(
                        text = stringResource(id = R.string.cancel),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = stringResource(id = R.string.feedback_header_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Toilet Info Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val toiletName = uiState.toilet?.name?.takeIf { it.isNotBlank() }
                    Text(
                        text = "WC #$toiletId" + if (toiletName != null) " - $toiletName" else "",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val owner = uiState.toilet?.owner?.takeIf { it.isNotBlank() }
                    if (owner != null) {
                        Text(
                            text = stringResource(id = R.string.operator_label, owner),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val address = uiState.toilet?.address?.takeIf { it.isNotBlank() }
                    if (address != null) {
                        Text(
                            text = address,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Betreff (Subject) Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.feedback_subject_header),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Preselectable Subject Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    preselectableSubjects.forEach { chipSubject ->
                        val isSelected = subject == chipSubject
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.clickable { subject = chipSubject }
                        ) {
                            Text(
                                text = chipSubject,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Subject Input Field
                TextField(
                    value = subject,
                    onValueChange = { subject = it },
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.feedback_subject_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            // Nachricht (Message) Section (Optional)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = R.string.feedback_message_header),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.optional_parentheses),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                TextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.feedback_message_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    minLines = 4
                )
            }

            // Error Message (if any)
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Submit Button
            Button(
                onClick = {
                    if (subject.isNotBlank()) {
                        viewModel.submitFeedback(toiletId, subject, message)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryPurple,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ),
                enabled = subject.isNotBlank() && !uiState.isSubmitting
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.feedback_submit_button),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
