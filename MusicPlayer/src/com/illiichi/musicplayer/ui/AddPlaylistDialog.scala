package com.illiichi.musicplayer.ui

import android.app.DialogFragment
import android.os.Bundle
import android.view.{ View, KeyEvent, ViewGroup, LayoutInflater }
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView.OnEditorActionListener
import android.widget.{ TextView, EditText }
import com.illiichi.musicplayer.R

class AddPlaylistDialog extends DialogFragment with OnEditorActionListener {

  var mEditText: EditText = _

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup,
    savedInstanceState: Bundle): View = {
    val view = inflater.inflate(R.layout.fragment_add_playlist, container)
    mEditText = view.findViewById(R.id.playlist_et).asInstanceOf[EditText]
    getDialog().setTitle("Creat Playlist")
    mEditText.requestFocus()
    getDialog().getWindow().setSoftInputMode(
      WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    mEditText.setOnEditorActionListener(this)
    view
  }

  override def onEditorAction(v: android.widget.TextView, actionId: Int, event: KeyEvent): Boolean = {
    if (EditorInfo.IME_ACTION_DONE == actionId) {
      val activity = getActivity().asInstanceOf[AddDialogListener]
      activity.onFinishAddDialog(mEditText.getText().toString())
      this.dismiss()
      true
    } else false
  }
}

trait AddDialogListener {
  def onFinishAddDialog(inputText: String)
}