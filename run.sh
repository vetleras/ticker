# use with "sudo bash run.sh <name of the file with no extension>"

if sudo javac -cp org.eclipse.paho.client.mqttv3-1.2.1.jar: $1.java; then
    echo "Java program '$1' successfully compiled"
    sudo java -cp org.eclipse.paho.client.mqttv3-1.2.1.jar:  $1
else
    echo "Java program '$1' compilation failed"
fi
