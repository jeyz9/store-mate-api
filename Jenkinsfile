pipeline {
    agent any

    environment {
        REGISTRY_USER = "jeyzdev"
        IMAGE_NAME = "store-mate-api"
        DOCKER_HUB    = credentials('docker-hub-creds')
        SONAR_TOKEN = credentials('sonar-token')
    }

    stages {

        stage('Build + Test + Sonar') {
            steps {
                sh '''
                mvn clean verify sonar:sonar \
                  -Dsonar.projectKey=jeyzdev_store-mate \
                  -Dsonar.organization=jeyzdev \
                  -Dsonar.host.url=https://sonarcloud.io \
                  -Dsonar.login=$SONAR_TOKEN
                '''
            }
        }

        stage('Build & Push Docker') {
            steps {
                sh 'echo ${DOCKER_HUB_PSW} | docker login -u ${DOCKER_HUB_USR} --password-stdin'
                sh 'docker build -t ${REGISTRY_USER}/${IMAGE_NAME}:latest .'
                sh 'docker push ${REGISTRY_USER}/${IMAGE_NAME}:latest'
            }
        }

        stage('Stop Old Container') {
            steps {
                sh '''
                docker stop $CONTAINER_NAME || true
                docker rm $CONTAINER_NAME || true
                '''
            }
        }

        stage('Deploy') {
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
        }
        failure {
            echo 'Build Failed!'
        }
        always {
            cleanWs()
        }
    }
}