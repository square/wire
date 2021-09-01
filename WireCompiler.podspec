require_relative 'wire-library/wire-runtime-swift/pod_helpers.rb'

Pod::Spec.new do |s|
  version = get_version

  s.name          = 'WireCompiler'
  s.version       = version
  s.license       = { :type => 'apache2', :file => 'LICENSE.txt' }
  s.homepage      = 'https://github.com/square/wire'
  s.authors       = { 'Eric Firestone' => '@firetweet' }
  s.summary       = 'gRPC and protocol buffer compiler for Android, Kotlin, Java, and Swift.'
  s.source        = { :git => 'https://github.com/square/wire.git', :tag => version }
  s.module_name   = 'WireCompiler'

  s.prepare_command = <<-CMD
    JAVA_HOME="$(/usr/libexec/java_home -v 1.8)"
    ./gradlew -p wire-library :wire-compiler:assemble
    MOST_RECENT_ARTIFACT="$(ls -t ./wire-library/wire-compiler/build/libs/wire-compiler-*-jar-with-dependencies.jar | head -n1)"
    cp "$MOST_RECENT_ARTIFACT" ./compiler.jar
  CMD

  s.preserve_paths = 'compiler.jar'
end
