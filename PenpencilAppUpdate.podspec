
  Pod::Spec.new do |s|
    s.name = 'PenpencilAppUpdate'
    s.version = '0.0.1'
    s.summary = 'This is for hot update'
    s.license = 'MIT'
    s.homepage = 'https://github.com/deveshmishra34/penpencil-app-update.git'
    s.author = 'Devesh mishra'
    s.source = { :git => 'https://github.com/deveshmishra34/penpencil-app-update.git', :tag => s.version.to_s }
    s.source_files = 'ios/Plugin/**/*.{swift,h,m,c,cc,mm,cpp}'
    s.ios.deployment_target  = '11.0'
    s.dependency 'Capacitor'
    s.dependency 'Zip', '~> 1.1'
  end