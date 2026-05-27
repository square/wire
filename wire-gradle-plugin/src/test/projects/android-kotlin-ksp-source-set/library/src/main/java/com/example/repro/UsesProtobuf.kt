package com.example.repro

import com.example.repro.proto.PBMessage
import org.koin.core.annotation.Factory

@Factory
class UsesProtobuf {
  fun makeMessage(): PBMessage = PBMessage("x")
}
