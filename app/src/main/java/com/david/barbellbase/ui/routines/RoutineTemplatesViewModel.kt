package com.david.barbellbase.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.david.barbellbase.data.repo.RoutineTemplate
import com.david.barbellbase.data.repo.RoutineTemplateRepository
import com.david.barbellbase.data.repo.RoutineTemplates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutineTemplatesState(
    val templates: List<RoutineTemplate> = RoutineTemplates,
    val importingId: String? = null,
    val error: String? = null,
)

@HiltViewModel
class RoutineTemplatesViewModel @Inject constructor(
    private val repository: RoutineTemplateRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RoutineTemplatesState())
    val state = _state.asStateFlow()

    fun import(templateId: String, onImported: (String) -> Unit) {
        if (_state.value.importingId != null) return
        _state.update { it.copy(importingId = templateId, error = null) }
        viewModelScope.launch {
            repository.import(templateId)
                .onSuccess { routineId ->
                    _state.update { it.copy(importingId = null) }
                    onImported(routineId)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            importingId = null,
                            error = error.message ?: "Could not add the routine",
                        )
                    }
                }
        }
    }
}
