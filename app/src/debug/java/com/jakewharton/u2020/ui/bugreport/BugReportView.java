package com.jakewharton.u2020.ui.bugreport;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.app.missednotificationsreminder.databinding.BugreportViewBinding;
import com.jakewharton.u2020.ui.misc.EmptyTextWatcher;
import com.jakewharton.u2020.util.Strings;

public final class BugReportView extends LinearLayout {
  BugreportViewBinding mBinding;

  public interface ReportDetailsListener {
    void onStateChanged(boolean valid);
  }

  private ReportDetailsListener listener;

  public BugReportView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();

    mBinding = BugreportViewBinding.bind(this);
    mBinding.title.setOnFocusChangeListener((v, hasFocus) -> {
      if (!hasFocus) {
        mBinding.title.setError(Strings.isBlank(mBinding.title.getText()) ? "Cannot be empty." : null);
      }
    });
    mBinding.title.addTextChangedListener(new EmptyTextWatcher() {
      @Override public void afterTextChanged(Editable s) {
        if (listener != null) {
          listener.onStateChanged(!Strings.isBlank(s));
        }
      }
    });

    mBinding.screenshot.setChecked(true);
    mBinding.logs.setChecked(true);
  }

  public void setBugReportListener(ReportDetailsListener listener) {
    this.listener = listener;
  }

  public Report getReport() {
    return new Report(String.valueOf(mBinding.title.getText()),
            String.valueOf(mBinding.description.getText()), mBinding.screenshot.isChecked(),
            mBinding.logs.isChecked());
  }

  public static final class Report {
    public final String title;
    public final String description;
    public final boolean includeScreenshot;
    public final boolean includeLogs;

    public Report(String title, String description, boolean includeScreenshot,
        boolean includeLogs) {
      this.title = title;
      this.description = description;
      this.includeScreenshot = includeScreenshot;
      this.includeLogs = includeLogs;
    }
  }
}
