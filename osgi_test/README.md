# Development

to run the osgi tests use:

    mvn clean && mvn install -DskipTests=true -Dmaven.javadoc.skip=true -B -V && mvn -B test -pl osgi_test