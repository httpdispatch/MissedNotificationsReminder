package com.app.missednotificationsreminder.ui.fragment.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.navArgs
import com.app.missednotificationsreminder.R
import com.app.missednotificationsreminder.data.source.ResourceDataSource
import com.app.missednotificationsreminder.di.qualifiers.FragmentScope
import dagger.android.ContributesAndroidInjector
import dagger.android.support.DaggerDialogFragment
import javax.inject.Inject

class AlertDialogFragment : DaggerDialogFragment() {
    private val args by navArgs<AlertDialogFragmentArgs>()

    @Inject
    lateinit var resourcesDataSource: ResourceDataSource

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireActivity())
                .setTitle(args.title)
                .setMessage(args.message)
                .setPositiveButton(args.okButton
                        ?: resourcesDataSource.getString(R.string.common_ok_action)) { _, _ -> }
                .setNegativeButton(args.cancelButton) { _, _ -> }
                .create()
    }

    @dagger.Module
    abstract class Module {
        @FragmentScope
        @ContributesAndroidInjector(modules = [])
        abstract fun contribute(): AlertDialogFragment
    }
}