package com.giathinh.canlua.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.R

/**
 * Dropdown chọn giống lúa — hỗ trợ chọn từ danh sách phổ biến
 * hoặc tự nhập tên giống lúa mới.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiceVarietyDropdown(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    suggestions: List<String> = com.giathinh.canlua.util.RiceVarieties.popular.take(5),
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(14.dp),
    colors: androidx.compose.material3.TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    ),
    fillMaxHeight: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = { onSelect(it) },
            label = { Text(stringResource(R.string.detail_info_rice_variety)) },
            placeholder = { Text(stringResource(R.string.dropdown_rice_variety_placeholder)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .then(if (fillMaxHeight) Modifier.fillMaxHeight() else Modifier.heightIn(min = 60.dp)),
            shape = shape,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = colors
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            suggestions.forEach { variety ->
                DropdownMenuItem(
                    text = {
                        Text(variety, style = MaterialTheme.typography.bodyLarge)
                    },
                    onClick = {
                        onSelect(variety)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}
