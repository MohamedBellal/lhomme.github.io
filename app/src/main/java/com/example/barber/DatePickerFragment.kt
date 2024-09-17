// DatePickerFragment.kt
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import com.example.barber.R

class DatePickerFragment(private val onDateSet: (String) -> Unit) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_date_picker, container, false)

        val datePicker = view.findViewById<DatePicker>(R.id.datePicker)
        val buttonConfirm = view.findViewById<Button>(R.id.buttonConfirmDate)

        buttonConfirm.setOnClickListener {
            val year = datePicker.year
            val month = datePicker.month + 1
            val day = datePicker.dayOfMonth
            val formattedDate = String.format("%04d-%02d-%02d", year, month, day)
            onDateSet(formattedDate)
            dismiss()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
