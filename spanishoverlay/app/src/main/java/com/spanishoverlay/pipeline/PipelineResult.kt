package com.spanishoverlay.pipeline

data class PipelineResult(val replacements: List<Replacement>, val ttlMs: Int, val alpha: Float)
