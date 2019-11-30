//
//  DinosaurView.swift
//  Dinosaurs
//
//  Created by Egor Andreevici on 2019-12-23.
//  Copyright © 2019 Square. All rights reserved.
//

import SwiftUI
import protos

struct DinosaurView: View {
  var body: some View {
    VStack(alignment: .leading) {
      Text("Name: \(dinosaur.name ?? "Unknown")")
        .font(.title)
        .padding()
      Text("Period: \(dinosaur.period?.description() ?? "Unknown")")
        .font(.title)
        .padding()
      Text("Length: \(dinosaur.length_meters?.description ?? "Unknown")")
        .font(.title)
        .padding()
      Text("Mass: \(dinosaur.mass_kilograms?.description ?? "Unknown")")
        .font(.title)
        .padding()
    }
  }
  
  var dinosaur: Dinosaur
}

struct DinosaurView_Previews: PreviewProvider {
  static var previews: some View {
    return DinosaurView(dinosaur: buildDinosaur())
  }
  
  static func buildDinosaur() -> Dinosaur {
    let encodedDino = "CgtTdGVnb3NhdXJ1cxIUaHR0cDovL2dvby5nbC9MRDVLWTUSFGh0dHA6Ly9nb28uZ2wvVllSTTY3GQAAAAAAACJAIQAAAAAAiLNAKAI="
    let nsData = NSData(base64Encoded: encodedDino, options: .ignoreUnknownCharacters)!
    let dinoBytes = OkioKt.decodeBase64(base64: nsData.base64EncodedString(options: .endLineWithCarriageReturn))
    return Dinosaur.Companion.init().ADAPTER.decode(bytes_: dinoBytes) as! Dinosaur
  }
}
