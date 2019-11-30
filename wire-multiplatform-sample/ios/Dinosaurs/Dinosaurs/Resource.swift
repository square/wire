//
//  Resource.swift
//  Dinosaurs
//
//  Created by Egor Andreevici on 2019-12-23.
//  Copyright © 2019 Square. All rights reserved.
//

import Combine
import Foundation
import protos

final class DinosaurResource: ObservableObject {
  let url = URL(string: "http://127.0.0.1:8080/dinosaur")!
  let didChange = PassthroughSubject<Dinosaur?, Never>()
  var value: Dinosaur? {
    willSet {
      DispatchQueue.main.async {
        self.didChange.send(self.value)
      }
    }
  }
  init() {
    reload()
  }
  func reload() {
    let task = URLSession.shared.dataTask(with: url) { data, response, error in
      if let clientError = error {
        self.handleClientError(error: clientError)
        return
      }
      guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
        self.handleServerError(response: response as! HTTPURLResponse)
        return
      }
      self.value = self.toDinosaur(data: data!).freeze()
    }
    task.resume()
  }
  
  func handleClientError(error: Error) {
    print("Client error: \(error.localizedDescription)")
  }
  
  func handleServerError(response: HTTPURLResponse) {
    print("Response: \(response.statusCode)")
  }
  
  func toDinosaur(data: Data) -> Dinosaur {
    let bytes = KotlinByteArray.init(size: Int32(data.count))
    for (index, element) in data.map({ Int8(bitPattern: $0) }).enumerated() {
      bytes.set(index: Int32(index), value: element)
    }
    return Dinosaur.Companion.init().ADAPTER.decode(bytes: bytes) as! Dinosaur
  }
}
