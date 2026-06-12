FROM registry.access.redhat.com/ubi9/ubi-minimal:9.6

RUN microdnf install -y haproxy && microdnf clean all

EXPOSE 8883

ENTRYPOINT ["haproxy","-W","-db","-f","/usr/local/etc/haproxy/haproxy.cfg"]

