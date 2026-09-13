package com.giathinh.canlua.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.giathinh.canlua.R
import com.giathinh.canlua.ui.theme.AppColors
import com.giathinh.canlua.util.SeasonOption
import com.giathinh.canlua.util.SeasonUtil
import java.util.Date

/**
 * Dropdown chọn vụ mùa — hỗ trợ gợi ý theo lịch nông vụ miền Tây
 * và cho phép nhập tùy biến (PrimaryEditable).
 * Hỗ trợ đa ngôn ngữ qua [SeasonOption.nameRes].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonDropdown(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    date: Date = remember { Date() },
    options: List<SeasonOption> = remember(date.time / 86400000L) {
        SeasonUtil.currentSeasonOptions(date)
    },
    shape: Shape = RoundedCornerShape(14.dp)
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {
                onSelect(it)
                expanded = true
            },
            label = { Text(stringResource(R.string.weight_lot_season_label)) },
            placeholder = { Text(stringResource(R.string.dropdown_season_placeholder)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .fillMaxWidth(),
            shape = shape,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.GreenPrimary,
                unfocusedBorderColor = AppColors.Divider,
                focusedLabelColor = AppColors.GreenPrimary,
                unfocusedLabelColor = AppColors.TextSecondary,
                cursorColor = AppColors.GreenPrimary,
                focusedTrailingIconColor = AppColors.GreenPrimary,
                unfocusedTrailingIconColor = AppColors.TextSecondary
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { seasonOption ->
                val seasonLabel = "${stringResource(seasonOption.nameRes)} ${seasonOption.year}"
                DropdownMenuItem(
                    text = { Text(seasonLabel, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        onSelect(seasonLabel)
                        expanded = false
                    }
                )
            }
        }
    }
}
