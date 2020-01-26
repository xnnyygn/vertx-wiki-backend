FROM java:8

RUN mkdir /usr/src/app
COPY . /usr/src/app/

ADD http://ftp.riken.jp/net/apache/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz .
RUN tar -xzvf apache-maven-3.3.9-bin.tar.gz && mv apache-maven-3.3.9 /opt/maven3 && ln -s /opt/maven3/bin/mvn /usr/local/bin/mvn && rm apache-maven-3.3.9-bin.tar.gz

RUN cd /usr/src/app && mvn package

EXPOSE 8080

WORKDIR /usr/src/app

CMD ["java", "-jar", "/usr/src/app/target/vertx-wiki-backend-1.0-SNAPSHOT-fat.jar"]