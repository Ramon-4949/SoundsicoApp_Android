package edu.ucne.soundsicoappandroid.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import edu.ucne.soundsicoappandroid.domain.model.SessionState
import edu.ucne.soundsicoappandroid.domain.usecase.ObserveSessionUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class SessionViewModel(observeSession: ObserveSessionUseCase) : ViewModel() {
    val state = observeSession().stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)
    private var identity: String? = null
    private var owner: ViewModelStoreOwner? = null

    fun scope(userIdentity: String): ViewModelStoreOwner {
        if (identity != userIdentity) {
            owner?.viewModelStore?.clear()
            identity = userIdentity
            owner = object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
        }
        return requireNotNull(owner)
    }

    override fun onCleared() {
        owner?.viewModelStore?.clear()
    }
}
