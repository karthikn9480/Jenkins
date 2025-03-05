pipeline {
    agent any
    
    environment {
        S3_BUCKET = 'my-app-bucket28'
        LOCAL_PATH = '/mnt/c/Users/Lenovo/Downloads/'  // WSL path for D:\Callproof-logs
        AWS_REGION = 'us-east-1'
    }
    
    stages {
        stage('Download from S3') {
            steps {
                script {
                    // Get current date in the format you need (e.g., dd-MM-yyyy)
                    def currentDate = new Date().format('dd-MM-yyyy')
                    def s3FilePath = "ip-10-0-0-251-nginx-error-log-${currentDate}.zip"
                    
                    // Create local directory if it doesn't exist
                    sh "mkdir -p ${LOCAL_PATH}"

                    // Use Jenkins credentials for AWS access
                    withCredentials([string(credentialsId: 'AWS_ACCESS_KEY_ID', variable: 'AWS_ACCESS_KEY_ID'),
                                     string(credentialsId: 'AWS_SECRET_ACCESS_KEY', variable: 'AWS_SECRET_ACCESS_KEY')]) {
                        // Run AWS CLI command to download the file from S3
                        sh """
                            AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} \
                            AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} \
                            AWS_REGION=${AWS_REGION} \
                            aws s3 cp s3://${S3_BUCKET}/${s3FilePath} ${LOCAL_PATH} --region ${AWS_REGION} || true
                        """
                    }
                }
            }
        }
    }
}
