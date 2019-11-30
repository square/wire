//
//  ContentView.swift
//  Dinosaurs
//
//  Created by Egor Andreevici on 2019-12-23.
//  Copyright © 2019 Square. All rights reserved.
//

import SwiftUI
import protos

struct ContentView: View {
  @ObservedObject var dinosaur = DinosaurResource()
  var body: some View {
    Group {
      if dinosaur.value == nil {
        Text("Loading...")
      } else {
        DinosaurView(dinosaur: dinosaur.value!)
      }
    }
  }
}

struct ContentView_Previews: PreviewProvider {
  static var previews: some View {
    ContentView()
  }
}
