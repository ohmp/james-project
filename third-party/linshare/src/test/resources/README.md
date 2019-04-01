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

To check that things are working, you can try pulling images from this registry:

```
docker pull --disable-content-trust docker-registry.linagora.com:5000/linshare-snapshots/linshare-ldap-for-tests
```

## Authenticating on Linagora Docker registry

Execute the following command:

```
docker login docker-registry.linagora.com:5000
```

Then use your Linagora credentials to authenticate.

## Testing this dockerfile

Executing a simple REST query being authentified should succeed:

```
curl -XGET -i -u "user1@linshare.org:password1"  http://172.26.0.5:8080/linshare/webservice/rest/user/v2/documents
```

## Current issues

### Fails

`linshare-backend` container fails to establish a hibernate connection with the database. The above check always result in **404**.

Solution: restart only the linshare-backend container - after this restart the connection is successful.

### Non provisionned LDAP user

Executing the above check then result in **401**.

No solution found so far.