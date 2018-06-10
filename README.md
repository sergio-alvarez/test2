# test2
Primero instalamos en nuestro respositorio local (o nuestro repo de artefactos, nexus, artifactory) la lib de sportalclientesweb:
	mvn install:install-file -Dfile=sportalclientesweb-1.19.0.jar -DgroupId=sanitas.bravo.clientes -DartifactId=sportalclientesweb -Dversion=1.19.0 -Dpackaging=jar

Para generar nuestro jar:
	mvn clean package
	
#Jacoco exclusions
Excluyo los pojos de com.mycorp.support, y como indicais en el enunciado con:
	mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent test org.jacoco:jacoco-maven-plugin:report

obtengo:
![Alt text](documentation/JacocoReport.png?raw=true "Jacoco Report")
