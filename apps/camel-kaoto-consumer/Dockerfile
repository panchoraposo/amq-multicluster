FROM registry.access.redhat.com/ubi9/openjdk-21:1.22 AS build
USER 0
RUN microdnf install -y maven && microdnf clean all
WORKDIR /workspace

COPY pom.xml pom.xml
COPY apps/camel-kaoto-consumer/pom.xml apps/camel-kaoto-consumer/pom.xml
COPY apps/camel-kaoto-consumer/src apps/camel-kaoto-consumer/src

RUN mvn -q -DskipTests -f apps/camel-kaoto-consumer/pom.xml package

FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.22
WORKDIR /work
COPY --from=build /workspace/apps/camel-kaoto-consumer/target/*-runner.jar /work/app.jar
EXPOSE 8080
CMD ["java","-jar","/work/app.jar"]

