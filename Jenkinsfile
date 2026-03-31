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
                docker rmi ${IMAGE_NAME} || true
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
        }
        failure {
            echo 'Build Failed!'
        }
        always {
            cleanWs()
        }
    }
}