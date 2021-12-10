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
    curl https://repo.maven.apache.org/maven2/com/squareup/wire/wire-compiler/#{version}/wire-compiler-#{version}-jar-with-dependencies.jar --output compiler.jar
  CMD

  s.preserve_paths = 'compiler.jar'
end
