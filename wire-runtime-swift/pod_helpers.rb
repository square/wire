# Gets the version of the library to use
def get_version
  # If there's a manually specified version, then use that
  return ENV['POD_VERSION'] unless ENV['POD_VERSION'].nil?

  version_from_gradle = version_from_gradle()

  # For development purposes (like with `pod gen`) we can use the version from Gradle directly.
  return version_from_gradle unless is_publishing?

  # When publishing, if this is a release version (not a snapshot), then we can use the Gradle version as well.
  return version_from_gradle if is_release_version?(version_from_gradle)

  error_message = "Must specify the POD_VERSION environment variable when publishing alpha builds. The alpha version should be based on #{version_from_gradle}."

  # CocoaPods won't surface the exception string, so print it separately here.
  puts error_message

  raise error_message
end

def get_maven_host
  return ENV['POD_MAVEN_HOST'] || 'https://repo.maven.apache.org/maven2'
end

private def git_root
  `git rev-parse --show-toplevel`.strip
end

private def is_publishing?
  ARGV.include?('trunk') && ARGV.include?('push')
end

private def is_release_version?(version)
  version =~ /^[0-9.]+$/
end

private def version_from_gradle
  properties_file = File.join(git_root, 'gradle.properties')
  File.read(properties_file).match(/^VERSION_NAME=([0-9.]+.*)$/).captures[0]
end
