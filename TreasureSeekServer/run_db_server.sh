cd bin
java -cp .:../libs/java-json.jar:../libs/sqlite-jdbc-3.21.0.jar main/DBServer $1
#java main.LoadBalancer
cd ..
