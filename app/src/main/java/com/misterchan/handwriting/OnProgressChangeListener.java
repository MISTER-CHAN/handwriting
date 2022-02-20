package com.misterchan.handwriting;

import android.widget.SeekBar;

public interface OnProgressChangeListener extends SeekBar.OnSeekBarChangeListener {

    void onProgressChanged(int progress);

    default void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        onProgressChanged(progress);
    }

    @Override
    default void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    default void onStopTrackingTouch(SeekBar seekBar) {}
}
