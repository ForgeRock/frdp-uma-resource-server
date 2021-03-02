# Dockerfile : Resource Server
#
# This dockerfile has two stages:
# 1: maven, gets source code and compiles/packages into web app
# 2: tomcat, copies web app, adds config
#
# Commands:
# docker build -t frdp-uma-resource-server:1.2.0 .
# docker run --name resource-server --rm -p 8080:8080 frdp-uma-resource-server:1.2.0
# docker exec -it resource-server /bin/bash
# docker login -u USER -p PASSWORD
# docker tag frdp-uma-resource-server:1.2.0 USER/frdp-uma-resource-server:1.2.0
# docker push USER/frdp-uma-resource-server:1.2.0

# Get a container (maven) for compiling source code

FROM maven:3-openjdk-11 AS build

# Get the required projects from github

RUN git clone --branch 1.2.0 --progress --verbose https://github.com/ForgeRock/frdp-framework 
RUN git clone --branch 1.2.0 --progress --verbose https://github.com/ForgeRock/frdp-dao-mongo
RUN git clone --branch 1.2.0 --progress --verbose https://github.com/ForgeRock/frdp-dao-rest 

# run maven (mvn) to compile jar files and package the war file

WORKDIR "/frdp-framework"
RUN mvn compile install

WORKDIR "/frdp-dao-mongo"
RUN mvn compile install

WORKDIR "/frdp-dao-rest"
RUN mvn compile install

RUN mkdir /frdp-uma-resource-server
COPY . /frdp-uma-resource-server

WORKDIR "/frdp-uma-resource-server"
RUN mvn compile package 

# Get a container (tomcat) to run the application

FROM tomcat:9-jdk11-adoptopenjdk-hotspot

# Remove default applicatons

RUN rm -rf /usr/local/tomcat/webapps/*

# Copy the expanded application folder

COPY --from=build /frdp-uma-resource-server/target/resource-server /usr/local/tomcat/webapps/resource-server

EXPOSE 8090:8080
# EXPOSE 9000:8000

# Environment variables for attaching Java debugger

# ENV JPDA_ADDRESS="8000"
# ENV JPDA_TRANSPORT="dt_socket"

# Command to run ... "jpda" is for the java debugger

# CMD ["catalina.sh", "jpda", "run"]
CMD ["catalina.sh", "run"]
