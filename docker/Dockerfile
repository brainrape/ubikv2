FROM openjdk:8

ARG jar_version
ENV jar_version ${jar_version}

EXPOSE 3000

WORKDIR /usr/local/ubikv2
ADD ubikv2-$jar_version.jar ubikv2-$jar_version.jar

CMD /usr/bin/java -jar -Xmx1024m ubikv2-$jar_version.jar
