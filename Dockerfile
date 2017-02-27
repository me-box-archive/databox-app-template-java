FROM openjdk:7

COPY . /usr/src/app
WORKDIR /usr/src/app

RUN javac *.java
CMD ["java", "Main"]
