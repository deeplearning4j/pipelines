### Tinc setup for jenkins machines

See http://tinc-vpn.org/ for details

Those run with the following additions to the normal iptables setup on Master-jenkins (`/etc/iptables.rules` and its `/etc/network/if-(pre|post)-up`):

```
-A DOCKER -i eth0 -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT
-A DOCKER -i eth0 -p tcp -m tcp --dport 8080 -j ACCEPT
-A DOCKER ! -s 10.9.7.0/24 -i eth0 -j DROP
```

Ensuring a safe setup and communications to the various slaves.
See also:

https://github.com/zodern/meteor-up/issues/119
https://github.com/moby/moby/issues/22054
