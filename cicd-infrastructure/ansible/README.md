# Prerequisites
1. Python >= 2.7.12
2. Ansible >= 2.4.2.0
3. Vagrant >= 2.0.3

# Run scripts

Run following commands to test your changes locally:

```
git clone git@github.com:deeplearning4j/pipelines.git && cd cicd-infrastructure/ansible
vagrant up
export ANSIBLE_VAULT_PASSWORD=<vault_password>
ansible-playbook -i development site.yml
# To check scripts before run
ansible-playbook -i development site.yml --check
```

Run following commands to run scripts on real hosts:

```
git clone git@github.com:deeplearning4j/pipelines.git && cd cicd-infrastructure/ansible
vagrant up
export ANSIBLE_VAULT_PASSWORD=<vault_password>
ansible-playbook -i production site.yml
```

# Issues/Limitations
1. Installation of Homebrew should be done manually.
2. There could be a chance when extraction of Xcode with open command can fail with error 10810,
   in this case you need to restart the host.
   If this didn't help try to open VNC connection during Ansible scritps execution, should fix the problem.
3. Missing logic for enabling remote logic for Jenkins user when connection is ssh
4. Missing logic for disabling screensaver.
5. Missing logic for customizing the dock.
6. Missing logic for disabling updates, except security updates.
7. Missing logic for fallback and cleanup.