package com.ibf.app.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.lang.ref.WeakReference
import java.text.DecimalFormat
import java.text.ParseException

class MoneyTextWatcher(
    editText: EditText,
    private val formatter: DecimalFormat
) : TextWatcher {

    private val editTextWeakReference: WeakReference<EditText> = WeakReference(editText)
    private var current = ""

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // Nada a fazer antes que o texto mude
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (s.toString() != current) {
            editTextWeakReference.get()?.removeTextChangedListener(this)

            val cleanString = s.toString().replace("[R$,.]".toRegex(), "")

            if (cleanString.isNotEmpty()) {
                val parsed: Double
                try {
                    parsed = cleanString.toDouble() / 100
                    val formatted = formatter.format(parsed)
                    current = formatted
                    editTextWeakReference.get()?.setText(formatted)
                    editTextWeakReference.get()?.setSelection(formatted.length)
                } catch (e: NumberFormatException) {
                    editTextWeakReference.get()?.setText("")
                    current = ""
                } catch (e: ParseException) {
                    editTextWeakReference.get()?.setText("")
                    current = ""
                }
            } else {
                editTextWeakReference.get()?.setText("")
                current = ""
            }

            editTextWeakReference.get()?.addTextChangedListener(this)
        }
    }

    override fun afterTextChanged(s: Editable?) {
        // Nada a fazer depois que o texto muda
    }
}