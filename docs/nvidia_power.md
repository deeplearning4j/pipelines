#### nvidia-docker on POWER: GPUs Inside Docker Containers

access to Power8 host (Centos7):
```
ssh -i ~/.ssh/keys/osu_centos centos@140.211.168.159
```
uninstall old docker
```
sudo yum install yum-plugin-remove-with-leaves
sudo yum remove docker --remove-leaves

```
add file /etc/yum.repos.d/docker.repo
```
[docker]
name=Docker
baseurl=http://ftp.unicamp.br/pub/ppc64el/rhel/7_1/docker-ppc64el/
enabled=1
gpgcheck=0
```

install docker, enable and start docker daemon
```
sudo yum install docker
sudo systemctl enable docker
sudo systemctl start docker
```
