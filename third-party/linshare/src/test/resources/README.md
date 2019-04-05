# Docker-compose set up

In order to be running this docker-compose, a few steps are required:

 - Trusting Linagora certificates
 - Authenticating on Linagora Docker registry

## Trusting Linagora certificates

Execute the following commands as root

```
mkdir /etc/docker/certs.d
mkdir /etc/docker/certs.d/docker-registry.linagora.com:5000
wget https://raw.githubusercontent.com/linagora/linshare/master/documentation/EN/development/GandiStandardSSLCA2.pem -O /etc/docker/certs.d/docker-registry.linagora.com:5000/ca.crt
```

Then restart docker

## Authenticating on Linagora Docker registry

Execute the following command:

```
docker login docker-registry.linagora.com:5000
```

Then use your Linagora credentials to authenticate.

To check that things are working, you can try pulling images from this registry:

```
docker pull --disable-content-trust docker-registry.linagora.com:5000/linshare-snapshots/linshare-ldap-for-tests
```

