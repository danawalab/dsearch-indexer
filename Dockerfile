FROM dcr.danawa.io/alpine-k8s-java:8

RUN yum -y update && yum install -y wget rsync

RUN useradd danawa
RUN usermod -aG wheel danawa
USER danawa

WORKDIR /app

ENV PATH=$PATH:${JAVA_HOME}/bin
ENV spring_logging_level=debug
ENV LANG=ko_KR.euckr
#ENV LANG=ko_KR.utf8
ENV VERSION=1.1.0

COPY lib/Altibase.jar .
#COPY lib/danawa-product-1.1.1.jar .

EXPOSE 9350
EXPOSE 8080
EXPOSE 9100

#COPY target/* .
#CMD ["java", "-classpath", "indexer-1.1.0.jar:Altibase.jar", "org.springframework.boot.loader.JarLauncher"]