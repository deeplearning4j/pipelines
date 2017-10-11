# nvidia-docker (for amd64 architecture) on demand EC2 instance

## Pre requirements:
1. Jenkins (2+ version) with Amazon EC2 (id: ec2), Docker Pipeline (id: docker-workflow) and Ansible (id: ansible) plugins.
2. AWS credentials with preconfigured security group(s), keypair(s), etc.
3. NVIDIA CUDA Toolkit 9 on Amazon Linux with accepted agreement for use.

## Configuration:

### 1. Jenkins Amazon EC2 plugin:

 - Go to **Manage Jenkins** -> **Cloud** and in **Amazon EC2** section and fill out fields according to yours:
 <p align="center">
   <img src="/imgs/ec2_plugin_1.png"/>
 </p>

 <p align="center">
   <img src="/imgs/ec2_plugin_2.png"/>
 </p>

 - And in **Init script** field define what need to do during provision EC2 slave of Jenkins:

 ```
  sudo yum update -y
  sudo yum install git -y
  sudo pip install ansible
  sudo yum install docker -y
  sudo usermod -G docker ec2-user
  wget -P /tmp http://us.download.nvidia.com/XFree86/Linux-x86_64/375.26/NVIDIA-Linux-x86_64-375.26.run
  sudo bash /tmp/NVIDIA-Linux-x86_64-375.26.run -s
 ```

### 2. Ansible plugin:

- In our case, ansible will be installed via **Init script** section. We should define the path to ansible on our future EC2 slave.
Go to **Manage Jenkins** -> **Global Tool Configuration** and inside **Ansible** section and define following (note, that path may be different
on different distributives):

<p align="center">
  <img src="/imgs/ansible_plugin.png"/>
</p>

### 3. Configure pipeline job on our Jenkins master:

- Click **New Item**, define name of **Item**, choose **Pipeline** and click **OK**
- Fill out **Pipeline** section (define **Repositories** with nececcery **Credentials** and **Script Path**):

<p align="center">
  <img src="/imgs/pipeline_config.png"/>
</p>

### Our pipeline script looks as [ec2-gpu-slave.groovy](/jobs/ec2-gpu-slave.groovy)
### Our playbook with roles looks as [playbook](/ansible/aws/cd)
