package com.example.androidcamerard.utils

import com.example.androidcamerard.R

enum class DetectionMode(val titleResId: Int, val subtitleResId: Int) {
    ILCTF_LIVE(R.string.mode_ilctf_live_title, R.string.mode_ilctf_live__subtitle),
    ILCFB_LIVE(R.string.mode_ilcfb_live_title, R.string.mode_ilcfb_live__subtitle),
    ILC_STATIC(R.string.mode_ilc_static_title, R.string.mode_ilc_static_subtitle),
    DC_LIVE(R.string.mode_dc_title, R.string.mode_dc_subtitle)
}