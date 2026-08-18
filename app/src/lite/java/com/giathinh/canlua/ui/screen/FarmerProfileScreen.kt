package com.giathinh.canlua.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.giathinh.canlua.ui.viewmodel.ProfileViewModel

@Composable
fun FarmerProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val profile by profileViewModel.profile.collectAsStateWithLifecycle(initialValue = null)
    val isGuest by profileViewModel.isGuestMode.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text(profile?.name?.takeIf { it.isNotBlank() } ?: "Khach offline", style = MaterialTheme.typography.headlineSmall) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Ho so cuc bo")
                    Text("Du lieu duoc luu tren thiet bi, khong can Firebase.")
                    profile?.phone?.takeIf { it.isNotBlank() }?.let { Text("So dien thoai: $it") }
                    profile?.region?.takeIf { it.isNotBlank() }?.let { Text("Khu vuc: $it") }
                    if (isGuest) Text("Che do khach offline")
                }
            }
        }
        item {
            Button(onClick = { navController.navigate("profile_setup") }, modifier = Modifier.fillMaxWidth()) {
                Text(if (profile == null) "Tao ho so" else "Cap nhat ho so")
            }
        }
    }
}
