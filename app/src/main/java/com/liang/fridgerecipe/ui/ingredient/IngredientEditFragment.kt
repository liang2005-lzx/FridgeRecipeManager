package com.liang.fridgerecipe.ui.ingredient

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.liang.fridgerecipe.R
import com.liang.fridgerecipe.databinding.FragmentIngredientEditBinding
import com.liang.fridgerecipe.model.FoodCategory
import com.liang.fridgerecipe.model.Ingredient
import com.liang.fridgerecipe.util.DateUtils
import kotlinx.coroutines.launch

/**
 * 界面 3：添加 / 编辑食材。表单校验 + 日期选择 + 保质期快捷设置。
 */
class IngredientEditFragment : Fragment(R.layout.fragment_ingredient_edit) {

    private var _binding: FragmentIngredientEditBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IngredientEditViewModel by viewModels()

    private var editingId = -1L
    private var selectedCategory = FoodCategory.VEGETABLE
    private var addedAt = DateUtils.todayStart()
    private var expiresAt = DateUtils.addDays(DateUtils.todayStart(), 7)
    private var formReady = false

    private val unitOptions = listOf("份", "个", "袋", "盒", "瓶", "块", "克", "斤")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentIngredientEditBinding.bind(view)
        editingId = arguments?.getLong("ingredientId") ?: -1L

        binding.tvTitle.text = if (editingId > 0L) "编辑食材" else "添加食材"
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        setupCategoryDropdown()
        setupUnitDropdown()
        setupDatePickers()
        setupShelfChips()
        binding.btnSave.setOnClickListener { collectAndSave() }

        renderDates()
        viewModel.load(editingId)
        collectState()
    }

    private fun setupCategoryDropdown() {
        val options = FoodCategory.entries.map { "${it.emoji} ${it.label}" }
        binding.etCategory.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options)
        )
        binding.etCategory.setText("${selectedCategory.emoji} ${selectedCategory.label}", false)
        binding.etCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = FoodCategory.entries[position]
            // 选了新分类就按该分类的参考保质期自动填到期日，用户仍可手改
            expiresAt = DateUtils.addDays(addedAt, selectedCategory.defaultShelfLifeDays)
            renderDates()
        }
    }

    private fun setupUnitDropdown() {
        binding.etUnit.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, unitOptions)
        )
        binding.etUnit.setText(unitOptions.first(), false)
    }

    private fun setupDatePickers() {
        binding.etAdded.setOnClickListener {
            pickDate(addedAt) { picked ->
                addedAt = picked
                if (expiresAt < addedAt) expiresAt = DateUtils.addDays(addedAt, 7)
                renderDates()
            }
        }
        binding.etExpires.setOnClickListener {
            pickDate(expiresAt) { picked ->
                expiresAt = picked
                binding.tilExpires.error = null
                renderDates()
            }
        }
    }

    private fun pickDate(current: Long, onPicked: (Long) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("选择日期")
            .setSelection(DateUtils.toPickerMillis(current))
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            onPicked(DateUtils.fromPickerMillis(selection))
        }
        picker.show(childFragmentManager, "date_picker")
    }

    private fun setupShelfChips() {
        binding.chipShelf3.setOnClickListener { applyShelf(3) }
        binding.chipShelf7.setOnClickListener { applyShelf(7) }
        binding.chipShelf30.setOnClickListener { applyShelf(30) }
        binding.chipShelf90.setOnClickListener { applyShelf(90) }
    }

    private fun applyShelf(days: Int) {
        expiresAt = DateUtils.addDays(addedAt, days)
        binding.tilExpires.error = null
        renderDates()
    }

    private fun renderDates() {
        binding.etAdded.setText(DateUtils.toDayText(addedAt))
        binding.etExpires.setText(DateUtils.toDayText(expiresAt))
    }

    private fun collectState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.loadFinished.collect { finished ->
                        val item = viewModel.editing.value
                        if (finished && item != null && !formReady) {
                            fillForm(item)
                        }
                    }
                }
                launch {
                    viewModel.saveState.collect { state ->
                        when (state) {
                            is EditState.Saving -> {
                                binding.btnSave.isEnabled = false
                                binding.btnSave.text = "保存中…"
                            }

                            is EditState.Saved -> {
                                binding.btnSave.isEnabled = true
                                binding.btnSave.text = "保存"
                                Toast.makeText(requireContext(), "已保存", Toast.LENGTH_SHORT).show()
                                viewModel.resetSaveState()
                                findNavController().navigateUp()
                            }

                            is EditState.Error -> {
                                binding.btnSave.isEnabled = true
                                binding.btnSave.text = "保存"
                                binding.tvHint.text = state.message
                                binding.tvHint.isVisible = true
                                viewModel.resetSaveState()
                            }

                            EditState.Idle -> {
                                binding.btnSave.isEnabled = true
                                binding.btnSave.text = "保存"
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fillForm(item: Ingredient) {
        formReady = true
        selectedCategory = item.category
        addedAt = item.addedAt
        expiresAt = item.expiresAt

        binding.etName.setText(item.name)
        binding.etCategory.setText("${item.category.emoji} ${item.category.label}", false)
        binding.etQuantity.setText(item.quantityText)
        binding.etUnit.setText(item.unit, false)
        binding.etNote.setText(item.note)
        renderDates()
    }

    private fun collectAndSave() {
        binding.tilName.error = null
        binding.tilQuantity.error = null
        binding.tilExpires.error = null

        val name = binding.etName.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            binding.tilName.error = "请填写食材名称"
            return
        }
        if (name.length > 20) {
            binding.tilName.error = "名称不要超过 20 个字"
            return
        }

        val quantity = binding.etQuantity.text?.toString()?.trim()?.toDoubleOrNull()
        if (quantity == null || quantity <= 0.0) {
            binding.tilQuantity.error = "请输入大于 0 的数量"
            return
        }

        if (expiresAt < addedAt) {
            binding.tilExpires.error = "到期日期不能早于购买日期"
            return
        }

        val unit = binding.etUnit.text?.toString()?.trim().orEmpty().ifEmpty { "份" }
        val note = binding.etNote.text?.toString()?.trim().orEmpty()

        viewModel.save(
            Ingredient(
                id = if (editingId > 0L) editingId else 0L,
                name = name,
                category = selectedCategory,
                quantity = quantity,
                unit = unit,
                addedAt = DateUtils.startOfDay(addedAt),
                expiresAt = DateUtils.startOfDay(expiresAt),
                note = note
            )
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
