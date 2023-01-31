FROM amazoncorretto:11
MAINTAINER Greg Bodi <hexagonalbits@fastmail.com>

ARG API_VERSION

# install AWS CLI
RUN yum -y install python3-pip
RUN pip3 install awscli

# copy deploy folder into image
COPY ./target/api-server-${API_VERSION}-deploy-folder /app 

# these variables used in src/main/deploy/run.sh
ENV API_VERSION=${API_VERSION}

LABEL API_VERSION=${API_VERSION}

EXPOSE 4567

WORKDIR /app

# run application 
CMD ["/app/run.sh"]
