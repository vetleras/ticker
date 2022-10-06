# use with "sudo bash run.sh <name of the file with no extension>"

if sudo javac -classpath .:classes:/opt/pi4j/lib/'*' -cp *.jar $1.java; then
    echo "Java program '$1' successfully compiled"
    sudo java -classpath .:classes:/opt/pi4j/lib/'*' -cp *.jar $1
else
    echo "Java program '$1' compilation failed"
fi