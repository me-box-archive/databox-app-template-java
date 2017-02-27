FROM openjdk:8

COPY . /usr/src/app
WORKDIR /usr/src/app

LABEL databox.type="app"

#EXPOSE 8080

RUN javac -XDignore.symbol.file -cp "lib/*:." *.java
CMD ["java", "-cp", "lib/*:.", "Main"]
