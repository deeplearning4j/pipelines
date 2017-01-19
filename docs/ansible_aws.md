#### Requirements

Important — yum must use Linux (Ubuntu >= 14.04 is preferable) or macOS. Windows is not supported in the scope of this document.

##### Install boto
https://github.com/boto/boto3
```bash
    vim ~/.boto
```
```
    [Credentials]
    aws_access_key_id = $AWS_ACCESS_KEY_ID
    aws_secret_access_key = $AWS_SECRET_KEY_ID
    aws_security_token = $AWS_SECURITY_TOKEN  
```
##### Install awscli
```bash
    sudo pip install awscli
```


##### Install ansible
[Detailed installation instructions](http://docs.ansible.com/ansible/intro_installation.html)

##### Export AWS credentials
```bash
export AWS_SECRET_ACCESS_KEY=XXXXXXXXXXX  
export AWS_ACCESS_KEY_ID=XXXXXXXXXXXXXXXXXXXXXXXXXXX  
export AWS_DEFAULT_REGION=us-west-2  
```

##### Start ec2 instances creation with ansible

be sure you are in directory with ansible files i.e. pipeline/ansible/aws/
```bash
ansible-playbook -v vpc.yml
```
Notice public FQDN or IP of the new created instance from ansible output, like:
```
"public_dns_name": "ec2-54-149-201-179.us-west-2.compute.amazonaws.com",
"public_ip": "54.149.201.179",
```
