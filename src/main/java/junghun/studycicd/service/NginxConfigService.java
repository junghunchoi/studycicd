package junghun.studycicd.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class NginxConfigService {
    
    private static final Logger logger = LoggerFactory.getLogger(NginxConfigService.class);
    
    @Value("${nginx.container.name:nginx-lb}")
    private String nginxContainerName;
    
    @Value("${nginx.reload.script:/reload-config.sh}")
    private String reloadScript;

    public boolean updateNginxConfig(Integer legacyWeight, Integer refactoredWeight) {
        try {
            // Docker exec 명령을 통해 NGINX 설정 업데이트
            String[] command = {
                "docker", "exec", nginxContainerName, 
                reloadScript, 
                legacyWeight.toString(), 
                refactoredWeight.toString(), 
                refactoredWeight.toString()
            };
            
            logger.info("Executing NGINX reload command: {}", String.join(" ", command));
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                logger.error("NGINX reload command timed out");
                return false;
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                logger.info("NGINX configuration updated successfully");
                return true;
            } else {
                logger.error("NGINX reload command failed with exit code: {}", exitCode);
                return false;
            }
            
        } catch (IOException e) {
            logger.error("Failed to execute NGINX reload command", e);
            return false;
        } catch (InterruptedException e) {
            logger.error("NGINX reload command was interrupted", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public boolean checkNginxHealth() {
        try {
            String[] command = {
                "docker", "exec", nginxContainerName, 
                "nginx", "-t"
            };
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            
            return process.exitValue() == 0;
            
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to check NGINX health", e);
            return false;
        }
    }
}