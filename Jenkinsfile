def sendNotificationToN8n(String status, String stageName, String imageTag, String containerName, String hostPort) {
    script {
        withCredentials([
            string(credentialsId: 'n8n-webhook', variable: 'N8N_WEBHOOK_URL'),
            string(credentialsId: 'host', variable: 'HOST')
        ]) {
            def payload = [
                project  : env.JOB_NAME,
                stage    : stageName,
                status   : status,
                build    : env.BUILD_NUMBER,
                image    : "${env.DOCKER_REPO}:${imageTag}",
                container: containerName,
                url      : "http://${HOST}:${hostPort}/",
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
            sendNotificationToN8n('SUCCESS', 'Pipeline Successfully', 'N/A', '${IMAGE_NAME}', 'N/A')
        }
        failure {
            echo 'Build Failed!'
        }
        always {
            cleanWs()
        }
    }
}