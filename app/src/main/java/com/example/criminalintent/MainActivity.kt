package com.example.criminalintent

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.content.Intent
import android.content.ActivityNotFoundException
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.ListAdapter
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.UUID

private val ink = Color.rgb(16, 24, 32)
private val panel = Color.rgb(27, 40, 50)
private val amber = Color.rgb(244, 191, 96)
private val white = Color.rgb(237, 242, 244)
private val muted = Color.rgb(169, 188, 198)
private fun View.dp(n: Int) = (n * resources.displayMetrics.density).toInt()
private fun dateLabel(date: Long) = DateFormat.getDateInstance(DateFormat.LONG).format(java.util.Date(date))
private fun LinearLayout.label(text: String, size: Float = 14f, color: Int = muted): TextView = TextView(context).also {
    it.text = text; it.textSize = size; it.setTextColor(color); it.setPadding(0, dp(8), 0, dp(8)); addView(it)
}
private fun LinearLayout.button(text: String, action: () -> Unit): Button = Button(context).also {
    it.text = text; it.setTextColor(ink); it.backgroundTintList = android.content.res.ColorStateList.valueOf(amber)
    addView(it, LinearLayout.LayoutParams(-1, -2)); it.setOnClickListener { action() }
}
private fun column(context: android.content.Context) = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL; setBackgroundColor(ink); setPadding(dp(20), dp(8), dp(20), dp(16))
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(ink) }
        val toolbar = Toolbar(this).apply { setBackgroundColor(panel); setTitleTextColor(amber); title = "CRIMINAL INTENT" }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, -2))
        val host = FrameLayout(this).apply { id = 1001 }
        root.addView(host, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root); setSupportActionBar(toolbar)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
        }
        if (savedInstanceState == null) supportFragmentManager.beginTransaction().replace(1001, CrimeListFragment()).commit()
        supportFragmentManager.addOnBackStackChangedListener { updateBar() }
        updateBar()
    }
    private fun updateBar() { supportActionBar?.setDisplayHomeAsUpEnabled(supportFragmentManager.backStackEntryCount > 0) }
    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
    fun openCase(id: String) {
        if (isFinishing || isDestroyed) return
        supportFragmentManager.beginTransaction().replace(1001, CrimeDetailFragment().apply { arguments = Bundle().apply { putString("id", id) } }).addToBackStack("case").commit()
    }
}

class CrimeListFragment : Fragment() {
    private val model: CaseViewModel by activityViewModels()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(object : androidx.core.view.MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                menu.add(0, 1, 0, "New case").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            }
            override fun onMenuItemSelected(item: MenuItem): Boolean {
                if (item.itemId != 1) return false
                model.add { if (isAdded) (activity as? MainActivity)?.openCase(it) }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val root = column(requireContext())
        root.label("INVESTIGATION BUREAU", 12f, amber)
        root.label("Case files", 30f, white)
        val summary = root.label("Loading case files…")
        val empty = root.label("No cases yet. Use + New case to open an investigation.", 17f, white)
        val list = RecyclerView(requireContext()).apply { layoutManager = LinearLayoutManager(context) }
        val adapter = CrimeAdapter { (requireActivity() as MainActivity).openCase(it.id) }
        list.adapter = adapter
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        root.button("+ New case") { model.add { if (isAdded) (activity as? MainActivity)?.openCase(it) } }
        viewLifecycleOwnerLiveData.observe(this) { owner ->
            if (owner == null) return@observe
            viewLifecycleOwnerLiveData.removeObservers(this)
            owner.lifecycleScope.launch { owner.repeatOnLifecycle(Lifecycle.State.STARTED) { model.crimes.collect { crimes ->
                adapter.submitList(crimes)
                summary.text = "${crimes.size} CASES  •  ${crimes.count { !it.solved }} OPEN"
                empty.visibility = if (crimes.isEmpty()) View.VISIBLE else View.GONE
            } } }
        }
        return root
    }
}

private class CrimeAdapter(val open: (Crime) -> Unit) : ListAdapter<Crime, CrimeAdapter.Holder>(object : DiffUtil.ItemCallback<Crime>() {
    override fun areItemsTheSame(a: Crime, b: Crime) = a.id == b.id
    override fun areContentsTheSame(a: Crime, b: Crime) = a == b
}) {
    class Holder(val card: LinearLayout, val title: TextView, val date: TextView, val status: TextView) : RecyclerView.ViewHolder(card)
    override fun onCreateViewHolder(parent: ViewGroup, type: Int): Holder {
        val card = column(parent.context).apply {
            setBackgroundColor(panel)
            layoutParams = RecyclerView.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
            minimumHeight = dp(110)
        }
        return Holder(card, card.label("", 21f, white), card.label(""), card.label("", 12f, amber))
    }
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val crime = getItem(position)
        holder.title.text = crime.title.ifBlank { "Untitled case" }
        holder.date.text = dateLabel(crime.date)
        holder.status.text = if (crime.solved) "✓ SOLVED  •  FILE CLOSED" else "● OPEN  •  UNDER INVESTIGATION"
        holder.status.setTextColor(if (crime.solved) Color.rgb(115, 212, 180) else amber)
        holder.card.setOnClickListener { open(crime) }
    }
}

class CrimeDetailFragment : Fragment() {
    private val model: CaseViewModel by activityViewModels()
    private val caseId get() = requireArguments().getString("id")!!
    private var current: Crime? = null
    private var pendingPhoto: String? = null
    private var evidenceView: ImageView? = null
    private var shownPhoto: String? = null
    private val camera = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        pendingPhoto?.let { name ->
            val file = File(requireContext().filesDir, "evidence/$name")
            if (success && file.length() > 0) model.photo(caseId, name) else file.delete()
        }
        pendingPhoto = null
    }
    override fun onCreate(state: Bundle?) { super.onCreate(state); pendingPhoto = state?.getString("pendingPhoto") }
    override fun onSaveInstanceState(out: Bundle) { super.onSaveInstanceState(out); out.putString("pendingPhoto", pendingPhoto) }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        var binding = false
        var initialized = false
        val scroll = ScrollView(requireContext())
        val root = column(requireContext()); scroll.addView(root)
        root.label("CONFIDENTIAL  /  CASE ${caseId.take(8).uppercase()}", 12f, amber)
        root.label("Investigation", 30f, white)
        root.label("CASE TITLE")
        val title = EditText(requireContext()).apply { hint = "What happened?"; setTextColor(white); setHintTextColor(muted); maxLines = 2 }
        root.addView(title)
        root.label("INCIDENT DATE")
        val date = root.button("Select date") {
            current?.let { crime -> DateDialog().apply { arguments = Bundle().apply { putLong("date", crime.date) } }.show(childFragmentManager, "date") }
        }
        val solved = CheckBox(requireContext()).apply { text = "Case solved"; setTextColor(white) }; root.addView(solved)
        root.label("FIELD NOTES")
        val notes = EditText(requireContext()).apply { hint = "Clues, witnesses, and observations…"; setTextColor(white); setHintTextColor(muted); minLines = 3; gravity = Gravity.TOP }
        root.addView(notes)
        root.label("PHOTO EVIDENCE")
        val photo = ImageView(requireContext()).apply { setBackgroundColor(panel); scaleType = ImageView.ScaleType.FIT_CENTER; contentDescription = "Crime photo evidence" }
        root.addView(photo, LinearLayout.LayoutParams(-1, root.dp(230)))
        evidenceView = photo
        root.button("Capture evidence") { takePhoto() }
        root.button("Share case report") { shareReport() }
        root.button("Delete case") {
            current?.let { crime -> androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete this case?").setMessage("The record and its attached photo will be permanently removed.")
                .setNegativeButton("Cancel", null).setPositiveButton("Delete") { _, _ ->
                    model.delete(crime) { if (isAdded) parentFragmentManager.popBackStack() }
                }.show() }
        }
        root.label("Changes save automatically on this device.", 12f)
        title.doAfterTextChanged { if (!binding && initialized) model.title(caseId, it.toString()) }
        notes.doAfterTextChanged { if (!binding && initialized) model.notes(caseId, it.toString()) }
        solved.setOnCheckedChangeListener { _, checked -> if (!binding && initialized) model.solved(caseId, checked) }
        childFragmentManager.setFragmentResultListener("date", this) { _, result -> model.date(caseId, result.getLong("date")) }
        viewLifecycleOwnerLiveData.observe(this) { owner ->
            if (owner == null) return@observe
            viewLifecycleOwnerLiveData.removeObservers(this)
            owner.lifecycleScope.launch { owner.repeatOnLifecycle(Lifecycle.State.STARTED) { model.dao.observe(caseId).collect { crime ->
                if (crime != null) {
                    current = crime; binding = true
                    if (!initialized) { title.setText(crime.title); notes.setText(crime.notes); initialized = true }
                    date.text = dateLabel(crime.date); solved.isChecked = crime.solved
                    binding = false
                    if (shownPhoto != crime.photo) { shownPhoto = crime.photo; displayPhoto(crime.photo) }
                }
            } } }
        }
        return scroll
    }
    private fun takePhoto() {
        val directory = File(requireContext().filesDir, "evidence").apply { mkdirs() }
        val name = "${UUID.randomUUID()}.jpg"
        val file = File(directory, name)
        pendingPhoto = name
        try {
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.files", file)
            camera.launch(uri)
        } catch (e: ActivityNotFoundException) { pendingPhoto = null; file.delete(); toast("No camera app available on this device.") }
    }
    private fun displayPhoto(name: String?) {
        val view = evidenceView ?: return
        if (name == null) { view.setImageDrawable(null); return }
        val path = File(requireContext().filesDir, "evidence/$name").absolutePath
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) sample *= 2
        view.setImageBitmap(BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }))
    }
    private fun shareReport() {
        val crime = current ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Case report: ${crime.title.ifBlank { "Untitled case" }}")
            putExtra(Intent.EXTRA_TEXT, "CRIMINAL INTENT — CASE REPORT\n\nTitle: ${crime.title.ifBlank { "Untitled case" }}\nDate: ${dateLabel(crime.date)}\nStatus: ${if (crime.solved) "Solved" else "Open"}\n\nField notes:\n${crime.notes}")
        }
        try { startActivity(Intent.createChooser(intent, "Send case report")) } catch (e: ActivityNotFoundException) { toast("No app available to share the report.") }
    }
    private fun toast(message: String) { Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show() }
    override fun onDestroyView() { evidenceView = null; shownPhoto = null; super.onDestroyView() }
}

class DateDialog : DialogFragment() {
    override fun onCreateDialog(state: Bundle?): Dialog {
        val calendar = Calendar.getInstance().apply { timeInMillis = requireArguments().getLong("date") }
        return DatePickerDialog(requireContext(), { _, year, month, day ->
            calendar.set(year, month, day)
            parentFragmentManager.setFragmentResult("date", Bundle().apply { putLong("date", calendar.timeInMillis) })
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    }
}

