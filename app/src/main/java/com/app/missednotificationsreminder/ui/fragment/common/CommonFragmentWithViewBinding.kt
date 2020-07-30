@file:JvmName("CommonFragmentWithViewBinding")

package com.app.missednotificationsreminder.ui.fragment.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

open class CommonFragmentWithViewBinding<T : ViewDataBinding>(
        @LayoutRes val layoutId: Int,
        private val clearBindingOnViewDestroyed: Boolean = true) : CommonFragment() {
    private var _binding: T? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    protected val viewDataBinding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        if (_binding == null) {
            _binding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        }
        return viewDataBinding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (clearBindingOnViewDestroyed) {
            _binding = null
        }
    }
}