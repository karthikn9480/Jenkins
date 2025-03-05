pipeline {
    agent any

    environment {
        S3_BUCKET = 'my-app-bucket28'
        LOCAL_NGINX_BACKUP_DIR = '/home/karthi/backup_files/testserver/nginx/'  // Replace with your local Nginx directory
        LOCAL_MYSQL_BACKUP_DIR = '/home/karthi/backup_files/testserver/mysql/'  // Replace with your local MySQL directory
        AWS_REGION = 'us-east-1'  // Replace with your region if needed
        CURRENT_DATE = sh(script: 'date +"%Y-%m-%d"', returnStdout: true).trim()  // Get current date dynamically
    }

    stages {
        stage('Download from S3') {
            steps {
                script {
                    // Create the local directories if they don't exist
                    sh "mkdir -p ${LOCAL_NGINX_BACKUP_DIR} ${LOCAL_MYSQL_BACKUP_DIR}"

                    // Use Jenkins credentials for AWS access
                    withCredentials([string(credentialsId: 'AWS_ACCESS_KEY_ID', variable: 'AWS_ACCESS_KEY_ID'),
                                     string(credentialsId: 'AWS_SECRET_ACCESS_KEY', variable: 'AWS_SECRET_ACCESS_KEY')]) {
                        // Set environment variables for AWS CLI
                        withEnv([
                            "AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}",
                            "AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}",
                            "AWS_DEFAULT_REGION=${AWS_REGION}"
                        ]) {
                            // Run AWS CLI command to download the file from S3
                            sh """
                                aws s3 cp s3://${S3_BUCKET}/nginx-backups/${CURRENT_DATE}/nginx_config_backup_${CURRENT_DATE}.tar.gz ${LOCAL_NGINX_BACKUP_DIR}
                                aws s3 cp s3://${S3_BUCKET}/mysql-backups/${CURRENT_DATE}/mysql_backup_${CURRENT_DATE}.sql ${LOCAL_MYSQL_BACKUP_DIR}
                            """
                        }
                    }
                }
            }
        }
    }
}
