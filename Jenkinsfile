pipeline {
    agent any

    tools {
        jdk 'jdk21'
    }

    environment {
        GRADLE_OPTS = '-Dorg.gradle.daemon=false'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew clean compileJava --no-daemon'
            }
        }
        stage('Test') {
            steps {
                sh './gradlew test --no-daemon'
            }
            post {
                always {
                    junit testResults: 'build/test-results/test/*.xml', allowEmptyResults: true
                }
            }
        }
        stage('Package') {
            steps {
                sh './gradlew bootJar --no-daemon'
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            }
        }
        stage('Report') {
            steps {
                echo 'Build, test, and package stages complete for field-dispatch-ops.'
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
