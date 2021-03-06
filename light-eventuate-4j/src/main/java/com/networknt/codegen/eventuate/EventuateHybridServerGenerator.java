package com.networknt.codegen.eventuate;

import com.jsoniter.any.Any;
import com.networknt.codegen.Generator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static java.io.File.separator;

/**
 * Created by steve on 28/04/17.
 */
public class EventuateHybridServerGenerator implements Generator {

    @Override
    public String getFramework() {
        return "light-hybrid-4j-server";
    }

    @Override
    public void generate(String targetPath, Object model, Any config) throws IOException {
        // whoever is calling this needs to make sure that model is converted to Map<String, Object>
        String rootPackage = config.get("rootPackage").toString();
        String modelPackage = config.get("modelPackage").toString();
        String handlerPackage = config.get("handlerPackage").toString();

        boolean enableHttp = config.toBoolean("enableHttp");
        String httpPort = config.toString("httpPort");
        boolean enableHttps = config.toBoolean("enableHttps");
        String httpsPort = config.toString("httpsPort");
        boolean enableRegistry = config.toBoolean("enableRegistry");
        String dockerOrganization = config.toString("dockerOrganization");
        String version = config.toString("version");
        
        if(dockerOrganization ==  null || dockerOrganization.length() == 0) dockerOrganization = "networknt";

        boolean supportClient = config.toBoolean("supportClient");
        boolean eventuateQueryModule = config.toBoolean("eventuateQueryModule");
        boolean eventuateCommandModule = config.toBoolean("eventuateCommandModule");
        transfer(targetPath, "", "pom.xml", templates.eventuate.hybrid.server.pom.template(config));
        transferMaven(targetPath);

        // There is only one port that should be exposed in Dockerfile, otherwise, the service
        // discovery will be so confused. If https is enabled, expose the https port. Otherwise http port.
        String expose = "";
        if(enableHttps) {
            expose = httpsPort;
        } else {
            expose = httpPort;
        }
        transfer(targetPath, "docker", "Dockerfile", templates.eventuate.hybrid.server.dockerfile.template(config, expose));
        transfer(targetPath, "docker", "Dockerfile-Redhat", templates.eventuate.hybrid.server.dockerfileredhat.template(config, expose));
        transfer(targetPath, "", "build.sh", templates.eventuate.hybrid.server.buildSh.template(dockerOrganization, config.get("groupId") + "." + config.get("artifactId") + "-" + config.get("version")));
        transfer(targetPath, "", ".gitignore", templates.eventuate.hybrid.gitignore.template());
        transfer(targetPath, "", "README.md", templates.eventuate.hybrid.server.README.template());
        transfer(targetPath, "", "LICENSE", templates.eventuate.hybrid.LICENSE.template());
        transfer(targetPath, "", ".classpath", templates.eventuate.hybrid.classpath.template());
        transfer(targetPath, "", ".project", templates.eventuate.hybrid.project.template());

        // config
        transfer(targetPath, ("src.main.resources.config").replace(".", separator), "service.yml", templates.eventuate.hybrid.serviceYml.template(config));

        transfer(targetPath, ("src.main.resources.config").replace(".", separator), "server.yml", templates.eventuate.hybrid.serverYml.template(config.get("groupId") + "." + config.get("artifactId") + "-" + config.get("version"), enableHttp, httpPort, enableHttps, httpsPort, enableRegistry, version));
        transfer(targetPath, ("src.test.resources.config").replace(".", separator), "server.yml", templates.eventuate.hybrid.serverYml.template(config.get("groupId") + "." + config.get("artifactId") + "-" + config.get("version"), enableHttp, "49587", enableHttps, "49588", enableRegistry, version));

        transfer(targetPath, ("src.main.resources.config").replace(".", separator), "secret.yml", templates.eventuate.hybrid.secretYml.template());
        transfer(targetPath, ("src.main.resources.config").replace(".", separator), "hybrid-security.yml", templates.eventuate.hybrid.securityYml.template());
        if(supportClient) {
            transfer(targetPath, ("src.main.resources.config").replace(".", separator), "client.yml", templates.eventuate.hybrid.clientYml.template());
        } else {
            transfer(targetPath, ("src.test.resources.config").replace(".", separator), "client.yml", templates.eventuate.hybrid.clientYml.template());
        }
        if(eventuateQueryModule) {
            transfer(targetPath, ("src.main.resources.config").replace(".", separator), "kafka.yml", templates.eventuate.hybrid.kafka.template());
            transfer(targetPath, ("src.main.resources.config").replace(".", separator), "eventuate-client.yml", templates.eventuate.hybrid.eventuateClient.template());
        }
        transfer(targetPath, ("src.main.resources.config").replace(".", separator), "primary.crt", templates.eventuate.hybrid.primaryCrt.template());
        transfer(targetPath, ("src.main.resources.config").replace(".", separator), "secondary.crt", templates.eventuate.hybrid.secondaryCrt.template());

        // logging
        transfer(targetPath, ("src.main.resources").replace(".", separator), "logback.xml", templates.eventuate.hybrid.logback.template());
        transfer(targetPath, ("src.test.resources").replace(".", separator), "logback-test.xml", templates.eventuate.hybrid.logback.template());

        // no handler as this is a server platform which supports other handlers to be deployed

        // no handler test case as this is a server platform which supports other handlers to be deployed.

        // transfer binary files without touching them.
        try (InputStream is = EventuateHybridServerGenerator.class.getResourceAsStream("/binaries/server.keystore")) {
            Files.copy(is, Paths.get(targetPath, ("src.main.resources.config").replace(".", separator), "server.keystore"), StandardCopyOption.REPLACE_EXISTING);
        }
        try (InputStream is = EventuateHybridServerGenerator.class.getResourceAsStream("/binaries/server.truststore")) {
            Files.copy(is, Paths.get(targetPath, ("src.main.resources.config").replace(".", separator), "server.truststore"), StandardCopyOption.REPLACE_EXISTING);
        }
        if(supportClient) {
            try (InputStream is = EventuateHybridServerGenerator.class.getResourceAsStream("/binaries/client.keystore")) {
                Files.copy(is, Paths.get(targetPath, ("src.main.resources.config").replace(".", separator), "client.keystore"), StandardCopyOption.REPLACE_EXISTING);
            }
            try (InputStream is = EventuateHybridServerGenerator.class.getResourceAsStream("/binaries/client.truststore")) {
                Files.copy(is, Paths.get(targetPath, ("src.main.resources.config").replace(".", separator), "client.truststore"), StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            try (InputStream is = EventuateHybridServerGenerator.class.getResourceAsStream("/binaries/client.keystore")) {
                Files.copy(is, Paths.get(targetPath, ("src.test.resources.config").replace(".", separator), "client.keystore"), StandardCopyOption.REPLACE_EXISTING);
            }
            try (InputStream is = EventuateHybridServerGenerator.class.getResourceAsStream("/binaries/client.truststore")) {
                Files.copy(is, Paths.get(targetPath, ("src.test.resources.config").replace(".", separator), "client.truststore"), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

}
