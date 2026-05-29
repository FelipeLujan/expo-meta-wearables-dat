require 'json'

package = JSON.parse(File.read(File.join(__dir__, '..', 'package.json')))

# Load only the SPM helper from the consuming app's React Native install.
# Do NOT require react_native_pods.rb — the vendor package may ship its own
# nested react-native, which would overwrite codegen paths and break pod install.
unless defined?(SPM)
  installation_root = Pod::Config.instance.installation_root
  spm_helper = File.expand_path(
    "../node_modules/react-native/scripts/cocoapods/spm.rb",
    installation_root
  )
  require spm_helper
end

def spm_dependency(spec, url:, requirement:, products:)
  SPM.dependency(spec, url: url, requirement: requirement, products: products)
end

Pod::Spec.new do |s|
  s.name           = 'EMWDAT'
  s.version        = package['version']
  s.summary        = package['description']
  s.description    = package['description']
  s.license        = package['license']
  s.author         = package['author']
  s.homepage       = package['homepage']
  s.platforms      = { :ios => '17.0' }
  s.swift_version  = '5.9'
  s.source         = { git: 'https://github.com/circus-kitchens/expo-meta-wearables-dat' }
  s.static_framework = true

  s.dependency 'ExpoModulesCore'

  # Meta Wearables DAT SDK via Swift Package Manager
  spm_dependency(s,
    url: 'https://github.com/facebook/meta-wearables-dat-ios',
    requirement: { kind: 'upToNextMinorVersion', minimumVersion: '0.7.0' },
    products: ['MWDATCore', 'MWDATCamera', 'MWDATDisplay', 'MWDATMockDevice']
  )

  # Swift/Objective-C settings
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    'SWIFT_COMPILATION_MODE' => 'wholemodule'
  }

  s.source_files = '**/*.swift'
end
