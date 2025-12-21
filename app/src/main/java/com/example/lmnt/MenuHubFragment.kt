package com.example.lmnt.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.example.lmnt.MainActivity
import com.example.lmnt.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MenuHubFragment : BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentBottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_menu_hub, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // KLICK AUF EINSTELLUNGEN
        view.findViewById<LinearLayout>(R.id.optionSettings).setOnClickListener {
            // Später: val settingsFragment = SettingsFragment()
            dismiss()
        }

        // KLICK AUF STATISTIKEN
        view.findViewById<LinearLayout>(R.id.optionStatistics).setOnClickListener {
            dismiss() // Zuerst das BottomSheet schließen

            // Das neue Statistik-Fragment erstellen
            val statsFragment = StatisticsFragment()

            // Über die MainActivity das Fragment tauschen
            (activity as? MainActivity)?.let { main ->
                main.supportFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragment_container, statsFragment)
                    .addToBackStack(null)
                    .commit()

                // Den Container sichtbar machen, falls er noch GONE ist
                main.findViewById<View>(R.id.fragment_container).visibility = View.VISIBLE
            }
        }

        // KLICK AUF ABOUT
        view.findViewById<LinearLayout>(R.id.optionAbout).setOnClickListener {
            dismiss()
        }
    }
}