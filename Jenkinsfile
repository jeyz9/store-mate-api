def sendNotificationToN8n(String status, String stageName, String image, String containerName, String message = "") {
    script {
        withCredentials([
            string(credentialsId: 'n8n-webhook', variable: 'N8N_WEBHOOK_URL')
        ]) {
            def payload = [
                project  : env.JOB_NAME,
                stage    : stageName,
                status   : status,
                build    : env.BUILD_NUMBER,
                branch   : env.GIT_BRANCH,
                image    : image ?: "N/A",
                container: containerName ?: "N/A",
                url      : "https://api.store-mate-api.me/swagger-ui/index.html",
                message  : message,
                timestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ssXXX")
            ]

            def body = groovy.json.JsonOutput.toJson(payload)

            try {
                httpRequest acceptType: 'APPLICATION_JSON',
                            contentType: 'APPLICATION_JSON',
                            httpMode: 'POST',
                            requestBody: body,
                            url: N8N_WEBHOOK_URL,
                            validResponseCodes: '200:299'

                echo "n8n webhook (${status}) sent successfully."
            } catch (err) {
                echo "Failed to send n8n webhook (${status}): ${err}"
            }
        }
    }
}

pipeline {
    agent any

    triggers {
        githubPush() 
    }

    environment {
        REGISTRY_USER = "jeyzdev"
        IMAGE_NAME = "store-mate-api"
        DOCKER_HUB    = credentials('docker-hub-creds')
        SONAR_TOKEN = credentials('sonar-token')
    }

    stages {
        stage('bug') {
            sh 'docker bug bug'
        }

        stage('Build + Test + Sonar') {
            when {
                expression { 
                    return env.GIT_BRANCH == 'origin/main' || env.GIT_BRANCH == 'main' 
                }
            }
            steps {
                sh '''
                mvn clean verify sonar:sonar \
                  -Dsonar.projectKey=jeyzdev_store-mate \
                  -Dsonar.organization=jeyzdev \
                  -Dsonar.host.url=https://sonarcloud.io \
                  -Dsonar.login=$SONAR_TOKEN \
                  -Dsonar.coverage.exclusions=**
                '''
            }
        }

        stage('Build & Push Docker') {
            when {
                expression { 
                    return env.GIT_BRANCH == 'origin/main' || env.GIT_BRANCH == 'main' 
                }
            }
            steps {
                sh 'echo ${DOCKER_HUB_PSW} | docker login -u ${DOCKER_HUB_USR} --password-stdin'
                sh 'docker build -t ${REGISTRY_USER}/${IMAGE_NAME}:latest .'
                sh 'docker push ${REGISTRY_USER}/${IMAGE_NAME}:latest'
            }
        }

        stage('Stop Old Container') {
            when {
                expression { 
                    return env.GIT_BRANCH == 'origin/main' || env.GIT_BRANCH == 'main' 
                }
            }
            steps {
                sh '''
                docker stop ${IMAGE_NAME} || true
                docker rm ${IMAGE_NAME} || true
                docker rmi ${REGISTRY_USER}/${IMAGE_NAME}:latest || true
                '''
            }
        }

        stage('Deploy') {
            when {
                expression { 
                    return env.GIT_BRANCH == 'origin/main' || env.GIT_BRANCH == 'main' 
                }
            }
            steps {
                sh '''
                docker run -d -p 8081:8080 \
                --name ${IMAGE_NAME} \
                --restart always \
                --env-file /home/ubuntu/app/.env \
                ${REGISTRY_USER}/${IMAGE_NAME}:latest
                '''
                
                sh 'docker logout'
            }
        }
    }

    post {
        success {
            echo 'Deploy Success!'
            sendNotificationToN8n(
                'SUCCESS',
                'Deploy Completed',
                "${REGISTRY_USER}/${IMAGE_NAME}:latest",
                IMAGE_NAME
            )
        }
    
        failure {
            script{
                echo 'Build Failed!'
                def logs = currentBuild.rawBuild.getLog(300)
                
                def errors = logs.findAll {
                    it.contains("ERROR") ||
                    it.contains("Failed") ||
                    it.contains("Module not found") ||
                    it.contains("Syntax error")
                }
                
                sendNotificationToN8n(
                    'FAILED',
                    env.STAGE_NAME ?: 'Unknown Stage',
                    'N/A',
                    'N/A',
                    errors.join('\n')
                )
            }
        }
    
        always {
            cleanWs()
        }
    }
}