package com.MohammadNoorAbuAsbe.myruppin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.MohammadNoorAbuAsbe.myruppin.data.TokenManager
import com.MohammadNoorAbuAsbe.myruppin.data.repository.GradesRepository

class GradesViewModelFactory(
    private val repository: GradesRepository,
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GradesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GradesViewModel(repository, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}