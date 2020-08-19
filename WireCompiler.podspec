Pod::Spec.new do |s|
  s.name          = 'WireCompiler'
  s.version       = "#{ENV['POD_VERSION']}"
  s.license       = { :type => 'apache2', :file => 'LICENSE.txt' }
  s.homepage      = 'https://github.com/square/wire'
  s.authors       = { 'Eric Firestone' => '@firetweet' }
  s.summary       = 'gRPC and protocol buffer compiler for Android, Kotlin, Java, and Swift.'
  s.source        = { :git => 'https://github.com/square/wire.git', :tag => "#{ENV['POD_VERSION']}" }
  s.module_name   = 'WireCompiler'

  s.prepare_command = <<-CMD
    ./gradlew -p wire-library :wire-compiler:assemble
    cp ./wire-library/wire-compiler/build/libs/wire-compiler-*-jar-with-dependencies.jar ./compiler.jar
  CMD

  s.preserve_paths = 'compiler.jar'
end
