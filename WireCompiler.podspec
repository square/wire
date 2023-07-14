require_relative 'wire-runtime-swift/pod_helpers.rb'

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

  s.osx.deployment_target  = '10.15'

  s.prepare_command = <<-CMD
    curl -L #{get_maven_host}/com/squareup/wire/wire-compiler/#{version}/wire-compiler-#{version}-jar-with-dependencies.jar --output compiler.jar
    if ! jar tf compiler.jar >/dev/null 2>&1; then
      echo "[WireCompiler] The compiler.jar file is invalid or corrupted."
      exit 1
    fi
  CMD

  s.preserve_paths = 'compiler.jar'
end
