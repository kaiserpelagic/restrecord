java -XX:PermSize=128M -XX:MaxPermSize=256M -Xmx3072M -Xms3072M -jar `dirname $0`/sbt-launch-0.12.4.jar "$@"
