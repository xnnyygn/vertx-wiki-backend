# vertx-wiki-backend
wiki backend based on vertx

# to generate server-keystore.jks

```shell script
keytool -genkey -alias test -keyalg RSA -keystore server-keystore.jks -keysize 2048 -validity 360 -dname CN=localhost -keypass secret -storepass secret
```

webroot

```
/usr/src/app/src/main/resources/webroot
```

user/password

```
foo/bar
```