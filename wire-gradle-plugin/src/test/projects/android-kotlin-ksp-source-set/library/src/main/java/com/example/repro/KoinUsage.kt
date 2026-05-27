package com.example.repro

import org.koin.ksp.generated.module

object KoinUsage {
  val modules = listOf(
    ReproModule().module,
  )
}
