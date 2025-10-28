def buildAndPushService(String serviceName, String imageName) {
    withCredentials([usernamePassword(credentialsId: 'docker', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
        sh """
            #!/bin/bash
            set -e
            echo "INFO - 도커 로그인 시도..."
            echo "${DOCKER_PASSWORD}" | docker login -u "${DOKCER_USER}" --password-stdin
            echo "INFO - 도커 빌드 시작: ${imageName}:${BUILD_NUMBER}"
            docker build -f ${serviceName}/Dockerfile -t "${imageName}:${BUILD_NUMBER}" .
            echo "INFO - 도커 허브에 도커 이미지 push..."
            docker push "${imageName}:${BUILD_NUMBER}"
            echo "INFO - 도커 push 완료 로그 아웃 합니다."
            docker logout
        """
    }
}

def runGradleTest(String serviceName) {
    sh """
        #!/bin/bash
        set -e
        echo "INFO - [${serviceName}] 테스트 시작..."
        ./gradlew :${serviceName}:clean test
    """
}

pipeline {
    agent any

    environment {
        AUTH_SERVICE_IMAGE_NAME = 'pkun2014/auth_service'
        POLL_SERVICE_IMAGE_NAME = 'pkun2014/poll_service'
        TESTCONTAINERS_HOST_OVERRIDE = 'host.docker.internal'
    }

    tools {
        gradle 'gradle-8'
        dockerTool 'docker'
    }

    stages {
        stage('Git Clone') {
            steps {
                git branch: 'master', credentialsId: 'github', url: 'https://github.com/pkun2/everypoll-msa.git'
            }
        }

        stage('Test authService') {
            when { 
                anyOf {
                    changeset "**/authService/**" 
                    changeset "**/common/**"
                }
            }
            steps {
                configFileProvider([configFile(fileId: 'authService-dotenv', targetLocation: 'authService/.env')]) {
                    script {
                        runGradleTest('authService')
                    }
                }
            }
        }
        stage('Build authService') {
            when { 
                anyOf {
                    changeset "**/authService/**" 
                    changeset "**/common/**"
                }
            }
            steps {
                script {
                    buildAndPushService('authService', env.AUTH_SERVICE_IMAGE_NAME)
                }
            }
        }

        stage('Test pollService') {
            when {
                anyOf {
                    changeset "**/pollService/**"
                    changeset "**/authService/**"
                    changeset "**/common/**"
                }
            }
            steps {
                configFileProvider([
                    configFile(fileId: 'pollService-dotenv', targetLocation: 'pollService/.env') // pollService용 .env 파일이 있다면 추가
                ]) {
                    script {
                        runGradleTest('pollService')
                    }
                }
            }
        }
        stage('Build pollService') {
            when {
                anyOf {
                    changeset "**/pollService/**"
                    changeset "**/authService/**"
                }
            }
            steps {
                script {
                    buildAndPushService('pollService', env.POLL_SERVICE_IMAGE_NAME)
                }
            }
        }
    }
    
    post {
        always {
            echo 'Pipeline finished.'
        }
    }
}
