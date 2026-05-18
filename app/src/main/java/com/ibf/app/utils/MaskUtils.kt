package com.ibf.app.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

object MaskUtils {
    fun applyMask(editText: EditText, mask: String): TextWatcher {
        return object : TextWatcher {
            private var isUpdating = false
            private var oldString = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) {
                    isUpdating = false
                    return
                }

                val str = unmask(s.toString())
                var formatted = ""
                var i = 0

                if (str.length > unmask(mask).length) {
                    isUpdating = true
                    editText.setText(oldString)
                    editText.setSelection(oldString.length)
                    return
                }

                for (m in mask.toCharArray()) {
                    if (m != '#' && str.length > i) {
                        formatted += m
                        continue
                    }
                    try {
                        formatted += str[i]
                    } catch (e: Exception) {
                        break
                    }
                    i++
                }

                isUpdating = true
                editText.setText(formatted)
                editText.setSelection(formatted.length)
            }

            override fun afterTextChanged(s: Editable?) {
                oldString = s.toString()
            }
        }
    }

    private fun unmask(s: String): String {
        return s.replace("[.]".toRegex(), "").replace("[-]".toRegex(), "")
            .replace("[/]".toRegex(), "").replace("[(]".toRegex(), "")
            .replace("[ ]".toRegex(), "").replace("[:]".toRegex(), "")
            .replace("[)]".toRegex(), "")
    }

    const val MASK_CPF = "###.###.###-##"
    const val MASK_DATE = "##/##/####"
    const val MASK_PHONE = "(##) #####-####"
}
