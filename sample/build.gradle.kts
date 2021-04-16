plugins {
  application
  id("com.squareup.wire")
}

application {
  mainClassName = "com.squareup.dinosaurs.Sample"
}

val jar by tasks.getting(Jar::class) {
  manifest {
    attributes("Automatic-Module-Name" to "wire-sample")
  }
}

dependencies {
  implementation(deps.wire.runtime)
}

/**
 * Default settings:
 * 1) all proto definitions located in "${projectDir}/src/main/proto"
 * 2) all protos generated in Java
 * 3) all generated protos written to "${projectDir}/build/generated/source/wire"
 *
 * If all of the above suffice, no additional configuration is needed!
 */
//wire {
//  // Keeps 'name' as the only field in 'Dinosaur'
//  // roots = ['squareup.dinosaurs.Dinosaur#name']
//
//  // Keeps all fields, except 'name', in 'Dinosaur'
//  // prunes = ['squareup.dinosaurs.Dinosaur#name']
//
//  // Both roots and prunes in an external file
//  // rules file("rules.txt")
//
//  // java {
//  //  out "${buildDir}/build/generated/source/wire"
//  // }
//}
