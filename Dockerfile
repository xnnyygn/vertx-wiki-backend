FROM java:8

RUN mkdir /usr/src/app
COPY target/vertx-wiki-backend-1.0-SNAPSHOT-fat.jar /usr/src/app/vertx-wiki-backend.jar

COPY src/main/resources/webroot/ /usr/src/app/webroot/
COPY keystore.jceks /usr/src/app/

EXPOSE 8080

WORKDIR /usr/src/app

#root@e9cbba43a5e1:/usr/src/app# ls -lh
#total 16M
#drwxr-xr-x 3 root root 4.0K Jan 26 13:15 db
#drwxr-xr-x 2 root root 4.0K Jan 26 13:15 file-uploads
#-rw-r--r-- 1 root root  10K Jan 17 07:46 keystore.jceks
#-rw-r--r-- 1 root root  16M Jan 26 12:48 vertx-wiki-backend.jar
#drwxr-xr-x 2 root root 4.0K Jan 26 13:12 webroot
#root@e9cbba43a5e1:/usr/src/app# ls webroot/
#index.html  wiki.js

CMD ["java", "-jar", "-Dapp.web.root=webroot", "/usr/src/app/vertx-wiki-backend.jar"]