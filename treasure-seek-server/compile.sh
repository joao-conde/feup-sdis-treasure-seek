mkdir -p  bin
javac -cp ./src:libs/java-json.jar:libs/sqlite-jdbc-3.21.0.jar src/model/*.java -d bin -Xlint
javac -cp ./src:libs/java-json.jar:libs/sqlite-jdbc-3.21.0.jar src/util/*.java -d bin -Xlint
javac -cp ./src:libs/java-json.jar:libs/sqlite-jdbc-3.21.0.jar src/communications/*.java -d bin -Xlint
javac -cp ./src:libs/java-json.jar:libs/sqlite-jdbc-3.21.0.jar src/controller/*.java -d bin -Xlint
javac -cp ./src:libs/java-json.jar:libs/sqlite-jdbc-3.21.0.jar src/main/*.java -d bin -Xlint 
