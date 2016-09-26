
VAGRANTFILE_API_VERSION = "2"
BOX_NAME = ENV['BOX_NAME'] || "ubuntu/trusty64"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|

  config.vm.box = BOX_NAME

  config.vm.provider :virtualbox do |vb|
    vb.customize ["modifyvm", :id, "--name", "eventuate-chaos", "--memory", "8192", "--cpus", "2"]
  end

  config.vm.provision "docker"
  config.vm.provision "shell" do |s|
    s.path = "provision.sh"
    s.args = "/vagrant"
  end
end
