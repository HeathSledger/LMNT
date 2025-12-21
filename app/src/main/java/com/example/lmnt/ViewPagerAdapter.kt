// In der Datei ViewPagerAdapter.kt
package com.example.lmnt.ui.theme

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(
    activity: AppCompatActivity,
    private val fragments: List<Fragment> // Hier speichern wir die Fragmente
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

    // DIESE FUNKTION FEHLT BEI DIR NOCH:
    fun getFragment(position: Int): Fragment {
        return fragments[position]
    }
}