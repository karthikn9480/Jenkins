pipeline {
    agent any

    environment {
        SSH_KEY_PATH = '/home/karthi/test.pem' // Path to your SSH key
        REMOTE_USER = 'ubuntu'
        REMOTE_HOST = '54.91.215.162' // EC2 instance IP
        LOG_FILE = '/var/log/nginx/error.log' // Nginx error log file path
        TIMESTAMP = sh(script: 'date +%d-%m-%Y_%H-%M-%S', returnStdout: true).trim() // Get current timestamp
        REMOTE_ZIP_PATH = "/tmp/${REMOTE_HOST}-nginx-error-log-${TIMESTAMP}.zip" // Remote path for the zip file with IP address
        LOCAL_DOWNLOAD_PATH = "/home/karthi/${REMOTE_HOST}-nginx-error-log-${TIMESTAMP}.zip" // Local path to download the zip file with IP address
        JENKINS_SSH_DIR = "${WORKSPACE}/.ssh" // Using the Jenkins workspace for SSH config
    }

    stages {
        stage('Zip files on EC2') {
            steps {
                script {
                    // Ensure SSH key exists and has correct permissions
                    sh """
                    if [ ! -f "$SSH_KEY_PATH" ]; then
                        echo "Error: SSH key not found at $SSH_KEY_PATH"
                        exit 1
                    fi
                    
                    chmod 600 $SSH_KEY_PATH

                    # Create the .ssh directory in the Jenkins workspace and add known_hosts
                    mkdir -p $JENKINS_SSH_DIR
                    ssh-keyscan -H $REMOTE_HOST >> $JENKINS_SSH_DIR/known_hosts
                    chmod 644 $JENKINS_SSH_DIR/known_hosts

                    # SSH command to zip the Nginx error log on EC2 with the IP address and timestamped file name
                    echo "Zipping Nginx error log on EC2 instance..."
                    ssh -i $SSH_KEY_PATH -o UserKnownHostsFile=$JENKINS_SSH_DIR/known_hosts $REMOTE_USER@$REMOTE_HOST "zip -r $REMOTE_ZIP_PATH $LOG_FILE"
                    """
                }
            }
        }

        stage('Download zipped file') {
            steps {
                script {
                    echo "Downloading the zipped file from EC2..."

                    // Download the file from EC2 to the local machine using SCP
                    sh """
                    echo "Downloading zipped file from EC2..."
                    scp -i $SSH_KEY_PATH -o UserKnownHostsFile=$JENKINS_SSH_DIR/known_hosts $REMOTE_USER@$REMOTE_HOST:$REMOTE_ZIP_PATH $LOCAL_DOWNLOAD_PATH

                    echo "File downloaded to $LOCAL_DOWNLOAD_PATH"
                    """
                }
            }
        }

        stage('Clean up EC2') {
            steps {
                script {
                    echo "Cleaning up on EC2..."
                    // Delete the zip file from EC2 after downloading
                    sh """
                    ssh -i $SSH_KEY_PATH -o UserKnownHostsFile=$JENKINS_SSH_DIR/known_hosts $REMOTE_USER@$REMOTE_HOST "rm -f $REMOTE_ZIP_PATH"
                    """
                }
            }
        }

        stage('Post Actions') {
            steps {
                echo "Pipeline completed."
            }
        }
    }

    post {
        failure {
            echo 'Pipeline failed'
        }
    }
}
