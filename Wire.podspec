Pod::Spec.new do |s|
  s.name          = 'Wire'
  s.version       = '3.2.2'
  s.license       = { :file => 'LICENSE.txt' }
  s.homepage      = 'https://github.com/square/wire'
  s.authors       = { 'Eric Firestone' => '@firetweet' }
  s.summary       = 'gRPC and protocol buffers for Android, Kotlin, Java, and Swift.'
  s.source        = { :git => 'https://github.com/square/wire.git', :tag => 'wire-3.2.2' }
  s.module_name   = 'Wire'
  s.swift_version = '5.0'

  s.ios.deployment_target  = '13.0'
  s.osx.deployment_target  = '10.15'

  s.source_files  = 'wire-library/wire-runtime-swift/src/main/swift/*.swift'

  s.test_spec do |test_spec|
    test_spec.source_files = 'wire-library/wire-runtime-swift/src/test/swift/*.swift'
  end
end
